package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreateEncounterDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateEncounterRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.UpdateEncounterDto
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EncounterApi {

    @GET("encounters")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("patientId") patientId: String? = null,
        @Query("visitId") visitId: String? = null,
    ): Response<PaginatedResponse<Encounter>>

    @POST("encounters")
    suspend fun create(@Body dto: CreateEncounterDto): Response<Encounter>

    @GET("encounters/{id}")
    suspend fun getById(@Path("id") id: String): Response<Encounter>

    @PATCH("encounters/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateEncounterDto,
    ): Response<Encounter>

    @DELETE("encounters/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("encounters/{id}/requests")
    suspend fun createRequest(
        @Path("id") id: String,
        @Body dto: CreateEncounterRequestDto,
    ): Response<ClinicalRequest>
}
