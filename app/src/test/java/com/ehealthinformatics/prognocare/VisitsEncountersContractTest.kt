package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.location.LocationScope
import com.ehealthinformatics.prognocare.data.remote.api.EncounterApi
import com.ehealthinformatics.prognocare.data.remote.api.VisitApi
import com.ehealthinformatics.prognocare.data.remote.models.CreateEncounterDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateVisitDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Contract tests for the visits and encounters APIs the new clinical screens
 * are built on, plus the shared location-match rule behind the
 * "location based query" scoping.
 */
class VisitsEncountersContractTest {

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
    fun `visit list sends filters and parses items`() = runBlocking {
        val api: VisitApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "vis-1",
                      "patientId": "MRN-1001",
                      "patientName": "Ada Obi",
                      "visitType": "OUTPATIENT",
                      "status": "ONGOING",
                      "providerId": "staff-1",
                      "providerName": "Dr. Ade",
                      "locationId": "loc-1",
                      "startDatetime": "2026-09-24T09:00:00Z"
                    }
                  ],
                  "meta": { "total": 1, "page": 1, "limit": 100, "totalPages": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(limit = 100)
        assertTrue(response.isSuccessful)
        val visit = response.body()!!.data.single()
        assertEquals("vis-1", visit.id)
        assertEquals("OUTPATIENT", visit.visitType)
        assertEquals("ONGOING", visit.status)
        assertTrue(visit.isOngoing)
        assertEquals("loc-1", visit.locationId)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("limit=100"))
    }

    @Test
    fun `visit create sends the payload with locationId`() = runBlocking {
        val api: VisitApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "id": "vis-2", "patientId": "MRN-1001", "visitType": "OUTPATIENT",
                  "status": "ONGOING", "locationId": "loc-2" }
                """.trimIndent(),
            ),
        )

        val response = api.create(
            CreateVisitDto(
                patientId = "MRN-1001",
                patientName = "Ada Obi",
                visitType = "OUTPATIENT",
                locationId = "loc-2",
            ),
        )
        assertTrue(response.isSuccessful)
        assertEquals("vis-2", response.body()!!.id)
        assertEquals("loc-2", response.body()!!.locationId)

        val recorded = server.takeRequest().body.readUtf8()
        assertTrue(recorded.contains("\"visitType\":\"OUTPATIENT\""))
        assertTrue(recorded.contains("\"locationId\":\"loc-2\""))
    }

    @Test
    fun `visit end and cancel post to the action endpoints`() = runBlocking {
        val api: VisitApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "id": "vis-1", "status": "COMPLETED", "stopDatetime": "2026-09-24T10:00:00Z" }""",
            ),
        )
        val ended = api.end("vis-1")
        assertTrue(ended.isSuccessful)
        assertEquals("COMPLETED", ended.body()!!.status)
        assertEquals("/api/visits/vis-1/end", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{ "id": "vis-1", "status": "CANCELLED" }"""),
        )
        val cancelled = api.cancel("vis-1")
        assertTrue(cancelled.isSuccessful)
        assertEquals("CANCELLED", cancelled.body()!!.status)
        assertEquals("/api/visits/vis-1/cancel", server.takeRequest().path)
    }

    @Test
    fun `encounter list sends filters and parses items`() = runBlocking {
        val api: EncounterApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "enc-1",
                      "patientId": "MRN-1001",
                      "visitId": "vis-1",
                      "encounterType": "CONSULTATION",
                      "providerName": "Dr. Ade",
                      "encounterDatetime": "2026-09-24T09:30:00Z",
                      "reason": "Chest pain"
                    }
                  ],
                  "meta": { "total": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(patientId = "MRN-1001", visitId = "vis-1")
        assertTrue(response.isSuccessful)
        val encounter = response.body()!!.data.single()
        assertEquals("enc-1", encounter.id)
        assertEquals("vis-1", encounter.visitId)
        assertEquals("CONSULTATION", encounter.encounterType)
        assertEquals("Chest pain", encounter.reason)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("patientId=MRN-1001"))
        assertTrue(path.contains("visitId=vis-1"))
    }

    @Test
    fun `encounter create sends the payload with visit link`() = runBlocking {
        val api: EncounterApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "id": "enc-2", "patientId": "MRN-1001", "visitId": "vis-2",
                  "encounterType": "CONSULTATION" }
                """.trimIndent(),
            ),
        )

        val response = api.create(
            CreateEncounterDto(
                patientId = "MRN-1001",
                visitId = "vis-2",
                encounterType = "CONSULTATION",
                encounterDatetime = "2026-09-24T09:00:00Z",
                reason = "Follow-up",
            ),
        )
        assertTrue(response.isSuccessful)
        assertEquals("enc-2", response.body()!!.id)
        assertEquals("vis-2", response.body()!!.visitId)

        val recorded = server.takeRequest().body.readUtf8()
        assertTrue(recorded.contains("\"encounterType\":\"CONSULTATION\""))
        assertTrue(recorded.contains("\"visitId\":\"vis-2\""))
        assertTrue(recorded.contains("\"reason\":\"Follow-up\""))
    }

    @Test
    fun `attended patients parses count and forwards providerId`() = runBlocking {
        val api: com.ehealthinformatics.prognocare.data.remote.api.DashboardApi =
            buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{ "count": 42 }"""),
        )

        val response = api.attendedPatients(providerId = "staff-1")
        assertTrue(response.isSuccessful)
        assertEquals(42, response.body()!!.count)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("providerId=staff-1"))
    }

    @Test
    fun `location match rule matches all when scope is null`() {
        assertTrue(LocationScope.matches(scopeId = null, itemLocationId = "loc-1"))
        assertTrue(LocationScope.matches(scopeId = null, itemLocationId = null))
        assertTrue(LocationScope.matches(scopeId = null, itemLocationId = null, fallback = "loc-3"))
    }

    @Test
    fun `location match rule filters by id with fallback`() {
        assertTrue(LocationScope.matches(scopeId = "loc-1", itemLocationId = "loc-1"))
        // scheduleLocation-style fallback carries the id when locationId is absent
        assertTrue(LocationScope.matches(scopeId = "loc-1", itemLocationId = null, fallback = "loc-1"))
        assertFalse(LocationScope.matches(scopeId = "loc-1", itemLocationId = "loc-2"))
        assertFalse(LocationScope.matches(scopeId = "loc-1", itemLocationId = null))
        assertFalse(LocationScope.matches(scopeId = "loc-1", itemLocationId = null, fallback = "loc-2"))
    }
}
