package com.ehealthwares.prognocare.feature.dashboard.therapist

data class TherapistDashboardState(
    val greeting: String = "",
    val therapistName: String = "Dr. Grace Obi",
    val specialty: String = "Physical Therapy",
    val todayDate: String = "Tuesday, Aug 18",
    val todaySessions: Int = 0,
    val activePlans: Int = 0,
    val pendingAssessments: Int = 0,
    val completedToday: Int = 0,
    val upcomingSessions: List<TherapySession> = emptyList(),
    val activePatients: List<TherapyPatient> = emptyList(),
    val recentAssessments: List<ProgressAssessment> = emptyList(),
    val therapyPlans: List<TherapyPlan> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class TherapySession(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val sessionType: SessionType,
    val scheduledTime: String,
    val duration: String,
    val location: String,
    val status: SessionStatus,
    val notes: String? = null,
    val isUrgent: Boolean = false,
) {
    val statusDisplay: String
        get() = status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

enum class SessionType {
    INITIAL_ASSESSMENT, FOLLOW_UP, TREATMENT, REHABILITATION, CONSULTATION
}

enum class SessionStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
}

data class TherapyPatient(
    val id: String,
    val name: String,
    val age: Int,
    val mrn: String,
    val condition: String,
    val therapyType: String,
    val sessionsCompleted: Int,
    val totalSessions: Int,
    val nextSession: String? = null,
    val progressPercent: Int = 0,
) {
    val isOngoing: Boolean get() = sessionsCompleted < totalSessions
}

data class TherapyPlan(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val planName: String,
    val diagnosis: String,
    val startDate: String,
    val endDate: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val status: PlanStatus,
    val goals: List<String> = emptyList(),
) {
    val progressPercent: Int
        get() = if (totalSessions > 0) (completedSessions * 100) / totalSessions else 0
    val statusDisplay: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class PlanStatus {
    ACTIVE, COMPLETED, PAUSED, CANCELLED
}

data class ProgressAssessment(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val assessmentType: String,
    val date: String,
    val score: Int,
    val maxScore: Int,
    val notes: String,
    val trend: AssessmentTrend,
) {
    val scoreDisplay: String get() = "$score/$maxScore"
    val trendDisplay: String
        get() = when (trend) {
            AssessmentTrend.IMPROVING -> "Improving"
            AssessmentTrend.STABLE -> "Stable"
            AssessmentTrend.DECLINING -> "Declining"
        }
}

enum class AssessmentTrend {
    IMPROVING, STABLE, DECLINING
}

data class TherapyNotification(
    val id: String,
    val title: String,
    val message: String,
    val patientName: String,
    val type: NotificationType,
    val timestamp: String,
    val isRead: Boolean = false,
)

enum class NotificationType {
    SESSION_REMINDER, PLAN_UPDATE, ASSESSMENT_DUE, PATIENT_MESSAGE
}
