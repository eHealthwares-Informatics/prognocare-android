package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.feature.settings.TapCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapCounterTest {

    @Test
    fun `default threshold is ten taps`() {
        val counter = TapCounter()
        repeat(10) { counter.onTap(it * 200L) }
        assertTrue(counter.revealed)
    }

    @Test
    fun `nine taps do not reveal`() {
        val counter = TapCounter()
        repeat(9) { counter.onTap(it * 200L) }
        assertFalse(counter.revealed)
    }

    @Test
    fun `remaining taps count down to zero`() {
        val counter = TapCounter(requiredTaps = 10)
        counter.onTap(0)
        counter.onTap(100)
        assertEquals(8, counter.remainingTaps)
        assertEquals(2, counter.count)
        repeat(8) { counter.onTap(200L + it * 200L) }
        assertEquals(0, counter.remainingTaps)
        assertTrue(counter.revealed)
    }

    @Test
    fun `ten taps within window reveal panel`() {
        val counter = TapCounter(requiredTaps = 10, windowMillis = 1_500)
        var now = 0L
        repeat(10) {
            counter.onTap(now)
            now += 200
        }
        assertTrue(counter.revealed)
    }

    @Test
    fun `fewer than required taps do not reveal`() {
        val counter = TapCounter(requiredTaps = 10, windowMillis = 1_500)
        repeat(9) { counter.onTap(it * 200L) }
        assertFalse(counter.revealed)
    }

    @Test
    fun `slow taps reset the counter`() {
        val counter = TapCounter(requiredTaps = 3, windowMillis = 1_000)
        counter.onTap(0)
        counter.onTap(2_000) // outside window: restarts at 1
        counter.onTap(2_100) // within window: 2
        assertFalse(counter.revealed)
        assertEquals(2, counter.count)
    }

    @Test
    fun `reset clears revealed state`() {
        val counter = TapCounter(requiredTaps = 2, windowMillis = 1_000)
        counter.onTap(0)
        counter.onTap(1)
        assertTrue(counter.revealed)
        counter.reset()
        assertFalse(counter.revealed)
        assertEquals(0, counter.count)
    }
}
