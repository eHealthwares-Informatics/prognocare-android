package com.ehealthinformatics.prognocare.feature.chat

import android.content.Context
import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.remote.AuthInterceptor
import com.ehealthinformatics.prognocare.data.remote.ChatClient
import com.ehealthinformatics.prognocare.data.remote.models.ConversationInboxItem
import com.ehealthinformatics.prognocare.data.remote.models.ExchangeMessage
import com.ehealthinformatics.prognocare.data.remote.models.SendWebhookDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** An incoming realtime message (inbound to this device) surfaced as a notification. */
data class IncomingMessageNotification(
    val conversationId: String,
    val senderId: String?,
    val text: String,
    val createdAt: String?,
)

/** Emitted when the conversation engine ends a conversation (e.g. questionnaire completed). */
data class ConversationEnded(
    val conversationId: String,
    val status: String?,
)

/**
 * Data layer for the Conversation Engine: inbox + messages REST calls, outbound
 * send via /webhooks/web, and realtime message/inbox updates via the socket.
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatClient: ChatClient,
    private val configStore: AppConfigStore,
    private val socket: ChatSocket,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _inbox = MutableStateFlow<List<ConversationInboxItem>>(emptyList())
    val inbox: StateFlow<List<ConversationInboxItem>> = _inbox.asStateFlow()

    private val _messagesByConversation = MutableStateFlow<Map<String, List<ExchangeMessage>>>(emptyMap())
    val messagesByConversation: StateFlow<Map<String, List<ExchangeMessage>>> = _messagesByConversation.asStateFlow()

    private val _inboxError = MutableStateFlow<String?>(null)
    val inboxError: StateFlow<String?> = _inboxError.asStateFlow()

    private val _newMessage = MutableSharedFlow<IncomingMessageNotification>(extraBufferCapacity = 8)
    val newMessage: SharedFlow<IncomingMessageNotification> = _newMessage.asSharedFlow()

    private val _conversationEnded = MutableSharedFlow<ConversationEnded>(extraBufferCapacity = 8)
    val conversationEnded: SharedFlow<ConversationEnded> = _conversationEnded.asSharedFlow()

    /** Every realtime message event, regardless of direction — screens use it to follow a new conversation. */
    private val _messageArrived = MutableSharedFlow<ExchangeMessage>(extraBufferCapacity = 16)
    val messageArrived: SharedFlow<ExchangeMessage> = _messageArrived.asSharedFlow()

    val chatApi: StateFlow<com.ehealthinformatics.prognocare.data.remote.api.ChatApi> = chatClient.chatApi
    val chatSocket: ChatSocket = socket

    init {
        socket.connect(
            onMessage = { message -> handleSocketMessage(message) },
            onInboxUpdated = {
                scope.launch { refreshInbox(loadOnFailure = true) }
            },
            onConversationEnded = { raw -> handleConversationEnded(raw) },
        )
    }

    suspend fun refreshInbox(loadOnFailure: Boolean = false) {
        val api = chatClient.chatApi.value
        runCatching { api.inbox(limit = 30, activeOnly = true) }
            .onSuccess { response ->
                if (response.isSuccessful && response.body() != null) {
                    _inbox.value = response.body()!!.items
                    _inboxError.value = null
                } else if (loadOnFailure) {
                    _inboxError.value = "Inbox fetch failed (${response.code()})"
                }
            }
            .onFailure { e ->
                if (loadOnFailure) _inboxError.value = e.message ?: "Inbox fetch failed"
            }
    }

    suspend fun loadMessages(conversationId: String) {
        val api = chatClient.chatApi.value
        runCatching { api.exchanges(conversationId = conversationId, limit = 30) }
            .onSuccess { response ->
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _messagesByConversation.value = buildMap {
                        putAll(_messagesByConversation.value)
                        put(conversationId, body.items.reversed())
                    }
                }
            }
    }

    /** The stable channel code PrognoCare addresses the messaging bot with. */
    private val messagingChannelCode = "PROGNOCARE_MESSAGING"

    suspend fun sendText(conversationId: String?, senderPhone: String, text: String, newConversation: Boolean = false) {
        val api = chatClient.chatApi.value
        api.sendWebhook(
            SendWebhookDto(
                channelCode = messagingChannelCode,
                senderPhone = senderPhone,
                text = text,
                conversationId = conversationId,
                newConversation = newConversation,
            )
        )
    }

    suspend fun markRead(conversationId: String) {
        val api = chatClient.chatApi.value
        runCatching { api.markRead(conversationId) }
    }

    fun senderPhone(): String = ChatIdentity.phone(context)

    private fun handleSocketMessage(raw: JSONObject) {
        scope.launch {
            val convId = raw.optString("conversationId")
            val text = raw.optString("text")
            if (convId.isNotEmpty()) {
                // A pending-<participantId> placeholder means the engine is
                // still minting the real conversation for this first reply;
                // keep it under its own bucket so the screen can follow it.
                val bucket = convId.ifEmpty { "pending" }
                val message = ExchangeMessage(
                    id = raw.optString("id"),
                    conversationId = convId,
                    senderId = raw.optString("senderId"),
                    receiverId = raw.optString("receiverId").ifEmpty { null },
                    direction = raw.optString("direction", "outbound"),
                    text = text,
                    createdAt = raw.optString("createdAt"),
                    status = raw.optString("status").ifEmpty { null },
                )
                val existing = _messagesByConversation.value[bucket].orEmpty()
                val hasSame = existing.any {
                    it.id.isNotEmpty() && it.id == message.id ||
                        it.text == text && it.direction == message.direction
                }
                if (!hasSame) {
                    _messagesByConversation.value = buildMap {
                        putAll(_messagesByConversation.value)
                        put(bucket, existing + message)
                    }
                }
                _messageArrived.tryEmit(message)

                // Bot/webhook replies carry the participant's participant id
                // (or a channel pseudo-participant id), not our phone — treat
                // empty-direction-orphan sends as inbound for the notify path.
                val selfSent = message.senderId == senderPhone()
                if (message.direction.equals("inbound", ignoreCase = true) && !selfSent) {
                    _newMessage.tryEmit(
                        IncomingMessageNotification(
                            conversationId = convId,
                            senderId = message.senderId,
                            text = text,
                            createdAt = message.createdAt,
                        ),
                    )
                }

                refreshInbox(loadOnFailure = true)
            }
        }
    }

    /**
     * The engine ended a conversation: drop its cached transcript so a stale
     * thread is never shown, notify open screens to reset, and refresh the
     * inbox (the next inbound message starts a brand-new conversation).
     */
    private fun handleConversationEnded(raw: JSONObject) {
        scope.launch {
            val convId = raw.optString("conversationId")
            if (convId.isEmpty()) return@launch
            if (_messagesByConversation.value.containsKey(convId)) {
                _messagesByConversation.value = buildMap {
                    putAll(_messagesByConversation.value)
                    remove(convId)
                }
            }
            _conversationEnded.tryEmit(
                ConversationEnded(
                    conversationId = convId,
                    status = raw.optString("status").ifEmpty { null },
                ),
            )
            refreshInbox(loadOnFailure = true)
        }
    }
}