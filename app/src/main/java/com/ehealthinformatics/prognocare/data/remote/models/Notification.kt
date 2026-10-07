package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

/**
 * In-app notification for the current user (EMR `/api/notifications`).
 * `sourceEntityType == "request"` means the item can be opened at request detail.
 */
@Serializable
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String? = null,
    val type: String = "info",
    val sourceEntityType: String? = null,
    val sourceEntityId: String? = null,
    val sourceEntityRef: String? = null,
    val read: Boolean = false,
    val readAt: String? = null,
    val createdAt: String? = null,
)

/**
 * POST /api/notification-subscriptions — org/location come from the JWT tenant.
 * Body is optional; sending `{}` heartbeats the active subscription.
 */
@Serializable
data class SubscribeNotificationsDto(
    val expiresAt: String? = null,
)

@Serializable
data class NotificationSubscription(
    val id: String? = null,
    val userId: String? = null,
    val organizationId: String? = null,
    val locationId: String? = null,
    val expiresAt: String? = null,
    val lastHeartbeatAt: String? = null,
)

@Serializable
data class UnreadCountResponse(
    val count: Int = 0,
)

@Serializable
data class MarkNotificationReadResponse(
    val id: String = "",
    val read: Boolean = false,
    val readAt: String? = null,
)

@Serializable
data class MarkAllNotificationsReadResponse(
    val updated: Int = 0,
)
