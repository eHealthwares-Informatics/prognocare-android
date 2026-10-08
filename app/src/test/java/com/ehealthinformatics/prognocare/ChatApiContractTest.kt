package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.ChatApi
import com.ehealthinformatics.prognocare.data.remote.models.SendWebhookDto
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

class ChatApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ChatApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = buildChatApi(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildChatApi(baseUrl: String): ChatApi {
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
        return retrofit.create(ChatApi::class.java)
    }

    @Test
    fun `inbox parses items and next cursor`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "conversationId": "conv-1",
                      "channelId": "ch-1",
                      "status": "ACTIVE",
                      "state": "ACTIVE",
                      "participant": { "id": "p-1", "firstName": "Ada", "lastName": "Obi", "phone": "+2341" },
                      "lastMessage": { "text": "Hello", "direction": "outbound", "createdAt": "2026-01-05T09:00:00Z" },
                      "unreadCount": 2,
                      "projection": { "id": "pr-1", "isPrimary": true, "active": true, "priority": 1 }
                    }
                  ],
                  "nextCursor": "cursor-42"
                }
                """.trimIndent(),
            ),
        )

        val response = api.inbox(limit = 30, activeOnly = true)
        val body = response.body()!!

        assertEquals(1, body.items.size)
        assertEquals("conv-1", body.items[0].conversationId)
        assertEquals("Ada Obi", body.items[0].participantName)
        assertEquals("AO", body.items[0].participantInitials)
        assertEquals("Hello", body.items[0].lastMessage!!.text)
        assertEquals("cursor-42", body.nextCursor)
    }

    @Test
    fun `exchanges parse message items`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "id": "x-1",
                      "conversationId": "conv-1",
                      "senderId": "p-1",
                      "direction": "outbound",
                      "text": "What is your pain level?",
                      "createdAt": "2026-01-05T09:00:00Z",
                      "status": "sent"
                    }
                  ],
                  "nextCursor": "cursor-9"
                }
                """.trimIndent(),
            ),
        )

        val body = api.exchanges(conversationId = "conv-1", limit = 30).body()!!

        assertEquals(1, body.items.size)
        assertEquals("x-1", body.items[0].id)
        assertEquals("What is your pain level?", body.items[0].text)
        assertEquals("cursor-9", body.nextCursor)
    }

    @Test
    fun `send webhook posts to the webhooks endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("{}"))

        val response = api.sendWebhook(
            SendWebhookDto(
                channelCode = "PROGNOCARE_MESSAGING",
                senderPhone = "+2348000000001",
                text = "1: Yes",
                conversationId = "conv-1",
            ),
        )

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("/webhooks/web", request.path)
        assertEquals("POST", request.method)
        val body = request.body.readUtf8()
        assertTrue(body.contains("PROGNOCARE_MESSAGING"))
        assertTrue(body.contains("+2348000000001"))
    }

    @Test
    fun `mark read hits the conversations read endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204).setBody(""))

        val response = api.markRead(conversationId = "conv-1")

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("/conversations/conv-1/read", request.path)
        assertEquals("POST", request.method)
    }
}