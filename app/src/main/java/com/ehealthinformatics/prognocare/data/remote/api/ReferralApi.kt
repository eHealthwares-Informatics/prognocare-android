package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CompleteReferralDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateReferralDto
import com.ehealthinformatics.prognocare.data.remote.models.DecideReferralDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.Referral
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReferralApi {

    @GET("api/referrals")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null,
        @Query("direction") direction: String? = null,
        @Query("providerId") providerId: String? = null,
        @Query("patientId") patientId: String? = null,
        @Query("encounterId") encounterId: String? = null,
    ): Response<PaginatedResponse<Referral>>

    @GET("api/referrals/{id}")
    suspend fun getById(@Path("id") id: String): Response<Referral>

    @POST("api/referrals")
    suspend fun create(@Body dto: CreateReferralDto): Response<Referral>

    @POST("api/referrals/{id}/decide")
    suspend fun decide(
        @Path("id") id: String,
        @Body dto: DecideReferralDto,
    ): Response<Referral>

    @POST("api/referrals/{id}/complete")
    suspend fun complete(
        @Path("id") id: String,
        @Body dto: CompleteReferralDto,
    ): Response<Referral>

    @DELETE("api/referrals/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
