package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.FormApi
import com.ehealthinformatics.prognocare.data.remote.models.CreateFormSubmissionDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

class FormApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: FormApi

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

    private fun buildApi(baseUrl: String): FormApi {
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
        return retrofit.create(FormApi::class.java)
    }

    @Test
    fun `available forms lists published definitions`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    { "id": "f1", "code": "CLINICAL_NOTE", "name": "Clinical Note", "category": "CLINICAL_NOTE", "version": 1, "isPublished": true }
                  ],
                  "meta": { "total": 1 }
                }
                """.trimIndent(),
            ),
        )

        val body = api.availableForms().body()!!

        assertEquals(1, body.data.size)
        assertEquals("f1", body.data[0].id)
        assertEquals("CLINICAL_NOTE", body.data[0].code)
        assertTrue(body.data[0].isPublished)

        val request = server.takeRequest()
        assertEquals("/forms/available", request.path)
    }

    @Test
    fun `create submission posts patient context and returns submission`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "s1",
                  "submissionNumber": "SUB-0001",
                  "formName": "Clinical Note",
                  "formVersion": 1,
                  "patientId": "p1",
                  "dataJson": { "reason": "Headache" },
                  "status": "SUBMITTED"
                }
                """.trimIndent(),
            ),
        )

        val dataJson = buildJsonObject { put("reason", "Headache") }
        val response = api.createSubmission(
            CreateFormSubmissionDto(
                formDefinitionId = "f1",
                patientId = "p1",
                visitId = null,
                encounterId = "e1",
                dataJson = dataJson,
                status = "SUBMITTED",
            ),
        )

        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals("s1", body.id)
        assertEquals("SUB-0001", body.submissionNumber)
        assertEquals("SUBMITTED", body.status)

        val request = server.takeRequest()
        assertEquals("/form-submissions", request.path)
        assertEquals("POST", request.method)
        val sent = request.body.readUtf8()
        assertTrue(sent.contains("\"formDefinitionId\":\"f1\""))
        assertTrue(sent.contains("\"status\":\"SUBMITTED\""))
    }
}