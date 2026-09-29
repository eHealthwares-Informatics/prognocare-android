package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.ConnectionCheck
import com.ehealthinformatics.prognocare.data.config.ServerConfigVerifier
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerConfigVerifierTest {

    private lateinit var emrServer: MockWebServer
    private lateinit var conversationServer: MockWebServer
    private val verifier = ServerConfigVerifier()

    @Before
    fun setUp() {
        emrServer = MockWebServer()
        conversationServer = MockWebServer()
        emrServer.start()
        conversationServer.start()
    }

    @After
    fun tearDown() {
        emrServer.shutdown()
        conversationServer.shutdown()
    }

    private fun config(): AppConfig {
        val emrUrl = emrServer.url("/").toString().trimEnd('/')
        val convUrl = conversationServer.url("/").toString().trimEnd('/')
        return AppConfig(
            emrBaseUrl = "$emrUrl/",
            conversationBaseUrl = "$convUrl/api",
            webChannelCode = "PROGNOCARE_MESSAGING",
        )
    }

    @Test
    fun `verifier succeeds when both services report ok`() = runBlocking {
        emrServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok","service":"emr"}"""))
        conversationServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok","service":"conversation-engine"}"""))

        val checks = verifier.verify(config())

        assertTrue(checks.all { it is ConnectionCheck.Success })
    }

    @Test
    fun `verifier fails when emr is unreachable`() = runBlocking {
        conversationServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok","service":"conversation-engine"}"""))
        emrServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"status":"error"}"""))

        val checks = verifier.verify(config())

        val emrCheck = checks.first { it.label == "EMR" }
        assertTrue(emrCheck is ConnectionCheck.Failure)
        assertTrue(checks.any { it is ConnectionCheck.Success && it.label == "Conversation engine" })
    }

    @Test
    fun `verifier fails when conversation engine is down`() = runBlocking {
        emrServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok","service":"emr"}"""))
        conversationServer.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))

        val checks = verifier.verify(config())

        val convCheck = checks.first { it.label == "Conversation engine" }
        assertTrue(convCheck is ConnectionCheck.Failure)
    }
}