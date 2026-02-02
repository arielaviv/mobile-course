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
import androidx.compose.ui.unit.dp
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
        WorkOrderStatus.DRAFT -> StatusDraft to "Draft"
        WorkOrderStatus.PENDING -> StatusPending to "Pending"
        WorkOrderStatus.SCHEDULED -> StatusScheduled to "Scheduled"
        WorkOrderStatus.IN_PROGRESS -> StatusInProgress to "In Progress"
        WorkOrderStatus.COMPLETED -> StatusCompleted to "Completed"
        WorkOrderStatus.FAILED -> StatusFailed to "Failed"
        WorkOrderStatus.CANCELLED -> StatusCancelled to "Cancelled"
    }
    ChipBase(color = color, label = label)
}

@Composable
fun TaskStatusChip(status: TaskStatus) {
    val (color, label) = when (status) {
        TaskStatus.PENDING -> StatusPending to "Pending"
        TaskStatus.IN_PROGRESS -> StatusInProgress to "In Progress"
        TaskStatus.BLOCKED -> StatusBlocked to "Blocked"
        TaskStatus.ESCALATED -> StatusEscalated to "Escalated"
        TaskStatus.COA_PENDING -> StatusScheduled to "COA Pending"
        TaskStatus.COA_APPROVED -> StatusCompleted to "COA Approved"
        TaskStatus.COA_REJECTED -> StatusFailed to "COA Rejected"
        TaskStatus.COMPLETED -> StatusCompleted to "Completed"
        TaskStatus.CANCELLED -> StatusCancelled to "Cancelled"
        TaskStatus.SKIPPED -> StatusCancelled to "Skipped"
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
