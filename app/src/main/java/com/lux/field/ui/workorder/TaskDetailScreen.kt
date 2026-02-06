package com.lux.field.ui.workorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lux.field.R
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.model.TaskStep
import com.lux.field.ui.map.components.TaskStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    workOrderId: String,
    taskId: String,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onToggleStep = viewModel::toggleStepCompletion,
                onStartTask = viewModel::startTask,
                onCompleteTask = viewModel::completeTask,
                error = uiState.error,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun TaskContent(
    task: Task,
    onToggleStep: (String, Boolean) -> Unit,
    onStartTask: () -> Unit,
    onCompleteTask: () -> Unit,
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
                ) {
                    Text(
                        text = task.voiceGuidance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = step.isCompleted,
                onCheckedChange = { onToggle(it) },
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
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (status) {
            TaskStatus.PENDING -> {
                Button(
                    onClick = onStartTask,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.task_start))
                }
            }
            TaskStatus.IN_PROGRESS -> {
                Button(
                    onClick = onCompleteTask,
                    enabled = allStepsCompleted,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.task_complete))
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.task_completed_label))
                }
            }
            else -> {}
        }
    }
}
