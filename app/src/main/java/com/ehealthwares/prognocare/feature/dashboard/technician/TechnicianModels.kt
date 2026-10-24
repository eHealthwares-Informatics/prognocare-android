package com.ehealthwares.prognocare.feature.dashboard.technician

/**
 * Technician dashboard state
 */
data class TechnicianDashboardState(
    val isLoading: Boolean = true,
    val technicianName: String = "Kemi Adeyemi",
    val greeting: String = "Good morning",
    val todayDate: String = "Tuesday, Aug 19",
    val department: String = "Laboratory",
    // KPIs
    val pendingOrders: Int = 0,
    val inProgress: Int = 0,
    val completedToday: Int = 0,
    val urgentOrders: Int = 0,
    // Data
    val pendingOrdersList: List<TechnicianOrder> = emptyList(),
    val recentResults: List<TechnicianResult> = emptyList(),
)

/**
 * Lab/diagnostic order from a doctor
 */
data class TechnicianOrder(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val orderType: OrderType,
    val testName: String,
    val orderedBy: String,
    val priority: OrderPriority,
    val status: OrderStatus,
    val orderedAt: String,
    val dueTime: String? = null,
    val notes: String? = null,
)

/**
 * Types of diagnostic orders
 */
enum class OrderType(val displayName: String) {
    LAB_BLOOD("Lab - Blood"),
    LAB_URINE("Lab - Urine"),
    LAB_STOOL("Lab - Stool"),
    IMAGING_XRAY("Imaging - X-Ray"),
    IMAGING_ULTRASOUND("Imaging - Ultrasound"),
    IMAGING_MRI("Imaging - MRI"),
    IMAGING_CT("Imaging - CT Scan"),
    ECG("ECG"),
    PFT("Pulmonary Function Test"),
    OTHER("Other")
}

/**
 * Order priority
 */
enum class OrderPriority(val displayName: String) {
    STAT("STAT"),
    URGENT("Urgent"),
    ROUTINE("Routine")
}

/**
 * Order status in the lab workflow
 */
