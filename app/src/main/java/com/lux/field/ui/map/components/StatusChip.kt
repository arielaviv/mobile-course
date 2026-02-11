package com.lux.field.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lux.field.R
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.ui.theme.StatusBlocked
import com.lux.field.ui.theme.StatusCancelled
import com.lux.field.ui.theme.StatusCompleted
import com.lux.field.ui.theme.StatusDraft
import com.lux.field.ui.theme.StatusEscalated
import com.lux.field.ui.theme.StatusFailed
import com.lux.field.ui.theme.StatusInProgress
import com.lux.field.ui.theme.StatusPending
import com.lux.field.ui.theme.StatusScheduled

@Composable
fun StatusChip(status: WorkOrderStatus) {
    val (color, label) = when (status) {
        WorkOrderStatus.DRAFT -> StatusDraft to stringResource(R.string.status_draft)
        WorkOrderStatus.PENDING -> StatusPending to stringResource(R.string.status_pending)
        WorkOrderStatus.SCHEDULED -> StatusScheduled to stringResource(R.string.status_scheduled)
        WorkOrderStatus.IN_PROGRESS -> StatusInProgress to stringResource(R.string.status_in_progress)
        WorkOrderStatus.COMPLETED -> StatusCompleted to stringResource(R.string.status_completed)
        WorkOrderStatus.FAILED -> StatusFailed to stringResource(R.string.status_failed)
        WorkOrderStatus.CANCELLED -> StatusCancelled to stringResource(R.string.status_cancelled)
    }
    ChipBase(color = color, label = label)
}

@Composable
fun TaskStatusChip(status: TaskStatus) {
    val (color, label) = when (status) {
        TaskStatus.PENDING -> StatusPending to stringResource(R.string.status_pending)
        TaskStatus.IN_PROGRESS -> StatusInProgress to stringResource(R.string.status_in_progress)
        TaskStatus.BLOCKED -> StatusBlocked to stringResource(R.string.status_blocked)
        TaskStatus.ESCALATED -> StatusEscalated to stringResource(R.string.status_escalated)
        TaskStatus.COA_PENDING -> StatusScheduled to stringResource(R.string.status_coa_pending)
        TaskStatus.COA_APPROVED -> StatusCompleted to stringResource(R.string.status_coa_approved)
        TaskStatus.COA_REJECTED -> StatusFailed to stringResource(R.string.status_coa_rejected)
        TaskStatus.COMPLETED -> StatusCompleted to stringResource(R.string.status_completed)
        TaskStatus.CANCELLED -> StatusCancelled to stringResource(R.string.status_cancelled)
        TaskStatus.SKIPPED -> StatusCancelled to stringResource(R.string.status_skipped)
    }
    ChipBase(color = color, label = label)
}

@Composable
private fun ChipBase(color: Color, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
