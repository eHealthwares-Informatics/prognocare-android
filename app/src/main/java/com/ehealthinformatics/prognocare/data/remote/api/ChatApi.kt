package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.ConversationInboxResponse
import com.ehealthinformatics.prognocare.data.remote.models.ExchangeMessagesResponse
import com.ehealthinformatics.prognocare.data.remote.models.SendWebhookDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @GET("conversations/inbox")
    suspend fun inbox(
        @Query("limit") limit: Int = 30,
        @Query("activeOnly") activeOnly: Boolean = true,
        @Query("cursor") cursor: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("mode") mode: String? = null,
    ): Response<ConversationInboxResponse>

    @GET("exchanges")
    suspend fun exchanges(
        @Query("conversationId") conversationId: String,
        @Query("limit") limit: Int = 30,
        @Query("cursor") cursor: String? = null,
    ): Response<ExchangeMessagesResponse>

    @POST("webhooks/web")
    suspend fun sendWebhook(@Body dto: SendWebhookDto): Response<Unit>

    @POST("conversations/{conversationId}/read")
    suspend fun markRead(
        @Path("conversationId") conversationId: String,
        @Query("participantId") participantId: String? = null,
    ): Response<Unit>
}