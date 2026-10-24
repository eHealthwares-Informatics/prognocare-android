package com.ehealthwares.prognocare.feature.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Exposes realtime incoming-message notifications from the [ChatRepository] to the
 * UI layer, so a shell (scaffold) can show a popup and navigate to the conversation.
 */
@HiltViewModel
class ChatNotificationViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    val newMessage: SharedFlow<IncomingMessageNotification> = repository.newMessage
}