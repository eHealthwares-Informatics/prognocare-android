package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val meta: PaginationMeta? = null,
)

@Serializable
data class PaginationMeta(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 0,
)

enum class SortOrder { ASC, DESC }
