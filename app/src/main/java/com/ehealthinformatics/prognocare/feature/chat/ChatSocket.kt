package com.ehealthinformatics.prognocare.feature.chat

import android.content.Context
import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.config.conversationSocketUrl
import com.ehealthinformatics.prognocare.data.remote.AuthInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton Socket.IO client for the Conversation Engine gateway.
 * Connects to `{conversationHost}/conversations`, authenticating with the
 * shared JWT via `auth.token`. Exposes the message and update events.
 */
@Singleton
class ChatSocket @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: AppConfigStore,
) {
    private var socket: Socket? = null

    @Synchronized
    fun connect(
        onMessage: (JSONObject) -> Unit,
        onInboxUpdated: (JSONObject) -> Unit,
        onConversationEnded: (JSONObject) -> Unit,
    ) {
        val existing = socket
        if (existing != null && existing.connected()) {
            existing.off("conversation.message.created")
            existing.off("conversation.updated")
            existing.off("conversation.ended")
            existing.on("conversation.message.created") { args ->
                if (args.isNotEmpty()) onMessage(args[0] as? JSONObject ?: JSONObject())
            }
            existing.on("conversation.updated") { args ->
                if (args.isNotEmpty()) onInboxUpdated(args[0] as? JSONObject ?: JSONObject())
            }
            existing.on("conversation.ended") { args ->
                if (args.isNotEmpty()) onConversationEnded(args[0] as? JSONObject ?: JSONObject())
            }
            return
        }

        val token = AuthInterceptor.getToken(context)
        val socketUrl = configStore.config.value.conversationSocketUrl

        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            auth = java.util.Collections.singletonMap("token", token.orEmpty())
        }

        socket = try {
            IO.socket(URI.create("$socketUrl/conversations"), options)
        } catch (e: Exception) {
            null
        } ?: return

        val s = socket!!
        s.on("conversation.message.created") { args ->
            if (args.isNotEmpty()) onMessage(args[0] as? JSONObject ?: JSONObject())
        }
        s.on("conversation.updated") { args ->
            if (args.isNotEmpty()) onInboxUpdated(args[0] as? JSONObject ?: JSONObject())
        }
        s.on("conversation.ended") { args ->
            if (args.isNotEmpty()) onConversationEnded(args[0] as? JSONObject ?: JSONObject())
        }
        s.connect()
    }

    @Synchronized
    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    /** Open a conversation (join its socket room). */
    @Synchronized
    fun openConversation(conversationId: String) {
        socket?.emit("conversation.opened", mapOf("conversationId" to conversationId))
    }

    @Synchronized
    fun closeConversation(conversationId: String) {
        socket?.emit("conversation.closed", mapOf("conversationId" to conversationId))
    }

    @Synchronized
    fun connected(): Boolean = socket?.connected() == true
}