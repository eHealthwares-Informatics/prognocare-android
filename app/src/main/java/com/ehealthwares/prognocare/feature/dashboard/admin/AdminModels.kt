package com.ehealthwares.prognocare.feature.dashboard.admin

data class AdminDashboardState(
    val greeting: String = "",
    val adminName: String = "Ibrahim Mohammed",
    val todayDate: String = "Tuesday, Aug 18",
    val totalPatients: Int = 0,
    val checkedInToday: Int = 0,
    val waitingForCheckIn: Int = 0,
    val totalStaff: Int = 0,
    val activeVisits: Int = 0,
    val searchResults: List<AdminPatient> = emptyList(),
    val checkInQueue: List<AdminCheckIn> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class AdminPatient(
    val id: String,
    val name: String,
    val age: Int,
    val mrn: String,
    val gender: String,
    val phone: String,
    val email: String,
    val lastVisit: String?,
    val status: PatientStatus,
    val hasActiveVisit: Boolean = false,
) {
    val statusDisplay: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class PatientStatus {
    ACTIVE, INACTIVE, NEW, BLOCKED
}

data class AdminCheckIn(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val patientAge: Int,
    val appointmentTime: String,
    val appointmentType: String,
    val providerName: String,
    val department: String,
    val status: CheckInStatus,
    val checkedInAt: String? = null,
    val checkedOutAt: String? = null,
) {
    val statusDisplay: String
        get() = status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

enum class CheckInStatus {
    WAITING, CHECKED_IN, IN_VISIT, CHECKED_OUT, NO_SHOW, CANCELLED
}

data class AdminVisit(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val visitType: String,
    val providerName: String,
    val department: String,
    val startTime: String,
    val endTime: String?,
    val status: String,
)

data class FacilityStats(
    val totalDoctors: Int = 12,
    val totalNurses: Int = 24,
    val totalOtherStaff: Int = 18,
    val occupancyRate: String = "78%",
    val avgWaitTime: String = "18 min",
    val patientSatisfaction: String = "4.6/5",
)
