package com.ehealthwares.prognocare.feature.dashboard.finance

data class FinanceDashboardState(
    val greeting: String = "",
    val financeName: String = "Adaeze Nwankwo",
    val todayDate: String = "Tuesday, Aug 18",
    val totalRevenue: String = "₦2,450,000",
    val pendingBills: Int = 0,
    val completedPayments: Int = 0,
    val overdueBills: Int = 0,
    val recentBills: List<Bill> = emptyList(),
    val recentPayments: List<Payment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class Bill(
    val id: String,
    val billNumber: String,
    val patientName: String,
    val patientMrn: String,
    val date: String,
    val dueDate: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val status: BillStatus,
    val lineItems: List<BillLineItem> = emptyList(),
    val notes: String? = null,
) {
    val balanceAmount: Double get() = totalAmount - paidAmount
    val balanceDisplay: String get() = "₦%,.2f".format(balanceAmount)
    val totalDisplay: String get() = "₦%,.2f".format(totalAmount)
    val paidDisplay: String get() = "₦%,.2f".format(paidAmount)
    val statusDisplay: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }
    val paymentProgress: Float
        get() = if (totalAmount > 0) (paidAmount / totalAmount).toFloat() else 0f
}

enum class BillStatus {
    DRAFT, PENDING, PARTIAL, PAID, OVERDUE, CANCELLED
}

data class BillLineItem(
    val id: String,
    val description: String,
    val category: ItemCategory,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double,
) {
    val unitPriceDisplay: String get() = "₦%,.2f".format(unitPrice)
    val totalDisplay: String get() = "₦%,.2f".format(total)
}

enum class ItemCategory {
    CONSULTATION, PROCEDURE, LAB_TEST, IMAGING, MEDICATION, ROOM_CHARGE, OTHER
}

data class Payment(
    val id: String,
    val paymentNumber: String,
    val billId: String,
    val billNumber: String,
    val patientName: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val paymentDate: String,
    val reference: String,
    val status: PaymentStatus,
) {
    val amountDisplay: String get() = "₦%,.2f".format(amount)
    val statusDisplay: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class PaymentMethod {
    CASH, CARD, BANK_TRANSFER, MOBILE_PAYMENT, INSURANCE
}

enum class PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}

data class FinancePatient(
    val id: String,
    val name: String,
    val mrn: String,
    val totalBills: Int,
    val totalPaid: Double,
    val totalOutstanding: Double,
    val lastPaymentDate: String?,
) {
    val totalPaidDisplay: String get() = "₦%,.2f".format(totalPaid)
    val outstandingDisplay: String get() = "₦%,.2f".format(totalOutstanding)
    val hasOutstanding: Boolean get() = totalOutstanding > 0
}
