package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Participant(
    val id: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val metadata: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class ConversationProjection(
    val id: String = "",
    val conversationId: String = "",
    val participant: Participant? = null,
    val channelId: String = "",
    val role: String = "USER",
    val type: String = "",
    val isPrimary: Boolean = false,
    val active: Boolean = true,
    val lastMessageAt: String? = null,
)

@Serializable
data class ConversationInboxItem(
    val conversationId: String = "",
    val channelId: String = "",
    val status: String = "ACTIVE",
    val state: String = "ACTIVE",
    val participant: Participant = Participant(),
    val moderator: Participant? = null,
    val lastMessage: LastMessage? = null,
    val unreadCount: Int = 0,
    val lastMessageAt: String? = null,
    val currentQuestion: CurrentQuestion? = null,
    val projection: Projection = Projection(),
) {
    val participantName: String
        get() = listOfNotNull(participant.firstName, participant.lastName)
            .joinToString(" ")
            .ifEmpty { participant.phone ?: "Unknown" }

    val participantInitials: String
        get() {
            val f = participant.firstName?.firstOrNull()?.toString().orEmpty()
            val l = participant.lastName?.firstOrNull()?.toString().orEmpty()
            return (f + l).uppercase().ifEmpty { participant.phone?.take(2).orEmpty().uppercase() }
        }
}

@Serializable
data class LastMessage(
    val id: String? = null,
    val text: String = "",
    val direction: String = "outbound",
    val createdAt: String? = null,
    val questionAttribute: String? = null,
)

@Serializable
data class CurrentQuestion(
    val id: String? = null,
    val attribute: String? = null,
    val text: String? = null,
)

@Serializable
data class Projection(
    val id: String = "",
    val isPrimary: Boolean = false,
    val active: Boolean = true,
    val priority: Int = 0,
    val externalThreadId: String? = null,
)

@Serializable
data class ConversationInboxResponse(
    val items: List<ConversationInboxItem> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class ExchangeMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String? = null,
    val direction: String = "outbound",
    val text: String = "",
    val questionId: String? = null,
    val attribute: String? = null,
    val createdAt: String = "",
    val status: String? = null,
    val optimistic: Boolean = false,
)

@Serializable
data class ExchangeMessagesResponse(
    val items: List<ExchangeMessage> = emptyList(),
    val nextCursor: String? = null,
)

/**
 * Inbound message to the Conversation Engine via POST /webhooks/web. The
 * channel is addressed by its stable code (e.g. PROGNOCARE_MESSAGING) — never
 * a DB id, which is environment-specific and breaks after re-seeding.
 */
@Serializable
data class SendWebhookDto(
    val channelCode: String,
    val senderPhone: String,
    val text: String,
    val conversationId: String? = null,
    val questionnaireCode: String? = null,
    val newConversation: Boolean = false,
)