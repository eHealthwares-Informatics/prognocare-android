package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Referral(
    val id: String = "",
    val referralNumber: String? = null,
    val patientId: String = "",
    val patientName: String? = null,
    val encounterId: String = "",
    val visitId: String? = null,
    val referringProviderId: String? = null,
    val referringProviderName: String? = null,
    val specialistProviderId: String? = null,
    val specialistProviderName: String? = null,
    val specialty: String? = null,
    val reason: String = "",
    val notes: String? = null,
    val priority: String = "ROUTINE",
    val status: String = "PENDING",
    val decisionReason: String? = null,
    val decisionAt: String? = null,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val statusDisplay: String
        get() = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

    val priorityDisplay: String
        get() = priority.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

    val isOpen: Boolean
        get() = status == "PENDING" || status == "ACCEPTED"
}

@Serializable
data class CreateReferralDto(
    val patientId: String,
    val patientName: String? = null,
    val encounterId: String,
    val visitId: String? = null,
    val specialistProviderId: String,
    val specialistProviderName: String? = null,
    val specialty: String? = null,
    val reason: String,
    val notes: String? = null,
    val priority: String = "ROUTINE",
)

@Serializable
data class DecideReferralDto(
    val decision: String,
    val reason: String? = null,
)

@Serializable
data class CompleteReferralDto(
    val outcomeNotes: String? = null,
)
