package com.lux.field.ui.workorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lux.field.R
import com.lux.field.domain.model.CameraFacing
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskPhoto
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.model.TaskStep
import com.lux.field.ui.components.AnimatedLinearProgress
import com.lux.field.ui.map.components.TaskStatusChip
import com.lux.field.ui.workorder.components.AiChatSheet
import com.lux.field.ui.workorder.components.CameraActionButtons
import com.lux.field.ui.workorder.components.PhotoGalleryStrip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TaskDetailScreen(
    workOrderId: String,
    taskId: String,
    onBack: () -> Unit,
    onNavigateToCamera: (workOrderId: String, taskId: String, cameraFacing: String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.task?.label ?: stringResource(R.string.task_detail_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (uiState.task?.status == TaskStatus.IN_PROGRESS) {
                FloatingActionButton(
                    onClick = viewModel::openChat,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(R.string.ai_chat_title),
                    )
                }
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading && uiState.task == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.tertiary,
            )
        } else if (uiState.task != null) {
            TaskContent(
                task = uiState.task!!,
                photos = uiState.photos,
                onToggleStep = viewModel::toggleStepCompletion,
                onStartTask = viewModel::startTask,
                onCompleteTask = viewModel::completeTask,
                onTakeWorkPhoto = {
                    onNavigateToCamera(workOrderId, taskId, CameraFacing.BACK.name.lowercase())
                },
                onTakeSelfie = {
                    onNavigateToCamera(workOrderId, taskId, CameraFacing.FRONT.name.lowercase())
                },
                workPhotoCount = uiState.photos.count { it.cameraFacing == CameraFacing.BACK },
                selfieCount = uiState.photos.count { it.cameraFacing == CameraFacing.FRONT },
                error = uiState.error,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }

    // AI Chat Bottom Sheet
    if (uiState.isChatOpen) {
        AiChatSheet(
            messages = uiState.chatMessages,
            isLoading = uiState.isChatLoading,
            taskContext = uiState.taskChatContext,
            speechState = speechState,
            playbackState = playbackState,
            autoSpeak = uiState.autoSpeak,
            photoPathMap = uiState.photoPathMap,
            onSendMessage = viewModel::sendChatMessage,
            onStartListening = {
                if (micPermissionState.status.isGranted) {
                    viewModel.startListening()
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            },
            onStopListening = viewModel::stopListening,
            onSpeakMessage = viewModel::speakMessage,
            onStopPlayback = viewModel::stopPlayback,
            onToggleAutoSpeak = viewModel::toggleAutoSpeak,
            onNavigateToCamera = { facing ->
                onNavigateToCamera(workOrderId, taskId, facing)
            },
            onDismiss = viewModel::closeChat,
        )
    }
}

@Composable
private fun TaskContent(
    task: Task,
    photos: List<TaskPhoto>,
    onToggleStep: (String, Boolean) -> Unit,
    onStartTask: () -> Unit,
    onCompleteTask: () -> Unit,
    onTakeWorkPhoto: () -> Unit,
    onTakeSelfie: () -> Unit,
    workPhotoCount: Int,
    selfieCount: Int,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val completedSteps = task.steps.count { it.isCompleted }
    val allStepsCompleted = task.steps.isNotEmpty() && completedSteps == task.steps.size
    val progress = if (task.steps.isNotEmpty()) completedSteps.toFloat() / task.steps.size else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            TaskHeader(task = task, progress = progress, completedSteps = completedSteps)
        }

        // Camera action buttons (when task is in progress)
        if (task.status == TaskStatus.IN_PROGRESS) {
            item {
                CameraActionButtons(
                    workPhotoCount = workPhotoCount,
                    selfieCount = selfieCount,
                    onTakeWorkPhoto = onTakeWorkPhoto,
                    onTakeSelfie = onTakeSelfie,
                )
            }
        }

        // Photo gallery strip (when photos exist)
        if (photos.isNotEmpty()) {
            item {
                PhotoGalleryStrip(photos = photos)
            }
        }

        item {
            Text(
                text = stringResource(R.string.task_steps_header, task.steps.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        itemsIndexed(task.steps, key = { _, step -> step.id }) { index, step ->
            StepItem(
                step = step,
                index = index + 1,
                enabled = task.status == TaskStatus.IN_PROGRESS,
                onToggle = { completed -> onToggleStep(step.id, completed) },
            )
        }

        if (error != null) {
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TaskActions(
                status = task.status,
                allStepsCompleted = allStepsCompleted,
                onStartTask = onStartTask,
                onCompleteTask = onCompleteTask,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TaskHeader(
    task: Task,
    progress: Float,
    completedSteps: Int,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = task.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TaskStatusChip(status = task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.wo_task_estimate, task.estimatedMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (task.voiceGuidance != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = task.voiceGuidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(12.dp)
                                .padding(end = 24.dp),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedLinearProgress(
                    progress = progress,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$completedSteps/${task.steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    step: TaskStep,
    index: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = step.isCompleted,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(checked)
                },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$index. ${step.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null,
                )
                AnimatedVisibility(
                    visible = !step.isCompleted,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskActions(
    status: TaskStatus,
    allStepsCompleted: Boolean,
    onStartTask: () -> Unit,
    onCompleteTask: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (status) {
            TaskStatus.PENDING -> {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartTask()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.task_start))
                }
            }
            TaskStatus.IN_PROGRESS -> {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteTask()
                    },
                    enabled = allStepsCompleted,
                    shape = RoundedCornerShape(12.dp),
                    colors = if (allStepsCompleted) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(R.string.task_complete),
                        fontWeight = if (allStepsCompleted) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (!allStepsCompleted) {
                    Text(
                        text = stringResource(R.string.task_complete_all_steps_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TaskStatus.COMPLETED -> {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.task_completed_label))
                }
            }
            else -> {}
        }
    }
}
