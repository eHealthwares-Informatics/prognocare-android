package com.ehealthinformatics.prognocare.feature.notifications

import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.MarkAllNotificationsReadResponse
import com.ehealthinformatics.prognocare.data.remote.models.MarkNotificationReadResponse
import com.ehealthinformatics.prognocare.data.remote.models.NotificationItem
import com.ehealthinformatics.prognocare.data.remote.models.NotificationSubscription
import com.ehealthinformatics.prognocare.data.remote.models.SubscribeNotificationsDto
import com.ehealthinformatics.prognocare.data.remote.models.UnreadCountResponse
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Data access for EMR in-app notifications. Reads the current
 * [RetrofitClient.ApiBundle] on every call so runtime base-URL changes are
 * picked up immediately.
 */
@Singleton
class NotificationsRepository @Inject constructor(
    private val retrofitClient: RetrofitClient,
) {

    suspend fun list(since: String? = null, limit: Int = 50): List<NotificationItem> {
        val response = retrofitClient.apis.first().notificationApi.list(
            since = since,
            limit = limit,
        )
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.data.orEmpty()
    }

    suspend fun unreadCount(): Int {
        val response = retrofitClient.apis.first().notificationApi.unreadCount()
        if (!response.isSuccessful) throw failure(response)
        return response.body()?.count ?: 0
    }

    suspend fun markRead(id: String): MarkNotificationReadResponse {
        val response = retrofitClient.apis.first().notificationApi.markRead(id)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    suspend fun markAllRead(): MarkAllNotificationsReadResponse {
        val response = retrofitClient.apis.first().notificationApi.markAllRead()
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiException(response.code(), "Empty response")
    }

    /**
     * Best-effort register/heartbeat of the in-app subscription. Callers that
     * treat 404 as non-fatal should catch around this (Phase 1 mobile never
     * blocks UI on subscription failure).
     */
    suspend fun subscribe(dto: SubscribeNotificationsDto = SubscribeNotificationsDto()): NotificationSubscription {
        val response = retrofitClient.apis.first().notificationApi.subscribe(dto)
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: NotificationSubscription()
    }

    private fun failure(response: retrofit2.Response<*>): ApiException {
        val detail = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.lineSequence()?.firstOrNull()?.take(180)
        return ApiException(
            response.code(),
            detail ?: "${response.code()} ${response.message()}",
        )
    }
}
