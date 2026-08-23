package app.semblance.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.TaskUi
import app.semblance.ui.theme.AccentAmber
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentRed
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleBorderBright
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.ConsoleSurfaceVariant
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun taskStatusColor(status: String): Color = when (status.lowercase()) {
    "queued" -> AccentAmber
    "running" -> AccentCyan
    "done" -> AccentGreen
    else -> AccentRed
}

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onNavigateToTrace: (taskId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBg)
            .padding(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "FLEET TASK ORCHESTRATOR",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${tasks.size} TASKS",
                style = Typography.labelMedium.copy(color = AccentCyan)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Composer
        TaskComposer(
            profiles = profiles,
            onDispatch = { targets, text, runAt ->
                viewModel.sendInstruction(targets, text, runAt)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tasks Queue
        Text(
            text = "DISPATCHED TASKS & QUEUE:",
            style = Typography.labelMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (tasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "No tasks currently in queue.\nDispatch an instruction using the composer above.",
                    style = Typography.bodySmall.copy(color = TextMuted)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigateToTrace(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskUi,
    onClick: () -> Unit
) {
    val statusColor = taskStatusColor(task.status)
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val createdStr = timeFormat.format(Date(task.createdAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ConsoleSurface)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        // Status & Target profiles row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(0.5.dp, statusColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.status.uppercase(),
                        style = Typography.labelSmall.copy(
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TARGETS: ${if (task.targets.isEmpty()) "ALL FLEET" else task.targets.map { "p$it" }.joinToString()}",
                    style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
            }

            Text(
                text = createdStr,
                style = Typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Instruction Text
        Text(
            text = task.instruction,
            style = Typography.bodyMedium.copy(
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Footer Trace Preview
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val lastTrace = task.traces.lastOrNull() ?: "Awaiting agent execution tick..."
            Text(
                text = lastTrace,
                style = Typography.bodySmall.copy(
                    color = AccentCyan,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "View Trace",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
