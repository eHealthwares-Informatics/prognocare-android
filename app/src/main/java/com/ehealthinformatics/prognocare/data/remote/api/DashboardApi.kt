package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.AttendedPatientsResponse
import com.ehealthinformatics.prognocare.data.remote.models.DashboardSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {

    @GET("dashboard")
    suspend fun summary(
        @Query("date") date: String? = null,
    ): Response<DashboardSummary>

    @GET("dashboard/attended-patients")
    suspend fun attendedPatients(
        @Query("providerId") providerId: String? = null,
    ): Response<AttendedPatientsResponse>
}
