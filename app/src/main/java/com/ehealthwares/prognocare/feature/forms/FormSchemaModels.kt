package com.ehealthwares.prognocare.feature.forms

/** Mirrors the backend FormFieldType enum. */
enum class FormFieldType {
    TEXT, TEXTAREA, NUMBER, DATE, DATETIME, SELECT, RADIO, CHECKBOX, CHECKBOX_GROUP,
    TABLE, SECTION, TAB, COL, ANNOTATE, HTML, HIDDEN,
    PATIENT, AUTH_PATIENT, PRACTITIONER, DEPARTMENT, FILE,
    ITEM, INVENTORY_ITEM, GENERIC_PRODUCT, GENERIC_DRUG, LOINC_TEST, DICOM_TEST,
    QR_CODE, PRINT, SEND_FORM;

    companion object {
        fun from(raw: String?): FormFieldType = runCatching {
            valueOf(raw?.uppercase()?.replace('-', '_').orEmpty())
        }.getOrDefault(TEXT)
    }
}

/** A column definition used inside a table field. */
data class FormTableColumn(
    val key: String,
    val label: String,
    val type: FormFieldType,
)

/** Target of an `annotate` field. */
enum class AnnotateTarget {
    BODY, DERMATOME, ORGAN;

    companion object {
        fun from(raw: String?): AnnotateTarget = when (raw?.lowercase()) {
            "dermatome" -> DERMATOME
            "organ" -> ORGAN
            else -> BODY
        }
    }
}

/**
 * Configuration for an `annotate` field (mirrors the web `AnnotateConfig`).
 * Value shape: body/organ → `List<String>` of ids; dermatome → `String` id.
 */
data class AnnotateConfig(
    val target: AnnotateTarget = AnnotateTarget.BODY,
    val multiple: Boolean = true,
    /** Restricts body regions to these ids (empty = all). */
    val regions: List<String> = emptyList(),
    /** Organ slug (e.g. `heart`) for the organ target. */
    val organ: String? = null,
    /** Restricts organ structures to these ids (empty = all). */
    val structures: List<String> = emptyList(),
    val views: List<String> = emptyList(),
    val showLabels: Boolean = true,
)

/** A single field in a dynamic form schema. */
data class FormField(
    val key: String,
    val label: String,
    val type: FormFieldType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val placeholder: String? = null,
    val defaultValue: Any? = null,
    val rows: Int = 2,
    val columns: List<FormTableColumn> = emptyList(),
    val fields: List<FormField> = emptyList(),
    val annotate: AnnotateConfig? = null,
    val html: String? = null,
    /** `file` field: accepted MIME types (comma-separated, e.g. image/jpeg,application/pdf). */
    val accept: String? = null,
    /** `file` field: allow more than one attachment. */
    val multiple: Boolean = false,
    /** `file` field: client-side size cap in MB. */
    val maxSizeMb: Int = 10,
)

/** The full parsed form schema. */
data class FormSchemaData(
    val fields: List<FormField>,
)

/** The value bag collected by the dynamic form UI, keyed by field key. */
typealias FormData = Map<String, Any?>
