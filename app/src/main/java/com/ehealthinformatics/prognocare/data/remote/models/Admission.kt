package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Admission(
    val id: String = "",
    val admissionNumber: String? = null,
    val patientId: String = "",
    val patientName: String = "",
    val wardId: String? = null,
    val bedId: String? = null,
    val admissionDatetime: String? = null,
    val admissionType: String = "ELECTIVE",
    val diagnosis: String? = null,
    val referringProviderId: String? = null,
    val referringProviderName: String? = null,
    val status: String = "ADMITTED",
    val dischargeDatetime: String? = null,
    val dischargeType: String? = null,
    val dischargeSummary: String? = null,
    val notes: String? = null,
    val visitId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val typeDisplay: String
        get() = admissionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

    val statusDisplay: String
        get() = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

    val isActive: Boolean
        get() = status == "ADMITTED"

    val isToday: Boolean
        get() = admissionDatetime?.take(10) == java.time.LocalDate.now().toString()
}
