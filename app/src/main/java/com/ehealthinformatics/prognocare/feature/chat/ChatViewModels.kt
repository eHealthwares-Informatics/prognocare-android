package com.ehealthinformatics.prognocare.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.models.ConversationInboxItem
import com.ehealthinformatics.prognocare.data.remote.models.ExchangeMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    val inbox: StateFlow<List<ConversationInboxItem>> = repository.inbox
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inboxError: StateFlow<String?> = repository.inboxError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshInbox(loadOnFailure = false)
        }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    val messagesByConversation: StateFlow<Map<String, List<ExchangeMessage>>> =
        repository.messagesByConversation
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Fired when the engine ends the open conversation — the screen resets for a fresh chat. */
    val conversationEnded: SharedFlow<ConversationEnded> = repository.conversationEnded

    /** Every realtime message event — lets the screen follow a newly-started conversation. */
    val messageArrived: SharedFlow<ExchangeMessage> = repository.messageArrived

    fun messagesFor(conversationId: String): List<ExchangeMessage> =
        messagesByConversation.value[conversationId].orEmpty()

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.loadMessages(conversationId)
            repository.markRead(conversationId)
            repository.chatSocket.openConversation(conversationId)
        }
    }

    fun onClearedConversation(conversationId: String) {
        viewModelScope.launch {
            repository.chatSocket.closeConversation(conversationId)
        }
    }

    fun sendMessage(conversationId: String?, text: String) {
        viewModelScope.launch {
            val senderPhone = repository.senderPhone()
            repository.sendText(conversationId, senderPhone, text)
        }
    }
}