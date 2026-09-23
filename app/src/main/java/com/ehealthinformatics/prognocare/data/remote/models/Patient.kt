package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Identifier(
    val type: String = "",
    val value: String = "",
)

@Serializable
data class Patient(
    val id: String = "",
    val patientId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val otherNames: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val nextOfKinName: String? = null,
    val nextOfKinPhone: String? = null,
    val nextOfKinRelationship: String? = null,
    val identifiers: List<Identifier> = emptyList(),
    val maritalStatus: String? = null,
    val occupation: String? = null,
    val bloodGroup: String? = null,
    val genotype: String? = null,
    val paymentProviderIds: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val fullName: String
        get() = listOfNotNull(firstName, otherNames, lastName).joinToString(" ")

    val displayName: String
        get() = "$firstName $lastName"

    val initials: String
        get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()

    /** Age in years derived from [dateOfBirth]; 0 when unknown. */
    val ageYears: Int
        get() = dateOfBirth?.take(10)?.let { dob ->
            runCatching {
                java.time.LocalDate.parse(dob).until(java.time.LocalDate.now()).years
            }.getOrNull()
        } ?: 0
}

@Serializable
data class CreatePatientDto(
    val patientId: String? = null,
    val firstName: String,
    val lastName: String,
    val otherNames: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val nextOfKinName: String? = null,
    val nextOfKinPhone: String? = null,
    val nextOfKinRelationship: String? = null,
    val identifiers: List<Identifier>? = null,
    val maritalStatus: String? = null,
    val occupation: String? = null,
    val bloodGroup: String? = null,
    val genotype: String? = null,
    val paymentProviderIds: List<String>? = null,
)

@Serializable
data class UpdatePatientDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val otherNames: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val nextOfKinName: String? = null,
    val nextOfKinPhone: String? = null,
    val nextOfKinRelationship: String? = null,
    val identifiers: List<Identifier>? = null,
    val maritalStatus: String? = null,
    val occupation: String? = null,
    val bloodGroup: String? = null,
    val genotype: String? = null,
    val isActive: Boolean? = null,
)
