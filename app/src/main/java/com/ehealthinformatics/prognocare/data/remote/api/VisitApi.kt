package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreateVisitDto
import com.ehealthinformatics.prognocare.data.remote.models.EndVisitDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import com.ehealthinformatics.prognocare.data.remote.models.UpdateVisitDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VisitApi {

    @GET("api/visits")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
        @Query("providerId") providerId: String? = null,
        @Query("patientId") patientId: String? = null,
    ): Response<PaginatedResponse<Visit>>

    @POST("api/visits")
    suspend fun create(@Body dto: CreateVisitDto): Response<Visit>

    @GET("api/visits/active")
    suspend fun active(): Response<List<Visit>>

    @GET("api/visits/{id}")
    suspend fun getById(@Path("id") id: String): Response<Visit>

    @PATCH("api/visits/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateVisitDto,
    ): Response<Visit>

    @DELETE("api/visits/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("api/visits/{id}/end")
    suspend fun end(
        @Path("id") id: String,
        @Body dto: EndVisitDto = EndVisitDto(),
    ): Response<Visit>

    @POST("api/visits/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): Response<Visit>
}
