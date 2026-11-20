package com.ehealthwares.prognocare.data.remote.api

import com.ehealthwares.prognocare.data.remote.models.CatalogRow
import com.ehealthwares.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** rxsoft core catalogue (`/items`), used by stock-item form fields. */
interface ItemsApi {

    @GET("items")
    suspend fun list(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<CatalogRow>>
}
