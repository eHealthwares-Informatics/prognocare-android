package com.ehealthinformatics.prognocare.feature.settings

/**
 * Counts taps toward revealing the hidden server-config panel (easter egg).
 * Taps that are more than [windowMillis] apart reset the counter.
 * Once the [requiredTaps] threshold (default 10) is reached the panel is unlocked.
 */
class TapCounter(
    private val requiredTaps: Int = 10,
    private val windowMillis: Long = 1_500,
) {
    private var lastTapTime: Long = 0L
    private var _count = 0
    var revealed = false
        private set

    /** Taps counted toward the current unlock attempt (resets on a pause). */
    val count: Int get() = _count

    /** Taps still required before the panel unlocks; 0 once revealed. */
    val remainingTaps: Int get() = (requiredTaps - _count).coerceAtLeast(0)

    fun onTap(timestampMillis: Long = System.currentTimeMillis()) {
        if (revealed) return
        if (timestampMillis - lastTapTime > windowMillis) {
            _count = 0
        }
        lastTapTime = timestampMillis
        _count++
        if (_count >= requiredTaps) {
            revealed = true
        }
    }

    fun reset() {
        _count = 0
        lastTapTime = 0L
        revealed = false
    }
}
