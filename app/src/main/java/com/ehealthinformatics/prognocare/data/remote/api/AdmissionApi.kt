package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.Admission
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

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
}
