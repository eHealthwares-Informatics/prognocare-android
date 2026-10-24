package com.ehealthwares.prognocare.feature.dashboard.patient

data class PatientDashboardState(
    val greeting: String = "",
    val patientName: String = "Chidi Okonkwo",
    val mrn: String = "MRN-00142",
    val todayDate: String = "Tuesday, Aug 18",
    val tagline: String = "Take charge of your health",
    val upcomingAppointments: Int = 0,
    val activeMedications: Int = 0,
    val labResults: Int = 0,
    val healthScore: Int = 0,
    val healthScoreLabel: String = "Good",
    val nextAppointment: PatientAppointment? = null,
    val recentAppointments: List<PatientAppointment> = emptyList(),
    val currentMedications: List<PatientMedication> = emptyList(),
    val recentRecords: List<PatientRecord> = emptyList(),
    val healthAlerts: List<HealthAlert> = emptyList(),
    val shouldShowProfilePrompt: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class PatientAppointment(
    val id: String,
    val providerName: String,
    val providerSpecialty: String,
    val type: String,
    val date: String,
    val time: String,
    val location: String,
    val status: String,
    val reason: String? = null,
) {
    val isUpcoming: Boolean get() = status in listOf("SCHEDULED", "CHECKED_IN")
    val statusDisplay: String
        get() = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

data class PatientMedication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val route: String,
    val prescriber: String,
    val startDate: String,
    val endDate: String? = null,
    val refillDate: String? = null,
    val isActive: Boolean = true,
    val instructions: String = "",
    val nextDose: String = "",
) {
    val needsRefill: Boolean
        get() = refillDate != null && isActive
}

data class PatientRecord(
    val id: String,
    val title: String,
    val type: String,
    val date: String,
    val providerName: String,
    val summary: String,
    val hasAttachment: Boolean = false,
    val status: RecordStatus = RecordStatus.NORMAL,
) {
    val typeDisplay: String
        get() = type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

enum class RecordStatus {
    NORMAL, ABNORMAL, PENDING, VIEW_REPORT
}

data class HealthAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val date: String,
)

enum class AlertSeverity {
    INFO, WARNING, URGENT,
}
