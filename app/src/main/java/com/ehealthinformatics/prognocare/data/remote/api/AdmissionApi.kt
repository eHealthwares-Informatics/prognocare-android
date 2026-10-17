package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.Admission
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class AdmitFromVisitDto(
    val wardId: String? = null,
    val bedId: String? = null,
    val admissionType: String? = null,
    val diagnosis: String? = null,
    val notes: String? = null,
)

@Serializable
data class DischargeDto(
    val dischargeType: String? = null,
    val dischargeSummary: String? = null,
    val dischargeDatetime: String? = null,
)

interface AdmissionApi {

    @GET("admissions")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
        @Query("patientId") patientId: String? = null,
    ): Response<PaginatedResponse<Admission>>

    @GET("admissions/{id}")
    suspend fun getById(@Path("id") id: String): Response<Admission>

    @POST("admissions/from-visit/{visitId}")
    suspend fun admitFromVisit(
        @Path("visitId") visitId: String,
        @Body dto: AdmitFromVisitDto,
    ): Response<Admission>

    @POST("admissions/{id}/discharge")
    suspend fun discharge(
        @Path("id") id: String,
        @Body dto: DischargeDto = DischargeDto(),
    ): Response<Admission>
}