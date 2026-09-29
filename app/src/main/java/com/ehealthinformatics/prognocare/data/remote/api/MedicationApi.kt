package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.AdministerMedicationDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateMedicationDto
import com.ehealthinformatics.prognocare.data.remote.models.Medication
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MedicationApi {

    @GET("api/medications")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null,
        @Query("patientId") patientId: String? = null,
        @Query("requestId") requestId: String? = null,
        @Query("encounterId") encounterId: String? = null,
        @Query("administered") administered: String? = null,
    ): Response<PaginatedResponse<Medication>>

    @GET("api/medications/{id}")
    suspend fun getById(@Path("id") id: String): Response<Medication>

    @POST("api/medications")
    suspend fun create(@Body dto: CreateMedicationDto): Response<Medication>

    @POST("api/medications/{id}/administer")
    suspend fun administer(
        @Path("id") id: String,
        @Body dto: AdministerMedicationDto,
    ): Response<Medication>

    @DELETE("api/medications/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
