package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.api.AuthApi
import com.ehealthinformatics.prognocare.data.remote.models.LoginDto
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

class AuthApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = buildAuthApi(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildAuthApi(baseUrl: String): AuthApi {
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
        return retrofit.create(AuthApi::class.java)
    }

    @Test
    fun `login posts credentials and parses tokens`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "accessToken": "at-123",
                  "refreshToken": "rt-456",
                  "accessTokenExpiresIn": 900,
                  "refreshTokenExpiresIn": 604800
                }
                """.trimIndent(),
            ),
        )

        val response = api.login(LoginDto(username = "admin", password = "secret1"))
        val body = response.body()!!

        assertTrue(response.isSuccessful)
        assertEquals("at-123", body.accessToken)
        assertEquals("rt-456", body.refreshToken)
        assertEquals(900, body.accessTokenExpiresIn)

        val request = server.takeRequest()
        assertEquals("/auth/login", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.body.readUtf8().contains("\"admin\""))
    }

    @Test
    fun `me parses roles and modules`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "u1",
                  "username": "admin",
                  "roles": ["super_admin"],
                  "permissions": ["dashboard.view"],
                  "modules": [
                    { "id": "emr", "name": "EMR", "description": "Medical record", "root": "/emr" },
                    { "id": "conversation", "name": "Conversation", "description": "Chat", "root": "/conversations" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val body = api.me().body()!!

        assertEquals("u1", body.id)
        assertEquals(listOf("super_admin"), body.roles)
        assertEquals(2, body.modules.size)
        assertEquals("emr", body.modules[0].id)
        assertEquals("/emr", body.modules[0].root)
    }

    @Test
    fun `login failure surfaces the error status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val response = api.login(LoginDto(username = "admin", password = "wrong1"))

        assertTrue(!response.isSuccessful)
        assertEquals(401, response.code())
    }
}