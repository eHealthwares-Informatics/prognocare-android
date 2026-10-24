package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: String = "",
    val organizationId: String? = null,
    val code: String = "",
    val name: String = "",
    val parentId: String? = null,
    val isActive: Boolean = true,
)