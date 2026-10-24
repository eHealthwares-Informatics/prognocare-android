package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentProvider(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val type: String = "CASH",
    val description: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val typeDisplay: String
        get() = type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

@Serializable
data class CreatePaymentProviderDto(
    val code: String,
    val name: String,
    val type: String,
    val description: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
)

@Serializable
data class UpdatePaymentProviderDto(
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val description: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val isActive: Boolean? = null,
)

enum class PaymentProviderType {
    CASH, HMO, COMPANY, PROGRAM, OTHER,
}