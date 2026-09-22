package com.ehealthinformatics.prognocare.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.ConversationInboxItem
import com.ehealthinformatics.prognocare.data.remote.models.ExchangeMessage
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    conversation: ConversationInboxItem? = null,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    var messageText by remember { mutableStateOf("") }
    // The conversation being displayed. It starts as the navigated-to id and
    // may switch when the engine ends this conversation and starts a fresh one
    // in response to the user's next message.
    var activeId by remember { mutableStateOf(conversationId) }
    var conversationEnded by remember { mutableStateOf(false) }
    val messagesMap by viewModel.messagesByConversation.collectAsStateWithLifecycle()
    val messages = remember(messagesMap, activeId) {
        messagesMap[activeId].orEmpty()
    }

    LaunchedEffect(activeId) {
        viewModel.loadMessages(activeId)
    }

    // Reset the thread when the engine ends the open conversation, and follow
    // the new conversation the server starts on the user's next message.
    LaunchedEffect(Unit) {
        viewModel.conversationEnded.collect { event ->
            if (event.conversationId == activeId) {
                conversationEnded = true
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messageArrived.collect { event ->
            if (conversationEnded && event.conversationId != activeId) {
                activeId = event.conversationId
                conversationEnded = false
            }
        }
    }

    DisposableEffect(activeId) {
        onDispose {
            viewModel.onClearedConversation(activeId)
        }
    }

    val participantName = conversation?.participantName ?: "Conversation"
    val participantInitials = conversation?.participantInitials ?: conversationId.take(2).uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = participantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = if (conversationEnded) "ENDED" else conversation?.status ?: "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* patient context */ }) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = "Patient Info",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = Spacing.base,
                    vertical = Spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(messages, key = { it.id.ifEmpty { "${it.createdAt}-${it.text}" } }) { message ->
                    ChatBubble(
                        message = message,
                        onOptionSelect = { value ->
                            viewModel.sendMessage(activeId, value)
                        },
                    )
                }
            }

            if (conversationEnded) {
                Text(
                    text = "Conversation ended — send a message to start a new one.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = Spacing.base, vertical = Spacing.sm),
                )
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.base, vertical = Spacing.sm)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(onClick = { /* attach file */ }) {
                    Icon(
                        Icons.Default.Attachment,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    shape = RoundedCornerShape(Spacing.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    maxLines = 4,
                )

                Spacer(modifier = Modifier.width(Spacing.sm))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(activeId, messageText.trim())
                            messageText = ""
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ExchangeMessage,
    onOptionSelect: (String) -> Unit,
) {
    val isMe = message.direction == "inbound"
    val parsed = if (!isMe) ChatOptionParser.parse(message.text) else null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.width(320.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        ) {
            if (parsed != null) {
                OptionCard(parsed = parsed, onOptionSelect = onOptionSelect)
            } else {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isMe) 16.dp else 4.dp,
                                topEnd = if (isMe) 4.dp else 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            )
                        )
                        .background(
                            if (isMe) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .padding(Spacing.md),
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = message.createdAt.toDisplayTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xxs),
            )
        }
    }
}

@Composable
private fun OptionCard(
    parsed: ParsedQuestionOptions,
    onOptionSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(Spacing.md),
            )
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = parsed.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        parsed.options.forEach { option ->
            ElevatedButton(
                onClick = { onOptionSelect(option.value) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.md),
            ) {
                Text(option.label)
            }
        }
    }
}

private fun String.toDisplayTime(): String {
    return try {
        val parsed = java.time.OffsetDateTime.parse(this)
        val local = parsed.toLocalTime()
        String.format("%02d:%02d", local.hour, local.minute)
    } catch (e: Exception) {
        this
    }
}