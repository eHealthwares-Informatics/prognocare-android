package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.CreateFormDefinitionDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateFormSubmissionDto
import com.ehealthinformatics.prognocare.data.remote.models.FormDefinition
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.PublishFormDto
import com.ehealthinformatics.prognocare.data.remote.models.UpdateFormDefinitionDto
import com.ehealthinformatics.prognocare.data.remote.models.UpdateFormSubmissionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FormApi {

    // ── Form Definitions ──────────────────────────────────────

    @GET("form-definitions")
    suspend fun listDefinitions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<PaginatedResponse<FormDefinition>>

    @POST("form-definitions")
    suspend fun createDefinition(@Body dto: CreateFormDefinitionDto): Response<FormDefinition>

    @GET("form-definitions/{id}")
    suspend fun getDefinitionById(@Path("id") id: String): Response<FormDefinition>

    @PATCH("form-definitions/{id}")
    suspend fun updateDefinition(
        @Path("id") id: String,
        @Body dto: UpdateFormDefinitionDto,
    ): Response<FormDefinition>

    @DELETE("form-definitions/{id}")
    suspend fun deleteDefinition(@Path("id") id: String): Response<Unit>

    @POST("form-definitions/{id}/publish")
    suspend fun publish(
        @Path("id") id: String,
        @Body dto: PublishFormDto = PublishFormDto(),
    ): Response<FormDefinition>

    @POST("form-definitions/{id}/unpublish")
    suspend fun unpublish(@Path("id") id: String): Response<FormDefinition>

    // ── Form Submissions ──────────────────────────────────────

    @GET("form-submissions")
    suspend fun listSubmissions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("patientId") patientId: String? = null,
        @Query("visitId") visitId: String? = null,
        @Query("encounterId") encounterId: String? = null,
        @Query("formDefinitionId") formDefinitionId: String? = null,
    ): Response<PaginatedResponse<FormSubmission>>

    @POST("form-submissions")
    suspend fun createSubmission(@Body dto: CreateFormSubmissionDto): Response<FormSubmission>

    @GET("form-submissions/{id}")
    suspend fun getSubmissionById(@Path("id") id: String): Response<FormSubmission>

    @PATCH("form-submissions/{id}")
    suspend fun updateSubmission(
        @Path("id") id: String,
        @Body dto: UpdateFormSubmissionDto,
    ): Response<FormSubmission>

    @DELETE("form-submissions/{id}")
    suspend fun deleteSubmission(@Path("id") id: String): Response<Unit>

    @GET("form-submissions/{id}/chain")
    suspend fun getAmendChain(@Path("id") id: String): Response<List<FormSubmission>>

    @GET("form-submissions/{id}/pdf")
    suspend fun getPdf(@Path("id") id: String): Response<okhttp3.ResponseBody>

    @POST("form-submissions/{id}/amend")
    suspend fun amend(
        @Path("id") id: String,
        @Body dto: UpdateFormSubmissionDto,
    ): Response<FormSubmission>

    // ── Available Forms ───────────────────────────────────────

    @GET("forms/available")
    suspend fun availableForms(): Response<PaginatedResponse<FormDefinition>>
}
