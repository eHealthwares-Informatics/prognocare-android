package com.ehealthinformatics.prognocare.data.auth

import android.content.Context

/**
 * Session-scoped values kept in the `prognocare_auth` SharedPreferences.
 *
 * Stored alongside the auth token so `SplashViewModel.clearAuthState` (called
 * on sign-out) wipes everything in one `clear()` — a device never carries a
 * patient identity across accounts.
 */
object SessionStore {
    private const val PREFS = "prognocare_auth"
    private const val KEY_PATIENT_ID = "patient_id"
    private const val KEY_PATIENT_NAME = "patient_name"

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
