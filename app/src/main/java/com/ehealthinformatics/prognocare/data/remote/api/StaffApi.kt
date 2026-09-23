package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreateStaffDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.data.remote.models.UpdateStaffDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StaffApi {

    @GET("api/staff")
    suspend fun list(
        @Query("search") search: String? = null,
        @Query("roleType") roleType: String? = null,
        @Query("category") category: String? = null,
        @Query("department") department: String? = null,
        @Query("isActive") isActive: Boolean? = null,
        @Query("userId") userId: String? = null,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<Staff>>

    @POST("api/staff")
    suspend fun create(@Body dto: CreateStaffDto): Response<Staff>

    @GET("api/staff/{id}")
    suspend fun getById(@Path("id") id: String): Response<Staff>

    @PATCH("api/staff/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateStaffDto,
    ): Response<Staff>

    @DELETE("api/staff/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
