package com.ehealthinformatics.prognocare.data.auth

import android.content.Context
import com.ehealthinformatics.prognocare.navigation.UserRole

/**
 * Session-scoped values kept in the `prognocare_auth` SharedPreferences.
 *
 * Stored alongside the auth token so `SplashViewModel.clearAuthState` (called
 * on sign-out) wipes everything in one `clear()` — a device never carries a
 * patient or staff identity across accounts.
 */
object SessionStore {
    private const val PREFS = "prognocare_auth"
    private const val KEY_PATIENT_ID = "patient_id"
    private const val KEY_PATIENT_NAME = "patient_name"
    private const val KEY_ROLE = "user_role"
    private const val KEY_USER_ID = "identity_user_id"
    private const val KEY_STAFF_ID = "staff_id"
    private const val KEY_STAFF_NAME = "staff_name"
    private const val KEY_STAFF_LOCATION = "staff_location"

    // ── Patient self-identity (patient role) ─────────────────────

    /** The MRN (patient_id) of the patient record linked to this device. */
    fun getPatientId(context: Context): String? =
        prefs(context).getString(KEY_PATIENT_ID, null)?.takeIf { it.isNotBlank() }

    fun getPatientName(context: Context): String? =
        prefs(context).getString(KEY_PATIENT_NAME, null)?.takeIf { it.isNotBlank() }

    fun savePatient(context: Context, patientId: String, patientName: String?) {
        prefs(context).edit()
            .putString(KEY_PATIENT_ID, patientId)
            .putString(KEY_PATIENT_NAME, patientName)
            .apply()
    }

    // ── Role ─────────────────────────────────────────────────────

    /** The role resolved at login (persisted by SplashViewModel.saveAuthState). */
    fun getRole(context: Context): UserRole? {
        val ordinal = prefs(context).getInt(KEY_ROLE, -1)
        return UserRole.entries.getOrNull(ordinal)
    }

    fun saveRole(context: Context, role: UserRole) {
        prefs(context).edit().putInt(KEY_ROLE, role.ordinal).apply()
    }

    // ── Signed-in identity user + linked staff record ────────────

    /** Identity-service user id from GET /api/auth/me. */
    fun getUserId(context: Context): String? =
        prefs(context).getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    fun saveUserId(context: Context, userId: String) {
        prefs(context).edit().putString(KEY_USER_ID, userId).apply()
    }

    /** The EMR staff record linked to the signed-in user (may be null). */
    fun getStaffId(context: Context): String? =
        prefs(context).getString(KEY_STAFF_ID, null)?.takeIf { it.isNotBlank() }

    fun getStaffName(context: Context): String? =
        prefs(context).getString(KEY_STAFF_NAME, null)?.takeIf { it.isNotBlank() }

    fun saveStaff(context: Context, staffId: String, staffName: String?) {
        prefs(context).edit()
            .putString(KEY_STAFF_ID, staffId)
            .putString(KEY_STAFF_NAME, staffName)
            .apply()
    }

    /** Location label resolved for the staff record (optional). */
    fun getStaffLocation(context: Context): String? =
        prefs(context).getString(KEY_STAFF_LOCATION, null)?.takeIf { it.isNotBlank() }

    fun saveStaffLocation(context: Context, location: String) {
        prefs(context).edit().putString(KEY_STAFF_LOCATION, location).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
