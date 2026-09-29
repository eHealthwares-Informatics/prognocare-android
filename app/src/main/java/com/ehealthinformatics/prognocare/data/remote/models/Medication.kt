package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Medication(
    val id: String = "",
    val medicationNumber: String? = null,
    val requestId: String? = null,
    val patientId: String = "",
    val patientName: String? = null,
    val encounterId: String? = null,
    val visitId: String? = null,
    val name: String = "",
    val dose: String? = null,
    val doseUnit: String? = null,
    val route: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val durationUnit: String? = null,
    val quantity: Int? = null,
    val instructions: String? = null,
    val status: String = "PRESCRIBED",
    val administeredAt: String? = null,
    val administeredById: String? = null,
    val administeredByName: String? = null,
    val administrationNotes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val doseDisplay: String
        get() = listOfNotNull(dose, doseUnit).joinToString(" ").ifBlank { "—" }

    val statusDisplay: String
        get() = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

    val isDue: Boolean
        get() = status == "PRESCRIBED" && administeredAt == null

    val isCancelled: Boolean
        get() = status == "CANCELLED"
}

@Serializable
data class CreateMedicationDto(
    val requestId: String,
    val itemIndex: Int,
    val name: String? = null,
    val dose: String? = null,
    val doseUnit: String? = null,
    val route: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val durationUnit: String? = null,
    val quantity: Int? = null,
    val instructions: String? = null,
)

@Serializable
data class AdministerMedicationDto(
    val administeredAt: String? = null,
    val notes: String? = null,
    val outcome: String? = null,
)
