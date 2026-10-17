package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.HealthStatus
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun health(): Response<HealthStatus>
}