package com.ehealthwares.prognocare.feature.forms

import android.content.Context
import android.net.Uri
import com.ehealthwares.prognocare.data.local.OfflineStore
import com.ehealthwares.prognocare.data.network.ConnectivityObserver
import com.ehealthwares.prognocare.data.remote.RetrofitClient
import com.ehealthwares.prognocare.data.remote.models.CatalogRow
import com.ehealthwares.prognocare.data.remote.models.ConceptRow
import com.ehealthwares.prognocare.data.remote.models.Department
import com.ehealthwares.prognocare.data.remote.models.Patient
import com.ehealthwares.prognocare.data.remote.models.Staff
import com.ehealthwares.prognocare.data.remote.models.UploadedFile
import com.ehealthwares.prognocare.feature.appointments.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lookups backing the dynamic-form pickers (`patient`, `practitioner`,
 * `department`) plus file upload (`file`). Reads the current API bundle on every
 * call so runtime base-URL changes apply immediately.
 */
@Singleton
class FormLookupRepository @Inject constructor(
    private val retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context,
    private val offlineStore: OfflineStore,
    private val connectivity: ConnectivityObserver,
) {

    suspend fun searchPatients(query: String): List<Patient> {
        return try {
            val response = retrofitClient.apis.value.patientApi.list(
                page = 1,
                limit = 20,
                search = query.takeIf { it.isNotBlank() },
            )
            if (!response.isSuccessful) throw failure(response)
            val patients = response.body()?.data.orEmpty()
            offlineStore.cachePatients(patients)
            patients
        } catch (e: Exception) {
            if (!connectivity.online) offlineStore.searchPatients(query, 20) else throw e
        }
    }

    suspend fun patient(id: String): Patient? {
        val response = retrofitClient.apis.value.patientApi.getById(id)
        if (!response.isSuccessful) return null
        return response.body()
    }

    suspend fun searchStaff(query: String): List<Staff> {
        val response = retrofitClient.apis.value.staffApi.list(
            search = query.takeIf { it.isNotBlank() },
            isActive = true,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    suspend fun departments(locationId: String? = null): List<Department> {
        val response = retrofitClient.apis.value.departmentApi.list(
            locationId = locationId?.takeIf { it.isNotBlank() },
            isActive = true,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    /** LOINC/DICOM concept suggestions from the concepts service. */
    suspend fun concepts(concept: String, term: String): List<ConceptRow> {
        val response = retrofitClient.apis.value.conceptsApi.suggest(
            concept = concept,
            term = term.takeIf { it.isNotBlank() },
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    /**
     * Catalogue lookup by UI kind:
     * - `STOCK_ITEM` → rxsoft `/items`
     * - `GENERIC_PRODUCT` / `GENERIC_DRUG` → concepts `/generic-products` / `/generic-drugs`
     */
    suspend fun catalogue(kind: String, search: String): List<CatalogRow> {
        val apis = retrofitClient.apis.value
        val response = when (kind) {
            "GENERIC_PRODUCT" -> apis.conceptsApi.genericProducts(
                search = search.takeIf { it.isNotBlank() },
            )
            "GENERIC_DRUG" -> apis.conceptsApi.genericDrugs(
                search = search.takeIf { it.isNotBlank() },
            )
            else -> apis.itemsApi.list(search = search.takeIf { it.isNotBlank() })
        }
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    /** Uploads a picked file to the EMR (`POST /uploads`) and returns its metadata. */
    suspend fun upload(uri: Uri, name: String? = null, mimeType: String? = null): UploadedFile =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val fileName = name ?: resolver.getFileName(uri) ?: "upload"
            val mime = mimeType ?: resolver.getType(uri) ?: "application/octet-stream"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw ApiException(400, "Could not read the selected file")
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val response = retrofitClient.apis.value.uploadApi.upload(
                MultipartBody.Part.createFormData("file", fileName, body),
            )
            if (!response.isSuccessful) throw failure(response)
            response.body() ?: throw ApiException(response.code(), "Empty upload response")
        }

    private fun android.content.ContentResolver.getFileName(uri: Uri): String? {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return runCatching {
            query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun failure(response: retrofit2.Response<*>): ApiException {
        val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.lineSequence()?.firstOrNull()?.take(180)
        return ApiException(response.code(), detail ?: "${response.code()} ${response.message()}")
    }
}
