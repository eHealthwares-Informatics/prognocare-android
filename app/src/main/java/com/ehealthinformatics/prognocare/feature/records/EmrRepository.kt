package com.ehealthinformatics.prognocare.feature.records

import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.AddRequestNoteDto
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.CreateRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.DashboardSummary
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.Location
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.RequestHistoryEntry
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.data.remote.models.SyncRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto
import com.ehealthinformatics.prognocare.data.remote.models.UpdateRequestDto
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin data-access wrappers over the EMR APIs, mirroring
 * [com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository].
 * Every call reads the current [RetrofitClient.ApiBundle] so runtime base-URL
 * changes apply immediately.
 */
@Singleton
class EmrRepository @Inject constructor(
    private val retrofitClient: RetrofitClient,
) {

    // ── Dashboard ────────────────────────────────────────────────

    suspend fun dashboard(date: String? = null): DashboardSummary {
        val response = retrofitClient.apis.first().dashboardApi.summary(date)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    // ── Requests ─────────────────────────────────────────────────

    suspend fun requests(
        patientId: String? = null,
        visitId: String? = null,
        encounterId: String? = null,
        requestType: String? = null,
        status: String? = null,
        limit: Int = 50,
    ): List<ClinicalRequest> {
        val response = retrofitClient.apis.first().requestApi.list(
            patientId = patientId,
            visitId = visitId,
            encounterId = encounterId,
            requestType = requestType,
            status = status,
            limit = limit,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    suspend fun request(id: String): ClinicalRequest {
        val response = retrofitClient.apis.first().requestApi.getById(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun requestHistory(id: String): List<RequestHistoryEntry> {
        val response = retrofitClient.apis.first().requestApi.getHistory(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body().orEmpty()
    }

    suspend fun createRequest(dto: CreateRequestDto): ClinicalRequest {
        val response = retrofitClient.apis.first().requestApi.create(dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun updateRequest(id: String, dto: UpdateRequestDto): ClinicalRequest {
        val response = retrofitClient.apis.first().requestApi.update(id, dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun transitionRequest(id: String, dto: TransitionRequestStatusDto): ClinicalRequest {
        val response = retrofitClient.apis.first().requestApi.transition(id, dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun addRequestNote(id: String, note: String): Unit {
        val response = retrofitClient.apis.first().requestApi.addNote(id, AddRequestNoteDto(note = note))
        if (!response.isSuccessful) throw failure(response)
    }

    suspend fun syncRequest(id: String, force: Boolean = false): ClinicalRequest {
        val response = retrofitClient.apis.first().requestApi.sync(id, SyncRequestDto(force = force))
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    // ── Encounters / visits ──────────────────────────────────────

    suspend fun encounters(
        patientId: String? = null,
        visitId: String? = null,
        limit: Int = 50,
    ): List<Encounter> {
        val response = retrofitClient.apis.first().encounterApi.list(
            patientId = patientId,
            visitId = visitId,
            limit = limit,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    suspend fun encounter(id: String): Encounter {
        val response = retrofitClient.apis.first().encounterApi.getById(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    // ── Staff ────────────────────────────────────────────────────

    suspend fun staff(
        search: String? = null,
        roleType: String? = null,
        isActive: Boolean? = null,
        userId: String? = null,
        limit: Int = 50,
    ): List<Staff> {
        val response = retrofitClient.apis.first().staffApi.list(
            search = search?.takeIf { it.isNotBlank() },
            roleType = roleType,
            isActive = isActive,
            userId = userId,
            limit = limit,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    // ── Locations ────────────────────────────────────────────────

    suspend fun locations(search: String? = null, limit: Int = 50): List<Location> {
        val response = retrofitClient.apis.first().locationApi.list(
            search = search?.takeIf { it.isNotBlank() },
            limit = limit,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    // ── Patients (pass-through helpers) ──────────────────────────

    suspend fun patients(search: String? = null, limit: Int = 50) =
        retrofitClient.apis.first().patientApi.let { api ->
            val response = api.list(page = 1, limit = limit, search = search?.takeIf { it.isNotBlank() })
            if (!response.isSuccessful) throw failure(response)
            response.body() ?: PaginatedResponse(emptyList())
        }

    private fun failure(response: retrofit2.Response<*>): ApiException {
        val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.lineSequence()?.firstOrNull()?.take(180)
        return ApiException(
            response.code(),
            detail ?: "${response.code()} ${response.message()}",
        )
    }
}
