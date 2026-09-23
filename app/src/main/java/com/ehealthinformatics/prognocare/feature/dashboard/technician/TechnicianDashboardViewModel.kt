package com.ehealthinformatics.prognocare.feature.dashboard.technician

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.feature.records.EmrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Technician orders are backed by LAB clinical requests. Status mapping:
 * REQUESTED → RECEIVED, IN_PROGRESS → IN_PROGRESS/PROCESSING,
 * COMPLETED → COMPLETED, CANCELLED/REJECTED → CANCELLED.
 */
@HiltViewModel
class TechnicianDashboardViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TechnicianDashboardState())
    val state: StateFlow<TechnicianDashboardState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                _error.value = null
                val labRequests = emrRepository.requests(requestType = "LAB", limit = 100)

                val orders = labRequests
                    .filter { it.status !in listOf("COMPLETED", "CANCELLED", "REJECTED") }
                    .map { request ->
                        val first = request.items.firstOrNull()
                        TechnicianOrder(
                            id = request.id,
                            patientName = request.patientName,
                            patientMrn = request.patientId,
                            orderType = OrderType.LAB_BLOOD,
                            testName = first?.name ?: "Lab order",
                            orderedBy = request.orderingProviderName ?: "—",
                            priority = when (request.priority) {
                                "EMERGENCY" -> OrderPriority.STAT
                                "URGENT" -> OrderPriority.URGENT
                                else -> OrderPriority.ROUTINE
                            },
                            status = when (request.status) {
                                "IN_PROGRESS" -> OrderStatus.IN_PROGRESS
                                else -> OrderStatus.RECEIVED
                            },
                            orderedAt = request.requestedAt?.take(10) ?: "—",
                            dueTime = null,
                            notes = request.clinicalNotes,
                        )
                    }

                val completed = labRequests.filter { it.status == "COMPLETED" }
                val results = completed.map { request ->
                    val first = request.items.firstOrNull()
                    TechnicianResult(
                        id = "${request.id}-result",
                        orderId = request.id,
                        patientName = request.patientName,
                        testName = first?.name ?: "Lab order",
                        resultSummary = request.clinicalNotes ?: "Result recorded via LIS sync",
                        isAbnormal = false,
                        uploadedAt = request.completedAt?.take(10) ?: request.updatedAt?.take(10) ?: "—",
                        reviewedBy = request.orderingProviderName,
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        greeting = getGreeting(),
                        todayDate = getTodayDate(),
                        pendingOrders = orders.count { o ->
                            o.status in listOf(OrderStatus.RECEIVED, OrderStatus.SAMPLE_COLLECTED)
                        },
                        inProgress = orders.count { o ->
                            o.status in listOf(OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING)
                        },
                        completedToday = results.size,
                        urgentOrders = orders.count { o ->
                            o.priority == OrderPriority.STAT || o.priority == OrderPriority.URGENT
                        },
                        pendingOrdersList = orders,
                        recentResults = results,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _error.value = e.message ?: "Failed to load lab orders"
            }
        }
    }

    /** Persist a status change on the underlying LAB request. */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _state.update { current ->
            val updatedOrders = current.pendingOrdersList.map { order ->
                if (order.id == orderId) order.copy(status = newStatus) else order
            }
            current.copy(
                pendingOrdersList = updatedOrders,
                pendingOrders = updatedOrders.count { o ->
                    o.status in listOf(OrderStatus.RECEIVED, OrderStatus.SAMPLE_COLLECTED)
                },
                inProgress = updatedOrders.count { o ->
                    o.status in listOf(OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING)
                },
            )
        }
        val requestStatus = when (newStatus) {
            OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING, OrderStatus.SAMPLE_COLLECTED -> "IN_PROGRESS"
            OrderStatus.COMPLETED -> "COMPLETED"
            OrderStatus.CANCELLED -> "CANCELLED"
            else -> return
        }
        viewModelScope.launch {
            try {
                emrRepository.transitionRequest(
                    orderId,
                    com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto(
                        status = requestStatus,
                        reason = "Updated by technician (mobile)",
                    ),
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not update order status"
            }
        }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }
}
