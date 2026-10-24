package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Visit(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val visitType: String = "OUTPATIENT",
    val status: String = "ONGOING",
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val appointmentId: String? = null,
    val startDatetime: String? = null,
    val stopDatetime: String? = null,
    val visitNumber: String? = null,
    val createdById: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val isOngoing: Boolean get() = status == "ONGOING"
    val typeDisplay: String
        get() = visitType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

@Serializable
data class UpdateVisitDto(
    val patientId: String? = null,
    val patientName: String? = null,
    val visitType: String? = null,
    val status: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val stopDatetime: String? = null,
)

@Serializable
data class CreateVisitDto(
    val patientId: String,
    val patientName: String? = null,
    val visitType: String = "OUTPATIENT",
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val appointmentId: String? = null,
    val startDatetime: String? = null,
)

@Serializable
data class EndVisitDto(
    val stopDatetime: String? = null,
)

enum class VisitType {
    OUTPATIENT, INPATIENT, EMERGENCY, HOME_VISIT,
}

enum class VisitStatus {
    ONGOING, COMPLETED, CANCELLED,
}
