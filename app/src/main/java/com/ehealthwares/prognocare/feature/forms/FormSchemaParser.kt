package com.ehealthwares.prognocare.feature.forms

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses the backend form `schemaJson` (a JSON object with a `fields` array of
 * [FormFieldSchema]) into typed [FormField] descriptors that the dynamic form UI
 * can render.
 */
object FormSchemaParser {

    fun parse(schemaJson: JsonElement?): FormSchemaData {
        val fields = schemaJson
            ?.jsonObject
            ?.get("fields")
            ?.takeIf { it !is JsonNull }
            ?.let { parseFields(it) }
            ?: emptyList()
        return FormSchemaData(fields)
    }

    private fun parseFields(element: JsonElement): List<FormField> =
        when (element) {
            is JsonArray -> element.mapNotNull { it as? JsonObject }.map(::parseField)
            is JsonObject -> element.get("fields")?.let { parseFields(it) } ?: emptyList()
            else -> emptyList()
        }

    fun parseField(obj: JsonObject): FormField = FormField(
        key = obj.optString("key"),
        label = obj.optString("label"),
        type = FormFieldType.from(obj.optString("type")),
        required = obj["required"]?.jsonPrimitive?.booleanOrNull ?: false,
        options = obj["options"]?.let { stringList(it) } ?: emptyList(),
        placeholder = obj["placeholder"]?.jsonPrimitive?.let { if (it.isString) it.content else null },
        defaultValue = obj["defaultValue"]?.let { toAny(it) },
        rows = obj["rows"]?.jsonPrimitive?.intOrNull ?: 2,
        columns = obj["columns"]?.let { parseColumns(it) } ?: emptyList(),
        fields = obj["fields"]?.let { parseFields(it) } ?: emptyList(),
    )

    private fun parseColumns(element: JsonElement): List<FormTableColumn> =
        when (element) {
            is JsonArray -> element.mapNotNull { it as? JsonObject }.map { col ->
                FormTableColumn(
                    key = col.optString("key"),
                    label = col.optString("label"),
                    type = FormFieldType.from(col.optString("type")),
                )
            }
            else -> emptyList()
        }

    private fun stringList(element: JsonElement): List<String> = when (element) {
        is JsonArray -> element.mapNotNull { it as? JsonPrimitive }.map { it.content }
        else -> emptyList()
    }

    private fun toAny(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.booleanOrNull
            element.intOrNull != null -> element.intOrNull
            else -> element.content.toDoubleOrNull() ?: element.content
        }
        is JsonArray -> element.map { toAny(it) }
        is JsonObject -> element.entries.associate { (k, v) -> k to toAny(v) }
    }
}

private fun JsonObject.optString(key: String): String =
    this[key]?.jsonPrimitive?.takeIf { it.isString }?.content ?: ""
