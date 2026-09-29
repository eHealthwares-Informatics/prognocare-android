package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummary(
    val date: String = "",
    val metrics: DashboardMetrics = DashboardMetrics(),
    val appointments: List<Appointment> = emptyList(),
    val providerLoad: List<ProviderLoad> = emptyList(),
    val upcoming: List<Appointment> = emptyList(),
) {
    val upcomingAppointments: List<Appointment> get() = upcoming
}

@Serializable
data class DashboardMetrics(
    val totalAppointments: Int = 0,
    val scheduled: Int = 0,
    val checkedIn: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val noShow: Int = 0,
    val providersOnDuty: Int = 0,
    val averageWaitMinutes: Int = 0,
    val totalPatients: Int = 0,
    val activeVisits: Int = 0,
    val pendingRequests: Int = 0,
)

/** Response of GET /api/dashboard/attended-patients. */
@Serializable
data class AttendedPatientsResponse(
    val count: Int = 0,
)

@Serializable
data class ProviderLoad(
    val providerId: String = "",
    val providerName: String = "",
    val patientCount: Int = 0,
    val activeCount: Int = 0,
)