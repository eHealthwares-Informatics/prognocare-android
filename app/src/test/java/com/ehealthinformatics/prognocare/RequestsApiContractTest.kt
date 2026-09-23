package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.DashboardApi
import com.ehealthinformatics.prognocare.data.remote.api.LocationApi
import com.ehealthinformatics.prognocare.data.remote.api.RequestApi
import com.ehealthinformatics.prognocare.data.remote.api.StaffApi
import com.ehealthinformatics.prognocare.data.remote.models.AddRequestNoteDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.RequestItem
import com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Contract tests for the API surfaces added for the role features:
 * requests (list/transition/note), dashboard summary, staff with the
 * self-scoping userId filter, and locations.
 */
class RequestsApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var json: Json

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private inline fun <reified T> buildApi(baseUrl: String): T {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(T::class.java)
    }

    @Test
    fun `requests list sends query params and parses items`() = runBlocking {
        val api: RequestApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "req-1",
                      "requestNumber": "REQ-0001",
                      "patientId": "pat-1",
                      "patientName": "Ada Obi",
                      "requestType": "LAB",
                      "priority": "URGENT",
                      "status": "REQUESTED",
                      "orderingProviderId": "staff-1",
                      "orderingProviderName": "Dr. Ade",
                      "diagnosis": "Anemia workup",
                      "syncStatus": "PENDING",
                      "items": [
                        { "id": "it-1", "name": "CBC", "code": "LAB-CBC", "notes": null }
                      ]
                    }
                  ],
                  "meta": { "total": 1, "page": 1, "limit": 50, "totalPages": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(patientId = "pat-1", requestType = "LAB", status = "REQUESTED")
        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals(1, body.data.size)
        val request = body.data.first()
        assertEquals("req-1", request.id)
        assertEquals("LAB", request.requestType)
        assertEquals("REQUESTED", request.status)
        assertEquals(1, request.items.size)
        assertEquals("CBC", request.items.first().name)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("patientId=pat-1"))
        assertTrue(path.contains("requestType=LAB"))
        assertTrue(path.contains("status=REQUESTED"))
    }

    @Test
    fun `request create sends the full payload`() = runBlocking {
        val api: RequestApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "req-2",
                  "patientId": "pat-1",
                  "requestType": "PRESCRIPTION",
                  "status": "REQUESTED",
                  "items": [ { "id": "it-2", "name": "Paracetamol", "dose": "500", "doseUnit": "mg" } ]
                }
                """.trimIndent(),
            ),
        )

        val response = api.create(
            CreateRequestDto(
                patientId = "pat-1",
                patientName = "Ada Obi",
                requestType = "PRESCRIPTION",
                priority = "ROUTINE",
                items = listOf(
                    RequestItem(name = "Paracetamol", dose = "500", doseUnit = "mg"),
                ),
            ),
        )
        assertTrue(response.isSuccessful)
        assertEquals("req-2", response.body()!!.id)
        assertEquals("PRESCRIPTION", response.body()!!.requestType)

        val recorded = server.takeRequest().body.readUtf8()
        assertTrue(recorded.contains("\"requestType\":\"PRESCRIPTION\""))
        assertTrue(recorded.contains("\"dose\":\"500\""))
    }

    @Test
    fun `transition posts status and reason`() = runBlocking {
        val api: RequestApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "id": "req-1", "requestType": "LAB", "status": "IN_PROGRESS" }""",
            ),
        )
        val response = api.transition("req-1", TransitionRequestStatusDto(status = "IN_PROGRESS"))
        assertTrue(response.isSuccessful)
        assertEquals("IN_PROGRESS", response.body()!!.status)

        val recorded = server.takeRequest().body.readUtf8()
        assertTrue(recorded.contains("\"status\":\"IN_PROGRESS\""))
    }

    @Test
    fun `note posts the note body`() = runBlocking {
        val api: RequestApi = buildApi(server.url("/").toString())
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        val response = api.addNote("req-1", AddRequestNoteDto(note = "Sample collected"))
        assertTrue(response.isSuccessful)
        assertTrue(server.takeRequest().body.readUtf8().contains("Sample collected"))
    }

    @Test
    fun `dashboard parses metrics and provider load`() = runBlocking {
        val api: DashboardApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "date": "2026-09-23",
                  "metrics": {
                    "totalAppointments": 6,
                    "scheduled": 3,
                    "checkedIn": 1,
                    "inProgress": 1,
                    "completed": 1,
                    "cancelled": 0,
                    "noShow": 0,
                    "providersOnDuty": 2,
                    "averageWaitMinutes": 12,
                    "totalPatients": 42,
                    "activeVisits": 1,
                    "pendingRequests": 7
                  },
                  "appointments": [],
                  "providerLoad": [
                    { "providerId": "staff-1", "providerName": "Dr. Ade", "patientCount": 1, "activeCount": 1 }
                  ],
                  "upcoming": []
                }
                """.trimIndent(),
            ),
        )

        val response = api.summary("2026-09-23")
        assertTrue(response.isSuccessful)
        val summary = response.body()!!
        assertEquals("2026-09-23", summary.date)
        assertEquals(6, summary.metrics.totalAppointments)
        assertEquals(42, summary.metrics.totalPatients)
        assertEquals(7, summary.metrics.pendingRequests)
        assertEquals(1, summary.providerLoad.size)
    }

    @Test
    fun `staff list forwards the userId self-scoping filter`() = runBlocking {
        val api: StaffApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    { "id": "staff-1", "staffNumber": "STF-1", "firstName": "Ada", "lastName": "Obi",
                      "roleType": "Doctor", "userId": "user-42", "isActive": true }
                  ],
                  "meta": { "total": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(userId = "user-42", limit = 1)
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()!!.data.size)
        assertEquals("user-42", response.body()!!.data.first().userId)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("userId=user-42"))
        assertTrue(path.contains("limit=1"))
    }

    @Test
    fun `locations list parses the envelope`() = runBlocking {
        val api: LocationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    { "id": "loc-1", "code": "MAIN", "name": "Main Clinic", "isActive": true }
                  ],
                  "meta": { "total": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(limit = 20)
        assertTrue(response.isSuccessful)
        val location = response.body()!!.data.single()
        assertEquals("Main Clinic", location.name)
        assertTrue(location.isActive)
    }
}
