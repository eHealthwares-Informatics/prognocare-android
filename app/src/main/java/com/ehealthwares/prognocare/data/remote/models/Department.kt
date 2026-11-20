package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

/** Functional department (EMR `/departments`). */
@Serializable
data class Department(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val departmentType: String = "",
    val description: String? = null,
    val isActive: Boolean = true,
    val parentId: String? = null,
    val locationId: String? = null,
) {
    val displayName: String
        get() = listOf(code, name).filter { it.isNotBlank() }.joinToString(" · ")

    /** Value written into a `department` form field (mirrors the web). */
    fun toFieldValue(): Map<String, Any?> = buildMap {
        put("id", id)
        put("code", code)
        put("name", name)
        put("departmentType", departmentType)
    }
}
