package com.ehealthwares.prognocare.feature.dashboard.support

import com.ehealthwares.prognocare.navigation.UserRole

/**
 * Support role dashboard state
 */
data class SupportDashboardState(
    val isLoading: Boolean = true,
    val supportName: String = "Emeka Nwosu",
    val greeting: String = "Good morning",
    val todayDate: String = "Tuesday, Aug 19",
    // KPIs
    val patientsWaiting: Int = 0,
    val checkedInToday: Int = 0,
    val activeRequests: Int = 0,
    val completedToday: Int = 0,
    // Data
    val checkInQueue: List<SupportCheckIn> = emptyList(),
    val recentRequests: List<SupportRequest> = emptyList(),
)

/**
 * Patient check-in/check-out record
 */
data class SupportCheckIn(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val appointmentTime: String,
    val appointmentType: String,
    val doctorName: String,
    val status: CheckInStatus,
    val waitTime: String? = null,
    val phone: String? = null,
)

/**
 * Check-in status
 */
enum class CheckInStatus(val displayName: String) {
    WAITING("Waiting"),
    CHECKED_IN("Checked In"),
    IN_SESSION("In Session"),
    CHECKED_OUT("Checked Out"),
    NO_SHOW("No Show")
}

/**
 * Support request / ticket
 */
data class SupportRequest(
    val id: String,
    val title: String,
    val description: String,
    val category: RequestCategory,
    val priority: RequestPriority,
    val status: RequestStatus,
    val patientName: String? = null,
    val createdAt: String,
    val assignedTo: String? = null,
)

/**
 * Request categories
 */
enum class RequestCategory(val displayName: String) {
    PREAUTHORIZATION("Preauthorization"),
    INSURANCE("Insurance"),
    RECORDS("Medical Records"),
    APPOINTMENT("Appointment"),
    BILLING("Billing"),
    COMPLAINT("Complaint"),
    OTHER("Other")
}

/**
 * Request priority levels
 */
enum class RequestPriority(val displayName: String) {
    URGENT("Urgent"),
    HIGH("High"),
    NORMAL("Normal"),
    LOW("Low")
}

/**
 * Request status
 */
enum class RequestStatus(val displayName: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    ESCALATED("Escalated"),
    CLOSED("Closed")
}

/**
 * Mock data for Support dashboard
 */
fun getMockCheckInQueue(): List<SupportCheckIn> = listOf(
    SupportCheckIn(
        id = "CI-001",
        patientName = "Adaeze Nwankwo",
        patientMrn = "MRN-2024-1001",
        appointmentTime = "09:00 AM",
        appointmentType = "General Consultation",
        doctorName = "Dr. Chidi Okonkwo",
        status = CheckInStatus.WAITING,
        waitTime = "15 min",
        phone = "+234 801 234 5678",
    ),
    SupportCheckIn(
        id = "CI-002",
        patientName = "Blessing Okafor",
        patientMrn = "MRN-2024-1002",
        appointmentTime = "09:30 AM",
        appointmentType = "Follow-up",
        doctorName = "Dr. Fatima Bello",
        status = CheckInStatus.WAITING,
        waitTime = "8 min",
        phone = "+234 802 345 6789",
    ),
    SupportCheckIn(
        id = "CI-003",
        patientName = "Chukwuemeka Obi",
        patientMrn = "MRN-2024-1003",
        appointmentTime = "10:00 AM",
        appointmentType = "Lab Results Review",
        doctorName = "Dr. Chidi Okonkwo",
        status = CheckInStatus.CHECKED_IN,
        waitTime = null,
        phone = "+234 803 456 7890",
    ),
    SupportCheckIn(
        id = "CI-004",
        patientName = "Doris Abebe",
        patientMrn = "MRN-2024-1004",
        appointmentTime = "10:30 AM",
        appointmentType = "Specialist Referral",
        doctorName = "Dr. Fatima Bello",
        status = CheckInStatus.IN_SESSION,
        waitTime = null,
        phone = "+234 804 567 8901",
    ),
    SupportCheckIn(
        id = "CI-005",
        patientName = "Emeka Ugwu",
        patientMrn = "MRN-2024-1005",
        appointmentTime = "11:00 AM",
        appointmentType = "Vaccination",
        doctorName = "Nurse Amara Eze",
        status = CheckInStatus.WAITING,
        waitTime = "3 min",
        phone = "+234 805 678 9012",
    ),
    SupportCheckIn(
        id = "CI-006",
        patientName = "Fatima Yusuf",
        patientMrn = "MRN-2024-1006",
        appointmentTime = "08:30 AM",
        appointmentType = "Blood Pressure Check",
        doctorName = "Nurse Amara Eze",
        status = CheckInStatus.CHECKED_OUT,
        waitTime = null,
        phone = "+234 806 789 0123",
    ),
)

fun getMockRequests(): List<SupportRequest> = listOf(
    SupportRequest(
        id = "REQ-001",
        title = "Preauthorization for MRI Scan",
        description = "Patient needs preauthorization for brain MRI scan scheduled for Aug 22",
        category = RequestCategory.PREAUTHORIZATION,
        priority = RequestPriority.URGENT,
        status = RequestStatus.OPEN,
        patientName = "Adaeze Nwankwo",
        createdAt = "2h ago",
        assignedTo = null,
    ),
    SupportRequest(
        id = "REQ-002",
        title = "Insurance Verification",
        description = "Verify NHIS coverage for new patient Chukwuemeka Obi",
        category = RequestCategory.INSURANCE,
        priority = RequestPriority.HIGH,
        status = RequestStatus.IN_PROGRESS,
        patientName = "Chukwuemeka Obi",
        createdAt = "3h ago",
        assignedTo = "Aisha Abdullahi",
    ),
    SupportRequest(
        id = "REQ-003",
        title = "Medical Records Transfer",
        description = "Transfer patient records from Lagos University Teaching Hospital",
        category = RequestCategory.RECORDS,
        priority = RequestPriority.NORMAL,
        status = RequestStatus.OPEN,
        patientName = "Doris Abebe",
        createdAt = "5h ago",
        assignedTo = null,
    ),
    SupportRequest(
        id = "REQ-004",
        title = "Reschedule Appointment",
        description = "Patient wants to reschedule cardiology appointment from Aug 20 to Aug 25",
        category = RequestCategory.APPOINTMENT,
        priority = RequestPriority.NORMAL,
        status = RequestStatus.RESOLVED,
        patientName = "Emeka Ugwu",
        createdAt = "1d ago",
        assignedTo = "Emeka Nwosu",
    ),
    SupportRequest(
        id = "REQ-005",
        title = "Billing Dispute",
        description = "Patient disputing charge for consultation on Aug 10",
        category = RequestCategory.BILLING,
        priority = RequestPriority.HIGH,
        status = RequestStatus.ESCALATED,
        patientName = "Fatima Yusuf",
        createdAt = "1d ago",
        assignedTo = "Aisha Abdullahi",
    ),
)
