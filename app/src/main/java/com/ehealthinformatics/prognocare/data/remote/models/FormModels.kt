package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FormDefinition(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    val category: String = "",
    val schemaJson: JsonElement? = null,
    val version: Int = 1,
    val isPublished: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val categoryDisplay: String
        get() = category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

@Serializable
data class CreateFormDefinitionDto(
    val code: String,
    val name: String,
    val description: String? = null,
    val category: String,
    val schemaJson: JsonElement,
    val version: Int = 1,
)

@Serializable
data class UpdateFormDefinitionDto(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val schemaJson: JsonElement? = null,
    val version: Int? = null,
)

@Serializable
data class PublishFormDto(
    val notes: String? = null,
)

@Serializable
data class FormSubmission(
    val id: String = "",
    val submissionNumber: String? = null,
    val formDefinitionId: String = "",
    val formName: String = "",
    val formVersion: Int = 1,
    val version: Int = 1,
    val patientId: String = "",
    val visitId: String? = null,
    val encounterId: String? = null,
    val dataJson: JsonElement? = null,
    val status: String = "DRAFT",
    val submittedById: String? = null,
    val submittedByName: String? = null,
    val submittedAt: String? = null,
    val amendmentOf: String? = null,
    val amendedFromId: String? = null,
    // Schema-version awareness: true when the submission was filled against
    // an older published schema than the form definition currently exposes.
    val schemaOutdated: Boolean = false,
    val schemaCurrentVersion: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateFormSubmissionDto(
    val formDefinitionId: String,
    val patientId: String,
    val visitId: String? = null,
    val encounterId: String? = null,
    val dataJson: JsonElement,
    val status: String = "DRAFT",
)

@Serializable
data class UpdateFormSubmissionDto(
    val dataJson: JsonElement? = null,
    val visitId: String? = null,
    val encounterId: String? = null,
    val status: String? = null,
)

enum class FormCategory {
    CLINICAL_NOTE, VITALS, ASSESSMENT, SCREENING, PROCEDURE, OTHER,
}

enum class FormSubmissionStatus {
    DRAFT, SUBMITTED, AMENDED,
}
