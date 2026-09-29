package com.ehealthinformatics.prognocare.feature.forms

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Maps raw values (keyed by clinical *concept*, e.g. "bpSystolic", "spo2")
 * onto the actual field keys of a form definition's schema (e.g.
 * "bloodPressureSystolic", "oxygenSaturation"), so submissions always use
 * whatever key naming the form was seeded/created with — instead of guessing
 * keys client-side and having the server reject or silently drop them.
 *
 * Keys are matched after normalization (lowercase, non-alphanumerics
 * stripped): a schema field maps to a concept when its normalized key equals
 * one of the concept's aliases, or (for aliases of 5+ characters) contains
 * one, so variants like "bloodPressureSystolicMmHg" still match "systolic".
 */
object SchemaKeyMapper {

    /** Canonical concept -> accepted key aliases (normalized). */
    private val conceptAliases: Map<String, Set<String>> = mapOf(
        "temperature" to setOf("temperature", "temp"),
        "heartRate" to setOf("heartrate", "pulse", "heartratebpm"),
        "respiratoryRate" to setOf("respiratoryrate", "resprate"),
        "bpSystolic" to setOf("bloodpressuresystolic", "bpsystolic", "systolic", "systolicbp"),
        "bpDiastolic" to setOf("bloodpressurediastolic", "bpdiastolic", "diastolic", "diastolicbp"),
        "oxygenSaturation" to setOf("oxygensaturation", "spo2", "oxygen"),
        "weight" to setOf("weight", "weightkg"),
        "height" to setOf("height", "heightcm"),
        "bloodGlucose" to setOf("bloodglucose", "glucose"),
        "painScore" to setOf("painscore"),
        "notes" to setOf("notes", "comment", "comments"),
    )

    private val containerTypes = setOf("section", "tab", "col")

    private fun normalize(key: String): String =
        key.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Builds the submission payload: every schema field whose key matches a
     * concept present in [raw] gets that value under the schema's own key.
     * Values not matching any schema field are dropped; blank/null values are
     * skipped.
     */    fun map(schemaJson: JsonElement?, raw: Map<String, Any?>): JsonObject =
        remap(
            schemaJson,
            raw.mapValues { (_, value) ->
                when {
                    value == null -> null
                    value is String && value.isBlank() -> null
                    else -> toJsonPrimitive(value)
                }
            },
        )

    /**
     * Generic submit-payload builder for the dynamic form renderer: resolves
     * each schema field's value from the form's value bag — exact key first,
     * then normalized equality, then concept-alias matching (same rules as
     * [map]) — so custom forms tolerate key drift (concept keys, legacy
     * naming, casing/punctuation variants) instead of silently dropping
     * values. Each bag entry is consumed at most once; entries with no schema
     * field and null/[JsonNull] values are dropped.
     */
    fun remap(schemaJson: JsonElement?, data: Map<String, JsonElement?>): JsonObject {
        val entries: Map<String, JsonElement> = data
            .filterValues { it != null && it !is JsonNull }
            .mapValues { it.value!! }
        if (schemaJson == null) return JsonObject(entries.mapValues { it.value!! })
        val fields = runCatching { schemaJson.jsonObject["fields"]?.jsonArray }.getOrNull()
            ?: return JsonObject(entries.mapValues { it.value!! })

        val used = mutableSetOf<String>()
        val out = linkedMapOf<String, JsonElement>()
        walkFields(fields) { fieldKey, _ ->
            if (out.containsKey(fieldKey)) return@walkFields
            val value = resolveEntry(fieldKey, entries, used) ?: return@walkFields
            out[fieldKey] = value
        }
        return JsonObject(out)
    }

    private fun resolveEntry(
        fieldKey: String,
        entries: Map<String, JsonElement>,
        used: MutableSet<String>,
    ): JsonElement? {
        // 1. exact key
        if (fieldKey in entries && fieldKey !in used) {
            used.add(fieldKey)
            return entries[fieldKey]
        }
        // 2. normalized equality (casing / punctuation variants)
        val normalizedField = normalize(fieldKey)
        entries.keys.firstOrNull { it !in used && normalize(it) == normalizedField }?.let {
            used.add(it)
            return entries[it]
        }
        // 3. concept aliases of the field (e.g. "pulse" -> heartRate)
        val aliases = conceptFor(fieldKey)?.let { conceptAliases[it] } ?: return null
        val match = entries.keys
            .filter { it !in used }
            .firstOrNull { key ->
                val nk = normalize(key)
                aliases.any { alias -> nk == alias || (alias.length >= 5 && nk.contains(alias)) }
            } ?: return null
        used.add(match)
        return entries[match]
    }

    /**
     * Reverse lookup: which clinical concept (if any) does a schema field key
     * represent — same alias rules as [map]. Used to read values back out of
     * a submission for display (e.g. the encounter VitalsCard).
     */
    fun conceptFor(fieldKey: String): String? {
        val normalizedField = normalize(fieldKey)
        for ((concept, aliases) in conceptAliases) {
            if (aliases.any { alias ->
                    normalizedField == alias ||
                        (alias.length >= 5 && normalizedField.contains(alias))
                }
            ) {
                return concept
            }
        }
        return null
    }

    private fun walkFields(fields: JsonArray, visit: (key: String, type: String) -> Unit) {
        for (element in fields) {
            val field = runCatching { element.jsonObject }.getOrNull() ?: continue
            val key = field["key"]?.let { (it as? JsonPrimitive)?.content } ?: continue
            val type = field["type"]?.let { (it as? JsonPrimitive)?.content } ?: ""
            visit(key, type)
            if (type in containerTypes) {
                field["fields"]?.let { child ->
                    runCatching { walkFields(child.jsonArray, visit) }
                }
            }
        }
    }

    private fun isBlank(value: Any?): Boolean = when (value) {
        null -> true
        is String -> value.isBlank()
        else -> false
    }

    private fun toJsonPrimitive(value: Any): JsonPrimitive = when (value) {
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    /** Convenience for tests/debugging: read a primitive back from a payload. */
    fun primitive(payload: JsonObject, key: String): Any? {
        val p = payload[key] as? JsonPrimitive ?: return null
        return p.doubleOrNull ?: p.booleanOrNull ?: p.content
    }
}
