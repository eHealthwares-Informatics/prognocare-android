package com.ehealthinformatics.prognocare.feature.forms

import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Reads vital-sign values back out of VITALS form submissions for display
 * (the encounter screen's VitalsCard). Values are located with
 * [SchemaKeyMapper.conceptFor], so any key naming the form uses resolves.
 */
object VitalsReadings {

    /** One display row on the VitalsCard. */
    data class Reading(val label: String, val value: String)

    data class Vitals(
        val temperature: String? = null,
        val bloodPressure: String? = null,
        val heartRate: String? = null,
        val respiratoryRate: String? = null,
        val oxygenSaturation: String? = null,
        val weight: String? = null,
        val height: String? = null,
        val bloodGlucose: String? = null,
        val painScore: String? = null,
        val notes: String? = null,
        val recordedAt: String? = null,
        val recordedBy: String? = null,
    ) {
        /** Non-null readings in stable display order. */
        fun rows(): List<Reading> = listOfNotNull(
            temperature?.let { Reading("Temperature", it) },
            bloodPressure?.let { Reading("Blood pressure", it) },
            heartRate?.let { Reading("Heart rate", it) },
            respiratoryRate?.let { Reading("Respiratory rate", it) },
            oxygenSaturation?.let { Reading("SpO₂", it) },
            weight?.let { Reading("Weight", it) },
            height?.let { Reading("Height", it) },
            bloodGlucose?.let { Reading("Blood glucose", it) },
            painScore?.let { Reading("Pain score", it) },
            notes?.takeIf { it.isNotBlank() }?.let { Reading("Notes", it) },
        )
    }

    /**
     * The most recent VITALS submission (form name contains "vital", not a
     * DRAFT), or null when the list has none. Ordering uses the submission
     * timestamp (ISO strings compare chronologically).
     */
    fun latest(submissions: List<FormSubmission>): Vitals? {
        val vitals = submissions
            .filter { it.formName.contains("vital", ignoreCase = true) }
            .filter { it.status != "DRAFT" }
            .maxByOrNull { it.submittedAt ?: it.createdAt.orEmpty() }
            ?: return null
        return from(vitals)
    }

    /** Extracts a [Vitals] from one submission's dataJson via schema concepts. */
    fun from(submission: FormSubmission): Vitals? {
        val data = runCatching { submission.dataJson?.jsonObject }.getOrNull()
            ?: return null
        // concept -> schema key present in this payload
        val byConcept = data.entries.mapNotNull { (key, _) ->
            SchemaKeyMapper.conceptFor(key)?.let { it to key }
        }.toMap()
        // No recognizable vital signs in the payload — nothing to display.
        if (byConcept.isEmpty()) return null

        fun value(concept: String): JsonPrimitive? =
            byConcept[concept]?.let { data[it] as? JsonPrimitive }

        val systolic = value("bpSystolic")?.doubleOrNull
        val diastolic = value("bpDiastolic")?.doubleOrNull
        val bloodPressure = when {
            systolic != null && diastolic != null -> "${formatNumber(systolic)}/${formatNumber(diastolic)} mmHg"
            systolic != null -> "${formatNumber(systolic)} mmHg (systolic)"
            diastolic != null -> "${formatNumber(diastolic)} mmHg (diastolic)"
            else -> null
        }

        return Vitals(
            temperature = value("temperature")?.doubleOrNull?.let { "${formatNumber(it)} °C" },
            bloodPressure = bloodPressure,
            heartRate = value("heartRate")?.doubleOrNull?.let { "${formatNumber(it)} bpm" },
            respiratoryRate = value("respiratoryRate")?.doubleOrNull?.let { "${formatNumber(it)} /min" },
            oxygenSaturation = value("oxygenSaturation")?.doubleOrNull?.let { "${formatNumber(it)} %" },
            weight = value("weight")?.doubleOrNull?.let { "${formatNumber(it)} kg" },
            height = value("height")?.doubleOrNull?.let { "${formatNumber(it)} cm" },
            bloodGlucose = value("bloodGlucose")?.doubleOrNull?.let { "${formatNumber(it)} mg/dL" },
            painScore = value("painScore")?.doubleOrNull?.let { "${formatNumber(it)} /10" },
            notes = value("notes")?.content,
            recordedAt = submission.submittedAt ?: submission.createdAt,
            recordedBy = submission.submittedByName,
        )
    }

    /** 72.0 → "72", 37.5 → "37.5" — no trailing zeros. */
    internal fun formatNumber(number: Double): String =
        if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
}
