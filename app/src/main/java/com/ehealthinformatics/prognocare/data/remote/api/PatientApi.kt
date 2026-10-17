package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreatePatientDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.UpdatePatientDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PatientApi {

    @GET("patients")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("filter") filter: String? = null,
    ): Response<PaginatedResponse<Patient>>

    @POST("patients")
    suspend fun create(@Body dto: CreatePatientDto): Response<Patient>

    @GET("patients/{id}")
    suspend fun getById(@Path("id") id: String): Response<Patient>

    @GET("patients/by-mrn/{patientId}")
    suspend fun getByMrn(@Path("patientId") mrn: String): Response<Patient>

    @PATCH("patients/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdatePatientDto,
    ): Response<Patient>

    @DELETE("patients/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
