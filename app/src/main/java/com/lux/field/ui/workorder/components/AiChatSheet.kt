package com.lux.field.ui.workorder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lux.field.R
import com.lux.field.domain.model.ChatMessage
import com.lux.field.domain.voice.PlaybackState
import com.lux.field.domain.voice.SpeechState

data class TaskChatContext(
    val taskLabel: String,
    val taskType: String,
    val currentStep: String?,
    val progress: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    taskContext: TaskChatContext?,
    speechState: SpeechState,
    playbackState: PlaybackState,
    autoSpeak: Boolean,
    photoPathMap: Map<String, String>,
    onSendMessage: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakMessage: (ChatMessage) -> Unit,
    onStopPlayback: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onNavigateToCamera: (cameraFacing: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        AiChatContent(
            messages = messages,
            isLoading = isLoading,
            taskContext = taskContext,
            speechState = speechState,
            playbackState = playbackState,
            autoSpeak = autoSpeak,
            photoPathMap = photoPathMap,
            onSendMessage = onSendMessage,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onSpeakMessage = onSpeakMessage,
            onStopPlayback = onStopPlayback,
            onToggleAutoSpeak = onToggleAutoSpeak,
            onNavigateToCamera = onNavigateToCamera,
        )
    }
}

@Composable
private fun AiChatContent(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    taskContext: TaskChatContext?,
    speechState: SpeechState,
    playbackState: PlaybackState,
    autoSpeak: Boolean,
    photoPathMap: Map<String, String>,
    onSendMessage: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakMessage: (ChatMessage) -> Unit,
    onStopPlayback: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onNavigateToCamera: (cameraFacing: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val isPlayingMessageId = when (playbackState) {
        is PlaybackState.Playing -> playbackState.messageId
        else -> null
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-fill text from speech result
    var text by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Result) {
            text = speechState.text
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header
        ChatHeader(
            taskContext = taskContext,
            autoSpeak = autoSpeak,
            onToggleAutoSpeak = onToggleAutoSpeak,
        )

        // Messages or Empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (messages.isEmpty() && !isLoading) {
                // Empty state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = stringResource(R.string.ai_chat_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageBubble(
                            message = message,
                            onSpeakMessage = { msg ->
                                if (isPlayingMessageId == msg.id) {
                                    onStopPlayback()
                                } else {
                                    onSpeakMessage(msg)
                                }
                            },
                            isPlayingMessageId = isPlayingMessageId,
                            photoPathMap = photoPathMap,
                        )
                    }

                    if (isLoading) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                }
            }
        }

        // Voice listening indicator
        if (speechState is SpeechState.Listening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoicePulseIndicator()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.voice_listening),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Input bar with surface background
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                isLoading = isLoading,
                speechState = speechState,
                onSendMessage = { msg ->
                    onSendMessage(msg)
                    text = ""
                },
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onNavigateToCamera = onNavigateToCamera,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    taskContext: TaskChatContext?,
    autoSpeak: Boolean,
    onToggleAutoSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_chat_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (taskContext != null) {
                    Text(
                        text = "${taskContext.taskLabel} · ${taskContext.progress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AssistChip(
                onClick = onToggleAutoSpeak,
                label = {
                    Text(
                        text = stringResource(R.string.voice_auto_speak),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = if (autoSpeak) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Filled.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    speechState: SpeechState,
    onSendMessage: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onNavigateToCamera: (cameraFacing: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isListening = speechState is SpeechState.Listening
    var showCameraMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mic button
        IconButton(
            onClick = {
                if (isListening) onStopListening() else onStartListening()
            },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Close else Icons.Filled.Mic,
                contentDescription = stringResource(R.string.voice_mic),
                tint = if (isListening) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        // Text field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.ai_chat_input_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        // Camera button with dropdown
        IconButton(
            onClick = { showCameraMenu = true },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = stringResource(R.string.chat_camera_menu_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DropdownMenu(
                expanded = showCameraMenu,
                onDismissRequest = { showCameraMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_work_photo)) },
                    onClick = {
                        showCameraMenu = false
                        onNavigateToCamera("back")
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_selfie)) },
                    onClick = {
                        showCameraMenu = false
                        onNavigateToCamera("front")
                    },
                )
            }
        }

        // Send button
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSendMessage(text.trim())
                }
            },
            enabled = !isLoading && text.isNotBlank(),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.ai_chat_send),
            )
        }
    }
}