enum class OrderStatus(val displayName: String) {
    RECEIVED("Received"),
    IN_PROGRESS("In Progress"),
    SAMPLE_COLLECTED("Sample Collected"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

/**
 * Completed result uploaded by technician
 */
data class TechnicianResult(
    val id: String,
    val orderId: String,
    val patientName: String,
    val testName: String,
    val resultSummary: String,
    val isAbnormal: Boolean,
    val uploadedAt: String,
    val reviewedBy: String? = null,
)

/**
 * Mock data for Technician dashboard
 */
fun getMockPendingOrders(): List<TechnicianOrder> = listOf(
    TechnicianOrder(
        id = "ORD-001",
        patientName = "Adaeze Nwankwo",
        patientMrn = "MRN-2024-1001",
        orderType = OrderType.LAB_BLOOD,
        testName = "Complete Blood Count (CBC)",
        orderedBy = "Dr. Chidi Okonkwo",
        priority = OrderPriority.URGENT,
        status = OrderStatus.RECEIVED,
        orderedAt = "30 min ago",
        dueTime = "10:00 AM",
        notes = "Fasting required",
    ),
    TechnicianOrder(
        id = "ORD-002",
        patientName = "Blessing Okafor",
        patientMrn = "MRN-2024-1002",
        orderType = OrderType.IMAGING_XRAY,
        testName = "Chest X-Ray (PA View)",
        orderedBy = "Dr. Fatima Bello",
        priority = OrderPriority.ROUTINE,
        status = OrderStatus.RECEIVED,
        orderedAt = "1h ago",
        dueTime = "11:00 AM",
    ),
    TechnicianOrder(
        id = "ORD-003",
        patientName = "Chukwuemeka Obi",
        patientMrn = "MRN-2024-1003",
        orderType = OrderType.LAB_BLOOD,
        testName = "Lipid Panel",
        orderedBy = "Dr. Chidi Okonkwo",
        priority = OrderPriority.ROUTINE,
        status = OrderStatus.SAMPLE_COLLECTED,
        orderedAt = "2h ago",
        dueTime = "12:00 PM",
    ),
    TechnicianOrder(
        id = "ORD-004",
        patientName = "Doris Abebe",
        patientMrn = "MRN-2024-1004",
        orderType = OrderType.ECG,
        testName = "12-Lead ECG",
        orderedBy = "Dr. Fatima Bello",
        priority = OrderPriority.STAT,
        status = OrderStatus.RECEIVED,
        orderedAt = "15 min ago",
        dueTime = "ASAP",
        notes = "Chest pain - STAT",
    ),
    TechnicianOrder(
        id = "ORD-005",
        patientName = "Emeka Ugwu",
        patientMrn = "MRN-2024-1005",
        orderType = OrderType.LAB_URINE,
        testName = "Urinalysis + Culture",
        orderedBy = "Nurse Amara Eze",
        priority = OrderPriority.ROUTINE,
        status = OrderStatus.IN_PROGRESS,
        orderedAt = "3h ago",
        dueTime = "02:00 PM",
    ),
    TechnicianOrder(
        id = "ORD-006",
        patientName = "Fatima Yusuf",
        patientMrn = "MRN-2024-1006",
        orderType = OrderType.IMAGING_ULTRASOUND,
        testName = "Abdominal Ultrasound",
        orderedBy = "Dr. Chidi Okonkwo",
        priority = OrderPriority.URGENT,
        status = OrderStatus.RECEIVED,
        orderedAt = "45 min ago",
        dueTime = "10:30 AM",
        notes = "Rule out appendicitis",
    ),
    TechnicianOrder(
        id = "ORD-007",
        patientName = "Grace Okoro",
        patientMrn = "MRN-2024-1007",
        orderType = OrderType.LAB_BLOOD,
        testName = "HbA1c (Glycated Hemoglobin)",
        orderedBy = "Dr. Chidi Okonkwo",
        priority = OrderPriority.ROUTINE,
        status = OrderStatus.PROCESSING,
        orderedAt = "4h ago",
        dueTime = "03:00 PM",
    ),
)

fun getMockRecentResults(): List<TechnicianResult> = listOf(
    TechnicianResult(
        id = "RES-001",
        orderId = "ORD-010",
        patientName = "Henry Adekunle",
        testName = "Complete Blood Count (CBC)",
        resultSummary = "WBC 7.2 (H: 4.5-11.0), RBC 4.8, Hgb 13.2, Plt 245",
        isAbnormal = false,
        uploadedAt = "1h ago",
        reviewedBy = "Dr. Chidi Okonkwo",
    ),
    TechnicianResult(
        id = "RES-002",
        orderId = "ORD-011",
        patientName = "Ifeoma Chukwu",
        testName = "Fasting Blood Glucose",
        resultSummary = "GLU 186 mg/dL (H: 70-100) ** HIGH **",
        isAbnormal = true,
        uploadedAt = "2h ago",
        reviewedBy = null,
    ),
    TechnicianResult(
        id = "RES-003",
        orderId = "ORD-012",
        patientName = "James Okonjo",
        testName = "Chest X-Ray (PA View)",
        resultSummary = "No acute cardiopulmonary abnormality",
        isAbnormal = false,
        uploadedAt = "3h ago",
        reviewedBy = "Dr. Fatima Bello",
    ),
    TechnicianResult(
        id = "RES-004",
        orderId = "ORD-013",
        patientName = "Kemi Adeleye",
        testName = "Lipid Panel",
        resultSummary = "Total Chol 268 (H), LDL 178 (H), HDL 38 (L) ** ABNORMAL **",
        isAbnormal = true,
        uploadedAt = "4h ago",
        reviewedBy = null,
    ),
)
