package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.MarkAllNotificationsReadResponse
import com.ehealthinformatics.prognocare.data.remote.models.MarkNotificationReadResponse
import com.ehealthinformatics.prognocare.data.remote.models.NotificationItem
import com.ehealthinformatics.prognocare.data.remote.models.NotificationSubscription
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.SubscribeNotificationsDto
import com.ehealthinformatics.prognocare.data.remote.models.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * EMR in-app notifications (phase 1). Poll cursor = latest `createdAt`.
 */
interface NotificationApi {

    @GET("notifications")
    suspend fun list(
        @Query("since") since: String? = null,
        @Query("limit") limit: Int = 50,
    ): Response<PaginatedResponse<NotificationItem>>

    @GET("notifications/unread-count")
    suspend fun unreadCount(): Response<UnreadCountResponse>

    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<MarkNotificationReadResponse>

    @PATCH("notifications/read-all")
    suspend fun markAllRead(): Response<MarkAllNotificationsReadResponse>

    @POST("notification-subscriptions")
    suspend fun subscribe(
        @Body dto: SubscribeNotificationsDto,
    ): Response<NotificationSubscription>
}
