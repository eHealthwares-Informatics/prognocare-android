package com.ehealthwares.prognocare.data.remote.api

import com.ehealthwares.prognocare.data.remote.models.Department
import com.ehealthwares.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DepartmentApi {

    @GET("departments")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("search") search: String? = null,
        @Query("locationId") locationId: String? = null,
        @Query("isActive") isActive: Boolean? = null,
    ): Response<PaginatedResponse<Department>>
}
