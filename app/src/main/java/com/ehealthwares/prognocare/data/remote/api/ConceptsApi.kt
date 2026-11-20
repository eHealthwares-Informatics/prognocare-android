package com.ehealthwares.prognocare.data.remote.api

import com.ehealthwares.prognocare.data.remote.models.CatalogRow
import com.ehealthwares.prognocare.data.remote.models.ConceptRow
import com.ehealthwares.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Concepts service (base URL ends at `/api/v1/` in development or `/coding/`
 * behind the production gateway).
 */
interface ConceptsApi {

    @GET("concepts/suggest")
    suspend fun suggest(
        @Query("concept") concept: String,
        @Query("term") term: String? = null,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<ConceptRow>>

    @GET("generic-products")
    suspend fun genericProducts(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<CatalogRow>>

    @GET("generic-drugs")
    suspend fun genericDrugs(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<CatalogRow>>
}
