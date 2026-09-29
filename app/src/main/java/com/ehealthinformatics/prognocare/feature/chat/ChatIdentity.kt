package com.ehealthinformatics.prognocare.feature.chat

import android.content.Context
import android.provider.Settings

/**
 * Stable chat sender identity for the signed-in user.
 *
 * The conversation engine keys participants by phone: the webhook resolves
 * `senderPhone` → participant, and the gateway delivers replies to
 * `phone:<normalized>` rooms. The identity must therefore be stable across
 * sessions — a token-derived value changes on every re-login and would fork
 * the participant (and orphan old conversations).
 */
object ChatIdentity {

    private const val PREFS = "prognocare_chat"
    private const val KEY_PHONE = "chat_phone"
    private const val KEY_DEVICE_ID = "chat_device_id"

    /**
     * The phone number to send/identify with. Prefers an explicitly saved
     * chat phone (set once the user's number is known), then the session's
     * user phone, then a deterministic device identity.
     */
    fun phone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_PHONE, null)?.takeIf { it.isNotBlank() }?.let { return it }
        context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
            .getString("user_phone", null)?.takeIf { it.isNotBlank() }?.let { return it }
        return deviceId(context)
    }

    /** Persist the user's chat phone (called once the number is known). */
    fun savePhone(context: Context, phone: String) {
        if (phone.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHONE, phone)
            .apply()
    }

    /**
     * Deterministic last-resort identity: a stable ANDROID_ID formatted as a
     * pseudonymous phone-compatible string. Same device → same participant.
     */
    private fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        // Normalize to digits so the engine's phone formatter (which strips
        // non-digits) yields a stable participant key.
        val digits = androidId.filter { it.isDigit() }.padEnd(10, '7').takeLast(10)
        val identity = "234$digits"
        prefs.edit().putString(KEY_DEVICE_ID, identity).apply()
        return identity
    }

    /** The identity user id (JWT `sub`) — resolved from JWT claims by the gateway. */
    fun userId(context: Context): String? = null
}
