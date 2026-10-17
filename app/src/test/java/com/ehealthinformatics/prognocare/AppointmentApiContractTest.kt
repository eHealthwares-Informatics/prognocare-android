package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.AppointmentApi
import com.ehealthinformatics.prognocare.data.remote.models.CancelAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CheckInAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
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

class AppointmentApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AppointmentApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = buildApi(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildApi(baseUrl: String): AppointmentApi {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(AppointmentApi::class.java)
    }

    @Test
    fun `list sends filters and parses the paginated envelope`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "apt-1",
                      "appointmentNumber": "APT-1",
                      "patientId": "MRN-100",
                      "patientName": "Ada Obi",
                      "appointmentType": "CONSULTATION",
                      "date": "2026-09-22",
                      "startTime": "09:00",
                      "providerName": "Dr. Adebayo",
                      "priority": "URGENT",
                      "status": "SCHEDULED",
                      "createdAt": "2026-09-20T10:15:00.000Z"
                    }
                  ],
                  "meta": { "page": 1, "limit": 50, "total": 1, "totalPages": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(
            page = 1,
            limit = 50,
            date = "2026-09-22",
            status = "SCHEDULED",
            patientId = "MRN-100",
            search = "ada",
            sortBy = "date",
            sortOrder = "desc",
        )
        val body = response.body()!!

        assertTrue(response.isSuccessful)
        assertEquals(1, body.data.size)
        assertEquals("APT-1", body.data[0].appointmentNumber)
        assertEquals("MRN-100", body.data[0].patientId)
        assertEquals("URGENT", body.data[0].priority)
        assertTrue(body.data[0].isUrgent)
        assertEquals(1, body.meta?.total)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        val path = request.path!!
        assertTrue(path.startsWith("/appointments?"))
        assertTrue(path.contains("date=2026-09-22"))
        assertTrue(path.contains("status=SCHEDULED"))
        assertTrue(path.contains("patientId=MRN-100"))
        assertTrue(path.contains("sortBy=date"))
        assertTrue(path.contains("sortOrder=desc"))
    }

    @Test
    fun `create posts the appointment dto`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "id": "apt-2", "appointmentNumber": "APT-2", "patientId": "MRN-100",
                  "patientName": "Ada Obi", "appointmentType": "CHECKUP",
                  "date": "2026-09-23", "startTime": "10:30", "status": "SCHEDULED" }
                """.trimIndent(),
            ),
        )

        val response = api.create(
            CreateAppointmentDto(
                patientId = "MRN-100",
                patientName = "Ada Obi",
                appointmentType = "CHECKUP",
                date = "2026-09-23",
                startTime = "10:30",
                reason = "General checkup",
            ),
        )

        assertTrue(response.isSuccessful)
        assertEquals("APT-2", response.body()!!.appointmentNumber)

        val request = server.takeRequest()
        assertEquals("/appointments", request.path)
        val bodyText = request.body.readUtf8()
        assertTrue(bodyText.contains("\"patientId\":\"MRN-100\""))
        assertTrue(bodyText.contains("\"appointmentType\":\"CHECKUP\""))
    }

    @Test
    fun `checkIn parses appointment and visit from the transition response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "appointment": { "id": "apt-1", "status": "IN_PROGRESS" },
                  "visit": { "id": "visit-9", "status": "ONGOING" }
                }
                """.trimIndent(),
            ),
        )

        val response = api.checkIn("apt-1", CheckInAppointmentDto())
        val body = response.body()!!

        assertTrue(response.isSuccessful)
        assertEquals("IN_PROGRESS", body.appointment.status)
        assertEquals("visit-9", body.visit?.id)

        val request = server.takeRequest()
        assertEquals("/appointments/apt-1/check-in", request.path)
        assertEquals("POST", request.method)
    }

    @Test
    fun `cancel posts a reason`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{ "id": "apt-1", "status": "CANCELLED", "notes": "Patient unavailable" }""",
            ),
        )

        val response = api.cancel("apt-1", CancelAppointmentDto(reason = "Patient unavailable"))

        assertTrue(response.isSuccessful)
        assertEquals("CANCELLED", response.body()!!.status)

        val request = server.takeRequest()
        assertEquals("/appointments/apt-1/cancel", request.path)
        assertTrue(request.body.readUtf8().contains("Patient unavailable"))
    }

    @Test
    fun `no-show and complete hit their transition endpoints`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{ "id": "apt-1", "status": "NO_SHOW" }"""))
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{ "id": "apt-1", "status": "COMPLETED" }"""))

        val noShow = api.noShow("apt-1").body()!!
        val complete = api.complete("apt-1").body()!!

        assertEquals("NO_SHOW", noShow.status)
        assertEquals("COMPLETED", complete.status)
        assertEquals("/appointments/apt-1/no-show", server.takeRequest().path)
        assertEquals("/appointments/apt-1/complete", server.takeRequest().path)
    }

    @Test
    fun `list failure surfaces the error status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val response = api.list()

        assertTrue(!response.isSuccessful)
        assertEquals(401, response.code())
    }
}
