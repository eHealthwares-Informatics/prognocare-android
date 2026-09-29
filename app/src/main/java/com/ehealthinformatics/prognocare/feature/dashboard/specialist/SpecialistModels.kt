package com.ehealthinformatics.prognocare.feature.dashboard.specialist

import com.ehealthinformatics.prognocare.data.remote.models.Referral

data class SpecialistDashboardState(
    val greeting: String = "",
    val specialistName: String = "Dr. Fatima Bello",
    val specialty: String = "Specialist Clinic",
    val todayDate: String = "Tuesday, Aug 18",
    val pendingReferrals: Int = 0,
    val activePatients: Int = 0,
    val completedReviews: Int = 0,
    val urgentCases: Int = 0,
    val recentReferrals: List<Referral> = emptyList(),
    val upcomingConsultations: List<SpecialistConsultation> = emptyList(),
    val specialtyStats: SpecialtyStats = SpecialtyStats(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

enum class ReferralPriority {
    ROUTINE, URGENT;
}

enum class ReferralStatus {
    PENDING, ACCEPTED, DECLINED, COMPLETED;
}

data class SpecialistConsultation(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val type: String,
    val date: String,
    val time: String,
    val location: String,
    val status: String,
    val reason: String? = null,
    val isUrgent: Boolean = false,
) {
    val statusDisplay: String
        get() = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

data class SpecialtyStats(
    val totalReferralsThisMonth: Int = 0,
    val avgResponseTime: String = "—",
    val acceptanceRate: String = "—",
    val patientSatisfaction: String = "—",
    val commonConditions: List<String> = emptyList(),
)

data class SpecialistPatient(
    val id: String,
    val name: String,
    val age: Int,
    val mrn: String,
    val condition: String,
    val lastVisit: String,
    val nextAppointment: String? = null,
    val isOngoing: Boolean = true,
)
