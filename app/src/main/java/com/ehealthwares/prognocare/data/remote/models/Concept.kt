package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

/** A coding concept row from the concepts service (`/concepts/suggest`). */
@Serializable
data class ConceptRow(
    val id: String = "",
    val concept: String = "",
    val code: String? = null,
    val name: String = "",
    val shortName: String? = null,
    val longName: String? = null,
)

/** A catalogue row (generic product/drug or stock item): `{ id, code, name }`. */
@Serializable
data class CatalogRow(
    val id: String = "",
    val code: String? = null,
    val name: String = "",
)
