package com.ehealthinformatics.prognocare.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed route classes for type-safe navigation.
 * Each role gets its own route tree to prevent navigation errors.
 */

// ── Auth ──────────────────────────────────────────────────────

object Routes {
    const val LOGIN = "login"
    const val SPLASH = "splash"
    const val SETTINGS = "settings"
}

// ── Doctor ────────────────────────────────────────────────────

object DoctorRoutes {
    const val DASHBOARD = "doctor/dashboard"
    const val APPOINTMENTS = "doctor/appointments"
    const val PATIENT_LIST = "doctor/patients"
    const val PATIENT_DETAIL = "doctor/patients/{patientId}"
    const val ENCOUNTER = "doctor/encounters/{encounterId}"
    const val REQUESTS = "doctor/requests"
    const val CREATE_REQUEST = "doctor/requests/create"
    const val REQUEST_DETAIL = "doctor/requests/{requestId}"

    fun patientDetail(patientId: String) = "doctor/patients/$patientId"
    fun encounter(encounterId: String) = "doctor/encounters/$encounterId"
    fun requestDetail(requestId: String) = "doctor/requests/$requestId"
}

// ── Nurse ─────────────────────────────────────────────────────

object NurseRoutes {
    const val DASHBOARD = "nurse/dashboard"
    /** Vitals history list (FAB opens the recording form). */
    const val VITALS = "nurse/vitals"
    /** Vitals recording form. */
    const val VITALS_RECORD = "nurse/vitals/record"
    const val PATIENT_LIST = "nurse/patients"
    const val CHECKIN = "nurse/checkin"
    const val MEDICATIONS = "nurse/medications"
    const val TASKS = "nurse/tasks"
}

// ── Patient ───────────────────────────────────────────────────

object PatientRoutes {
    const val DASHBOARD = "patient/dashboard"
    const val APPOINTMENTS = "patient/appointments"
    const val BOOKING = "patient/booking"
    const val RECORDS = "patient/records"
    const val MEDICATIONS = "patient/medications"
}

// ── Specialist ────────────────────────────────────────────────

object SpecialistRoutes {
    const val DASHBOARD = "specialist/dashboard"
    const val REFERRALS = "specialist/referrals"
    const val PATIENT_LIST = "specialist/patients"
    const val REFERRAL_DETAIL = "specialist/referrals/{referralId}"
    const val CONSULTATION_NOTES = "specialist/consultation-notes"

    fun referralDetail(referralId: String) = "specialist/referrals/$referralId"
}

// ── Therapist ─────────────────────────────────────────────────

object TherapistRoutes {
    const val DASHBOARD = "therapist/dashboard"
    const val SESSIONS = "therapist/sessions"
    const val PATIENT_LIST = "therapist/patients"
    const val SESSION_DETAIL = "therapist/sessions/{sessionId}"
    const val THERAPY_PLAN = "therapist/plan"
    const val ASSESSMENT = "therapist/assessment"

    fun sessionDetail(sessionId: String) = "therapist/sessions/$sessionId"
}

// ── Technician ────────────────────────────────────────────────

object TechnicianRoutes {
    const val DASHBOARD = "technician/dashboard"
    const val ORDERS = "technician/orders"
    const val RESULTS = "technician/results"
}

// ── Support ───────────────────────────────────────────────────

object SupportRoutes {
    const val DASHBOARD = "support/dashboard"
    const val CHECKIN = "support/checkin"
    const val REQUESTS = "support/requests"
}

// ── Finance ───────────────────────────────────────────────────

object FinanceRoutes {
    const val DASHBOARD = "finance/dashboard"
    const val BILLS = "finance/bills"
    const val BILL_DETAIL = "finance/bills/{billId}"
    const val PAYMENTS = "finance/payments"
    const val PATIENT_SEARCH = "finance/patients"

    fun billDetail(billId: String) = "finance/bills/$billId"
}

// ── Admin ─────────────────────────────────────────────────────

object AdminRoutes {
    const val DASHBOARD = "admin/dashboard"
    const val PATIENT_SEARCH = "admin/patients"
    const val REGISTER_PATIENT = "admin/patients/register"
    const val CHECKIN = "admin/checkin"
    const val STAFF = "admin/staff"
    const val FACILITIES = "admin/facilities"
    const val ANALYTICS = "admin/analytics"
}

// ── Chat (shared across all roles) ────────────────────────────

object ChatRoutes {
    const val CONVERSATIONS = "chat"
    const val CONVERSATION_DETAIL = "chat/{conversationId}"

    /**
     * Sentinel for a not-yet-existing conversation: the screen sends without a
     * conversation id (newConversation=true) and adopts the id the engine
     * mints from the first reply event — the same flow as the web widget.
     */
    const val NEW_CONVERSATION_ID = "new"

    fun conversationDetail(conversationId: String) = "chat/$conversationId"
}

// ── Notifications (shared across all roles) ───────────────────

object NotificationRoutes {
    const val NOTIFICATIONS = "notifications"
}

// ── Profile (shared across all roles) ──────────────────────────

object ProfileRoutes {
    const val PROFILE = "profile"
}

// ── Clinical records (appointments / visits / encounters, shared) ─

object ClinicalRoutes {
    const val APPOINTMENTS = "clinical/appointments"
    const val VISITS = "clinical/visits"
    const val ENCOUNTERS = "clinical/encounters"
}

// ── Forms (dynamic documentation, shared) ──────────────────────

object FormsRoutes {
    const val PICKER = "forms?patientId={patientId}&visitId={visitId}&encounterId={encounterId}"
    const val FORM = "forms/{formId}?patientId={patientId}&visitId={visitId}&encounterId={encounterId}"

    fun picker(patientId: String? = null, visitId: String? = null, encounterId: String? = null): String =
        "forms?patientId=${patientId.orEmpty()}&visitId=${visitId.orEmpty()}&encounterId=${encounterId.orEmpty()}"

    fun form(formId: String, patientId: String? = null, visitId: String? = null, encounterId: String? = null): String =
        "forms/$formId?patientId=${patientId.orEmpty()}&visitId=${visitId.orEmpty()}&encounterId=${encounterId.orEmpty()}"
}

// ── Role enum ─────────────────────────────────────────────────

enum class UserRole(val displayName: String, val route: String) {
    Doctor("Doctor", DoctorRoutes.DASHBOARD),
    Nurse("Nurse", NurseRoutes.DASHBOARD),
    Patient("Patient", PatientRoutes.DASHBOARD),
    Specialist("Specialist", SpecialistRoutes.DASHBOARD),
    Therapist("Therapist", TherapistRoutes.DASHBOARD),
    Technician("Technician", TechnicianRoutes.DASHBOARD),
    Support("Support", SupportRoutes.DASHBOARD),
    Finance("Finance", FinanceRoutes.DASHBOARD),
    Admin("Admin", AdminRoutes.DASHBOARD),
}
