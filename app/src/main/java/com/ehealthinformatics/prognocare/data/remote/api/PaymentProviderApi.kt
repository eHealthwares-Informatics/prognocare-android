package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreatePaymentProviderDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.PaymentProvider
import com.ehealthinformatics.prognocare.data.remote.models.UpdatePaymentProviderDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentProviderApi {

    @GET("payment-providers")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
    ): Response<PaginatedResponse<PaymentProvider>>

    @POST("payment-providers")
    suspend fun create(@Body dto: CreatePaymentProviderDto): Response<PaymentProvider>

    @GET("payment-providers/{id}")
    suspend fun getById(@Path("id") id: String): Response<PaymentProvider>

    @PATCH("payment-providers/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdatePaymentProviderDto,
    ): Response<PaymentProvider>

    @DELETE("payment-providers/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}