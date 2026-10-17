package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.AddRequestNoteDto
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.CreateRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.RequestHistoryEntry
import com.ehealthinformatics.prognocare.data.remote.models.SyncRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto
import com.ehealthinformatics.prognocare.data.remote.models.UpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RequestApi {

    @GET("requests")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("patientId") patientId: String? = null,
        @Query("visitId") visitId: String? = null,
        @Query("encounterId") encounterId: String? = null,
        @Query("requestType") requestType: String? = null,
        @Query("status") status: String? = null,
    ): Response<PaginatedResponse<ClinicalRequest>>

    @POST("requests")
    suspend fun create(@Body dto: CreateRequestDto): Response<ClinicalRequest>

    @GET("requests/{id}")
    suspend fun getById(@Path("id") id: String): Response<ClinicalRequest>

    @PATCH("requests/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateRequestDto,
    ): Response<ClinicalRequest>

    @DELETE("requests/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @GET("requests/{id}/history")
    suspend fun getHistory(@Path("id") id: String): Response<List<RequestHistoryEntry>>

    @POST("requests/{id}/transition")
    suspend fun transition(
        @Path("id") id: String,
        @Body dto: TransitionRequestStatusDto,
    ): Response<ClinicalRequest>

    @POST("requests/{id}/note")
    suspend fun addNote(
        @Path("id") id: String,
        @Body dto: AddRequestNoteDto,
    ): Response<Unit>

    @POST("requests/{id}/sync")
    suspend fun sync(
        @Path("id") id: String,
        @Body dto: SyncRequestDto,
    ): Response<ClinicalRequest>
}
