package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.data.remote.models.FormDefinition
import com.ehealthwares.prognocare.feature.forms.ViewFormMap
import com.ehealthwares.prognocare.feature.forms.ViewFormMap.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ViewFormMapTest {

    private fun form(code: String, category: String = "CLINICAL_NOTE") =
        FormDefinition(id = "id-$code", code = code, name = code, category = category)

    @Test
    fun `maps an encounter type to a view`() {
        assertEquals(View.CONSULTATION, ViewFormMap.viewForEncounterType("CONSULTATION"))
        assertEquals(View.VITALS, ViewFormMap.viewForEncounterType("vitals"))
        assertEquals(View.ADMISSION, ViewFormMap.viewForEncounterType("ADMISSION"))
    }

    @Test
    fun `falls back to clinical note for unknown encounter types`() {
        assertEquals(View.CLINICAL_NOTE, ViewFormMap.viewForEncounterType("UNKNOWN_THING"))
    }

    @Test
    fun `picks the preferred form code when available`() {
        val available = listOf(form("VITALS"), form("CLINICAL_NOTE"))
        val picked = ViewFormMap.pick(available, View.CONSULTATION)
        assertEquals("CLINICAL_NOTE", picked!!.code)
    }

    @Test
    fun `falls back to matching category when exact code absent`() {
        val available = listOf(form("VITALS"), form("ADMISSION_NOTE", category = "PROCEDURE"))
        val picked = ViewFormMap.pick(available, View.ADMISSION)
        assertEquals("PROCEDURE", picked!!.category)
    }

    @Test
    fun `returns null when nothing matches`() {
        assertNull(ViewFormMap.pick(listOf(form("VITALS")), View.LAB_RESULTS))
    }

    @Test
    fun `pick returns the actual instance from available list`() {
        val target = form("CLINICAL_NOTE")
        val picked = ViewFormMap.pick(listOf(target), View.CONSULTATION)
        assertSame(target, picked)
    }
}