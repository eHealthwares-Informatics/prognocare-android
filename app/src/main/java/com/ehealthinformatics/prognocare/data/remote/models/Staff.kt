package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Staff(
    val id: String = "",
    val staffNumber: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val otherNames: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hireDate: String? = null,
    val roleType: String = "",
    val category: String? = null,
    val department: String? = null,
    val identityLocationId: String? = null,
    val userId: String? = null,
    val isActive: Boolean = true,
    val otherDetails: Map<String, String>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val displayName: String
        get() = "$firstName $lastName".trim()

    /** Name with department in brackets, e.g. `Dr. Fatima Bello (Cardiology)`. */
    val displayWithDepartment: String
        get() {
            val name = displayName
            val dept = department?.trim().orEmpty()
            return if (dept.isEmpty()) name else "$name ($dept)"
        }

    val roleDisplay: String
        get() = roleType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

@Serializable
data class CreateStaffDto(
    val staffNumber: String? = null,
    val firstName: String,
    val lastName: String,
    val otherNames: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hireDate: String? = null,
    val roleType: String,
    val category: String? = null,
    val department: String? = null,
    val identityLocationId: String? = null,
    val userId: String? = null,
    val isActive: Boolean? = null,
    val otherDetails: Map<String, String>? = null,
)

@Serializable
data class UpdateStaffDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val otherNames: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hireDate: String? = null,
    val roleType: String? = null,
    val category: String? = null,
    val department: String? = null,
    val identityLocationId: String? = null,
    val userId: String? = null,
    val isActive: Boolean? = null,
    val otherDetails: Map<String, String>? = null,
)

enum class StaffRoleType {
    Doctor, Nurse, Technician, Therapist, Admin, Support,
}

enum class StaffCategory {
    Medical, Nursing, @SerialName("Allied Health") AlliedHealth,
    Pharmacy, Laboratory, Administrative, Support, Other,
}
