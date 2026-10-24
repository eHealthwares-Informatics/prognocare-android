package com.ehealthwares.prognocare.feature.auth

import com.ehealthwares.prognocare.data.remote.models.MeResponse
import com.ehealthwares.prognocare.navigation.UserRole

/**
 * Maps identity-service role codes / modules to the Android [UserRole].
 *
 * Identity uses functional/access role codes (admin, super_admin, emr_clinician,
 * lis_technician, cashier, customer, conversation_operator, ...) which do not map
 * 1:1 to clinical titles. This mapper picks the most specific supported role the
 * signed-in user can act as, falling back to the safest default.
 */
object UserRoleMapper {

    private const val MODULE_EMR = "emr"
    private const val MODULE_RXSOFT = "rxsoft"
    private const val MODULE_LIS = "lis"
    private const val MODULE_COMMUNICATION = "communication"
    private const val MODULE_CONVERSATION = "conversation"

    private val ROLE_TO_USER_ROLE: Map<String, UserRole> = mapOf(
        "admin" to UserRole.Admin,
        "admin_operator" to UserRole.Admin,
        "super_admin" to UserRole.Admin,
        "cashier" to UserRole.Finance,
        "auditor" to UserRole.Finance,
        "emr_clinician" to UserRole.Doctor,
        "doctor" to UserRole.Doctor,
        "nurse" to UserRole.Nurse,
        "lis_technician" to UserRole.Technician,
        "technician" to UserRole.Technician,
        "therapist" to UserRole.Therapist,
        "specialist" to UserRole.Specialist,
        "conversation_operator" to UserRole.Support,
        "communication_manager" to UserRole.Support,
        "customer" to UserRole.Patient,
    )

    /**
     * Determines the primary Android [UserRole] for a signed-in user.
     *
     * Resolution order:
     *  1. First role code with a direct mapping.
     *  2. Otherwise, derive from accessible modules (EMR => Doctor, LIS => Technician,
     *     RxSoft => Technician, communication/conversation => Support).
     *  3. Fallback to [UserRole.Doctor].
     */
    fun map(me: MeResponse): UserRole {
        me.roles.firstNotNullOfOrNull { ROLE_TO_USER_ROLE[it.lowercase()] }?.let { return it }

        val modules = me.modules.map { it.id.lowercase() }
        when {
            MODULE_RXSOFT in modules -> return UserRole.Technician
            MODULE_LIS in modules -> return UserRole.Technician
            MODULE_EMR in modules -> return UserRole.Doctor
            MODULE_COMMUNICATION in modules -> return UserRole.Support
            MODULE_CONVERSATION in modules -> return UserRole.Support
        }

        return UserRole.Doctor
    }

    /** True if the given Android [UserRole] name is among the signer's backend role codes. */
    fun hasRole(me: MeResponse, role: UserRole): Boolean {
        val backendRole = role.name.lowercase()
        val allPossible = ROLE_TO_USER_ROLE.filterValues { it == role }.keys
        return me.roles.any { r -> r.lowercase() == backendRole || r.lowercase() in allPossible }
    }
}