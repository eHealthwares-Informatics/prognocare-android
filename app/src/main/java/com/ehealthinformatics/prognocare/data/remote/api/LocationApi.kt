package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.Location
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LocationApi {

    @GET("locations")
    suspend fun list(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1,
    ): Response<PaginatedResponse<Location>>

    @GET("locations/{id}")
    suspend fun getById(@Path("id") id: String): Response<Location>
}