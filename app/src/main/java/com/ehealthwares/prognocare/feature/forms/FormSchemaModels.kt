package com.ehealthwares.prognocare.feature.forms

/** Mirrors the backend FormFieldType enum. */
enum class FormFieldType {
    TEXT, TEXTAREA, NUMBER, DATE, DATETIME, SELECT, RADIO, CHECKBOX, CHECKBOX_GROUP,
    TABLE, SECTION, TAB, COL;

    companion object {
        fun from(raw: String?): FormFieldType = remember(raw)
        private fun remember(raw: String?): FormFieldType = runCatching {
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
)

/** The full parsed form schema. */
data class FormSchemaData(
    val fields: List<FormField>,
)

/** The value bag collected by the dynamic form UI, keyed by field key. */
typealias FormData = Map<String, Any?>
