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
            registerListeners(existing, onMessage, onInboxUpdated, onConversationEnded)
            return
        }

        val token = AuthInterceptor.getToken(context)
        val socketUrl = configStore.config.value.conversationSocketUrl

        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            val authMap = java.util.HashMap<String, String>()
            authMap["token"] = token.orEmpty()
            // The gateway joins per-message delivery rooms keyed by phone.
            // Sending our phone in the handshake joins the room immediately —
            // no race with the first reply (identify would arrive too late).
            ChatIdentity.phone(context)?.let { authMap["phone"] = it }
            auth = authMap
        }

        socket = try {
            IO.socket(URI.create("$socketUrl/conversations"), options)
        } catch (e: Exception) {
            null
        } ?: return

        val s = socket!!
        registerListeners(s, onMessage, onInboxUpdated, onConversationEnded)

        // Belt-and-braces: also identify after connect (covers the case where
        // the phone is learned post-connect, e.g. profile fetch completes).
        s.on(Socket.EVENT_CONNECT) {
            ChatIdentity.phone(context)?.let { phone ->
                s.emit("conversation.identify", mapOf("phone" to phone))
            }
        }
        s.connect()
    }

    private fun registerListeners(
        s: Socket,
        onMessage: (JSONObject) -> Unit,
        onInboxUpdated: (JSONObject) -> Unit,
        onConversationEnded: (JSONObject) -> Unit,
    ) {
        s.off("conversation.message.created")
        s.off("conversation.message.orphan")
        s.off("conversation.updated")
        s.off("conversation.ended")
        s.on("conversation.message.created") { args ->
            if (args.isNotEmpty()) onMessage(args[0] as? JSONObject ?: JSONObject())
        }
        // Orphan events carry the first bot reply while the conversation is
        // still a pending placeholder; treat them as ordinary messages.
        s.on("conversation.message.orphan") { args ->
            if (args.isNotEmpty()) onMessage(args[0] as? JSONObject ?: JSONObject())
        }
        s.on("conversation.updated") { args ->
            if (args.isNotEmpty()) onInboxUpdated(args[0] as? JSONObject ?: JSONObject())
        }
        s.on("conversation.ended") { args ->
            if (args.isNotEmpty()) onConversationEnded(args[0] as? JSONObject ?: JSONObject())
        }
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

    /**
     * Rebind this socket to a (possibly new) phone-based participant without a
     * reconnect — mirrors the storefront's `conversation.identify` usage.
     */
    @Synchronized
    fun identify(phone: String) {
        if (phone.isNotBlank()) {
            socket?.emit("conversation.identify", mapOf("phone" to phone))
        }
    }

    @Synchronized
    fun connected(): Boolean = socket?.connected() == true
}
