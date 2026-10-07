package com.ehealthinformatics.prognocare.feature.appointments

import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.config.resolveQueryDateRange
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.data.remote.models.CancelAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CheckInAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Raised for non-2xx API responses with a short, user-presentable message. */
class ApiException(val code: Int, message: String) : Exception(message)

/** List filters mirroring the EMR appointments query params. */
data class AppointmentQuery(
    val page: Int = 1,
    val limit: Int = 50,
    /** Explicit single-day filter; wins over the configured query range. */
    val date: String? = null,
    /**
     * When [date] is null and no query date-range is configured, fall back to
     * today (dashboard default). Set false for open-ended lists.
     */
    val defaultToday: Boolean = false,
    val status: String? = null,
    val providerId: String? = null,
    val patientId: String? = null,
    val search: String? = null,
    val sortBy: String = "date",
    val sortOrder: String = "desc",
)

/**
 * Data access for appointments (and the patient/provider lookups needed to
 * schedule one). Reads the current [RetrofitClient.ApiBundle] on every call so
 * runtime base-URL changes are picked up immediately.
 *
 * Date resolution order for list queries:
 * 1. Explicit [AppointmentQuery.date]
 * 2. Settings query range period (today / last day / week / month — resolved
 *    dynamically at query time)
 * 3. Today, when [AppointmentQuery.defaultToday] is true
 */
@Singleton
class AppointmentsRepository @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val configStore: AppConfigStore,
) {

    /** Today's date as `yyyy-MM-dd` in the device timezone. */
    fun today(): String =
        java.time.LocalDate.now().toString()

    /**
     * Resolves the EMR `date` query param from explicit filter → configured
     * query range period → optional today fallback.
     */
    fun resolveDateParam(explicit: String?, defaultToday: Boolean = false): String? {
        if (explicit != null) return explicit
        val fromRange = configStore.config.value
            .resolveQueryDateRange()
            ?.toAppointmentDateParam()
        if (fromRange != null) return fromRange
        return if (defaultToday) today() else null
    }

    suspend fun list(query: AppointmentQuery = AppointmentQuery()): List<Appointment> {
        val dateParam = resolveDateParam(query.date, query.defaultToday)
        val response = retrofitClient.apis.first().appointmentApi.list(
            page = query.page,
            limit = query.limit,
            date = dateParam,
            status = query.status,
            providerId = query.providerId,
            patientId = query.patientId,
            search = query.search,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    suspend fun create(dto: CreateAppointmentDto): Appointment {
        val response = retrofitClient.apis.first().appointmentApi.create(dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun checkIn(id: String, dto: CheckInAppointmentDto = CheckInAppointmentDto()): Appointment {
        val response = retrofitClient.apis.first().appointmentApi.checkIn(id, dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.appointment ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun cancel(id: String, reason: String = ""): Appointment {
        val response = retrofitClient.apis.first().appointmentApi.cancel(id, CancelAppointmentDto(reason))
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun noShow(id: String): Appointment {
        val response = retrofitClient.apis.first().appointmentApi.noShow(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun complete(id: String): Appointment {
        val response = retrofitClient.apis.first().appointmentApi.complete(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    /** Search patient records by name or MRN (used to schedule/self-identify). */
    suspend fun searchPatients(search: String, limit: Int = 20): List<Patient> {
        val response = retrofitClient.apis.first().patientApi.list(
            page = 1,
            limit = limit,
            search = search.takeIf { it.isNotBlank() },
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    /** Providers available for scheduling (Doctor role type). */
    suspend fun searchProviders(search: String = "", limit: Int = 50): List<Staff> {
        val response = retrofitClient.apis.first().staffApi.list(
            search = search.takeIf { it.isNotBlank() },
            roleType = "Doctor",
            isActive = true,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty().take(limit)
    }

    private fun failure(response: retrofit2.Response<*>): ApiException {
        val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.lineSequence()?.firstOrNull()?.take(180)
        return ApiException(
            response.code(),
            detail ?: "${response.code()} ${response.message()}",
        )
    }
}
