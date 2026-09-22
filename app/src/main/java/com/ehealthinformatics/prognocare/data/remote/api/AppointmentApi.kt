package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.data.remote.models.CancelAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CheckInAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CheckInResponse
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.UpdateAppointmentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentApi {

    @GET("api/appointments")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("date") date: String? = null,
        @Query("status") status: String? = null,
        @Query("providerId") providerId: String? = null,
        @Query("patientId") patientId: String? = null,
        @Query("search") search: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
    ): Response<PaginatedResponse<Appointment>>

    @POST("api/appointments")
    suspend fun create(@Body dto: CreateAppointmentDto): Response<Appointment>

    @GET("api/appointments/{id}")
    suspend fun getById(@Path("id") id: String): Response<Appointment>

    @PATCH("api/appointments/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateAppointmentDto,
    ): Response<Appointment>

    @DELETE("api/appointments/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("api/appointments/{id}/check-in")
    suspend fun checkIn(
        @Path("id") id: String,
        @Body dto: CheckInAppointmentDto = CheckInAppointmentDto(),
    ): Response<CheckInResponse>

    @POST("api/appointments/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String,
        @Body dto: CancelAppointmentDto,
    ): Response<Appointment>

    @POST("api/appointments/{id}/no-show")
    suspend fun noShow(@Path("id") id: String): Response<Appointment>

    @POST("api/appointments/{id}/complete")
    suspend fun complete(@Path("id") id: String): Response<Appointment>
}
