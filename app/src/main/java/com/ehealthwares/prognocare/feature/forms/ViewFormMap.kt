package com.ehealthwares.prognocare.feature.forms

import com.ehealthwares.prognocare.data.remote.models.FormDefinition

/**
 * Maps a clinical view/screen (encounter type or documentation intent) to one or
 * more preferred backend form codes. Used to pre-select the relevant form when the
 * user opens a documentation view from a patient/encounter context.
 *
 * The EMR serves forms dynamically via `GET /forms/available`; codes are authored
 * with the form builder and governed by `form_access`. This map is a UI convention
 * for picking the best match rather than a strict backend contract.
 */
object ViewFormMap {

    enum class View(val codes: List<String>) {
        CONSULTATION(listOf("CONSULTATION_NOTE", "CLINICAL_NOTE")),
        CLINICAL_NOTE(listOf("CLINICAL_NOTE")),
        VITALS(listOf("VITALS")),
        HISTORY_AND_PHYSICAL(listOf("HISTORY_AND_PHYSICAL", "CLINICAL_NOTE")),
        SCREENING(listOf("SCREENING")),
        ASSESSMENT(listOf("ASSESSMENT", "SCREENING")),
        PROCEDURE(listOf("PROCEDURE")),
        ADMISSION(listOf("ADMISSION")),
        DISCHARGE(listOf("DISCHARGE", "CLINICAL_NOTE")),
        LAB_RESULTS(listOf("LAB_RESULTS")),
    }

    private val BY_CODE: Map<String, View> = run {
        val m = mutableMapOf<String, View>()
        for (view in View.entries) for (code in view.codes) m[code] = view
        m
    }

    /** Returns the suggested view for an encounter type string (case-insensitive). */
    fun viewForEncounterType(encounterType: String): View =
        runCatching { View.valueOf(encounterType.uppercase()) }
            .getOrElse {
                when (encounterType.uppercase()) {
                    "CONSULTATION" -> View.CONSULTATION
                    "HISTORY_AND_PHYSICAL" -> View.HISTORY_AND_PHYSICAL
                    "LAB_RESULTS" -> View.LAB_RESULTS
                    "DISCHARGE" -> View.DISCHARGE
                    "PROCEDURE" -> View.PROCEDURE
                    "ADMISSION" -> View.ADMISSION
                    else -> View.CLINICAL_NOTE
                }
            }

    /**
     * Picks the best form from the available set for a view, preferring the first
     * preferred code that exists; falls back to a form whose category matches the
     * view category.
     */
    fun pick(available: List<FormDefinition>, view: View): FormDefinition? {
        for (code in view.codes) {
            available.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it }
        }
        val category = categoryFor(view)
        return available.firstOrNull { it.category.equals(category, ignoreCase = true) }
    }

    /** Preferred form category for a view, used as a fallback match. */
    fun categoryFor(view: View): String = when (view) {
        View.CONSULTATION, View.CLINICAL_NOTE,
        View.HISTORY_AND_PHYSICAL, View.DISCHARGE -> "CLINICAL_NOTE"
        View.VITALS -> "VITALS"
        View.SCREENING, View.ASSESSMENT -> "SCREENING"
        View.PROCEDURE, View.ADMISSION -> "PROCEDURE"
        View.LAB_RESULTS -> "OTHER"
    }
}