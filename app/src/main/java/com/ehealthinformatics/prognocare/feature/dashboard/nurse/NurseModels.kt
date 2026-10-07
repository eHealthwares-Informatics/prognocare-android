package com.ehealthinformatics.prognocare.feature.dashboard.nurse

data class NurseDashboardState(
    val greeting: String = "",
    val nurseName: String = "Nurse Amina",
    val todayDate: String = "Tuesday, Aug 18",
    val patientsCheckedIn: Int = 0,
    val vitalsToRecord: Int = 0,
    /** Today's visits (from VisitApi). */
    val visitsCount: Int = 0,
    /** Active admissions (status=ADMITTED, from AdmissionApi). */
    val admissionsCount: Int = 0,
    val pendingTasks: Int = 0,
    val completedToday: Int = 0,
    val urgentTasks: Int = 0,
    val taskQueue: List<NurseTask> = emptyList(),
    val upcomingCheckIns: List<NurseCheckIn> = emptyList(),
    val recentVitals: List<VitalsRecord> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

data class NurseTask(
    val id: String,
    val patientName: String,
    val patientId: String,
    val taskType: NurseTaskType,
    val description: String,
    val priority: TaskPriority,
    val scheduledTime: String,
    val status: TaskStatus,
    val encounterId: String? = null,
    val visitId: String? = null,
)

enum class NurseTaskType {
    VITALS, MEDICATION, CHECK_IN, CHECK_OUT, ASSESSMENT, DOCUMENTATION, SPECIMEN,
}

enum class TaskPriority {
    LOW, NORMAL, HIGH, URGENT,
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, SKIPPED,
}

data class NurseCheckIn(
    val id: String,
    val patientName: String,
    val patientId: String,
    val appointmentTime: String,
    val appointmentType: String,
    val providerName: String,
    val isCheckedIn: Boolean = false,
    val vitalsComplete: Boolean = false,
)

data class VitalsRecord(
    val id: String,
    val patientName: String,
    val recordedAt: String,
    val temperature: String? = null,
    val bloodPressureSystolic: String? = null,
    val bloodPressureDiastolic: String? = null,
    val heartRate: String? = null,
    val respiratoryRate: String? = null,
    val oxygenSaturation: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val recordedBy: String = "",
)

data class MedicationAdministration(
    val id: String,
    val patientName: String,
    val medicationName: String,
    val dosage: String,
    val route: String,
    val scheduledTime: String,
    val status: MedAdminStatus,
    val notes: String = "",
)

enum class MedAdminStatus {
    SCHEDULED, ADMINISTERED, SKIPPED, REFUSED,
}
