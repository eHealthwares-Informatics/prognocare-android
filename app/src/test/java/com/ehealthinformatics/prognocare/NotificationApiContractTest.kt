package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.NotificationApi
import com.ehealthinformatics.prognocare.data.remote.models.NotificationItem
import com.ehealthinformatics.prognocare.data.remote.models.SubscribeNotificationsDto
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
 * Contract tests for EMR in-app notifications (android#6).
 * Matches the RequestsApiContractTest MockWebServer pattern.
 */
class NotificationApiContractTest {

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
            explicitNulls = false
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
    fun `notifications list sends since and limit and parses items`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "notif-1",
                      "title": "New request REQ-0001 created",
                      "body": "New request REQ-0001 created",
                      "type": "info",
                      "sourceEntityType": "request",
                      "sourceEntityId": "req-1",
                      "sourceEntityRef": "REQ-0001",
                      "read": false,
                      "readAt": null,
                      "createdAt": "2026-10-06T08:00:00.000Z"
                    }
                  ],
                  "meta": { "page": 1, "limit": 50, "total": 1 }
                }
                """.trimIndent(),
            ),
        )

        val response = api.list(since = "2026-10-06T08:58:26.069Z", limit = 50)
        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals(1, body.data.size)
        val item = body.data.first()
        assertEquals("notif-1", item.id)
        assertEquals("request", item.sourceEntityType)
        assertEquals("req-1", item.sourceEntityId)
        assertEquals("REQ-0001", item.sourceEntityRef)
        assertEquals(false, item.read)
        assertEquals("2026-10-06T08:00:00.000Z", item.createdAt)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("since=2026-10-06T08%3A58%3A26.069Z") || path.contains("since=2026-10-06T08:58:26.069Z"))
        assertTrue(path.contains("limit=50"))
    }

    @Test
    fun `unread-count parses the count envelope`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{ "count": 3 }"""),
        )

        val response = api.unreadCount()
        assertTrue(response.isSuccessful)
        assertEquals(3, response.body()!!.count)
        assertTrue(server.takeRequest().path.orEmpty().contains("unread-count"))
    }

    @Test
    fun `mark read hits the notification read endpoint`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "id": "notif-1", "read": true, "readAt": "2026-10-06T09:00:00.000Z" }""",
            ),
        )

        val response = api.markRead("notif-1")
        assertTrue(response.isSuccessful)
        assertEquals("notif-1", response.body()!!.id)
        assertTrue(response.body()!!.read)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.path.orEmpty().contains("/api/notifications/notif-1/read"))
    }

    @Test
    fun `mark all read patches the read-all endpoint`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{ "updated": 2 }"""),
        )

        val response = api.markAllRead()
        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()!!.updated)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertTrue(request.path.orEmpty().contains("/api/notifications/read-all"))
    }

    @Test
    fun `subscribe posts the optional body to notification-subscriptions`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "sub-1",
                  "userId": "user-1",
                  "organizationId": "org-1",
                  "locationId": "loc-1",
                  "lastHeartbeatAt": "2026-10-06T10:00:00.000Z"
                }
                """.trimIndent(),
            ),
        )

        val response = api.subscribe(SubscribeNotificationsDto())
        assertTrue(response.isSuccessful)
        assertEquals("sub-1", response.body()!!.id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path.orEmpty().contains("/api/notification-subscriptions"))
    }

    @Test
    fun `list failure surfaces the error status`() = runBlocking {
        val api: NotificationApi = buildApi(server.url("/").toString())
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"boom"}"""))

        val response = api.list()
        assertTrue(!response.isSuccessful)
        assertEquals(500, response.code())
    }
}
