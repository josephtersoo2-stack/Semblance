package app.semblance.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleBorderBright
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskTraceScreen(
    taskId: String,
    viewModel: TasksViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task by viewModel.getTaskTrace(taskId).collectAsState(initial = null)

    Scaffold(
        containerColor = ConsoleBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TASK EXECUTION TRACE",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    task?.let { t ->
                        val statusColor = taskStatusColor(t.status)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .border(0.5.dp, statusColor, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = t.status.uppercase(),
                                style = Typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            task?.let { t ->
                // Task Overview Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ConsoleSurface)
                        .border(1.dp, ConsoleBorderBright, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "INSTRUCTION PROMPT:",
                        style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t.instruction,
                        style = Typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CREATED: ${timeFormat.format(Date(t.createdAt))}",
                            style = Typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                        )
                        t.completedAt?.let { comp ->
                            Text(
                                text = "COMPLETED: ${timeFormat.format(Date(comp))}",
                                style = Typography.labelSmall.copy(color = AccentGreen, fontSize = 9.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "EXECUTION TRACE LOG (${t.traces.size} ENTRIES):",
                    style = Typography.labelMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(t.traces) { traceLine ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(ConsoleSurfaceElevated)
                                .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = traceLine,
                                style = Typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (traceLine.contains("[DONE]") || traceLine.contains("[VERIFY]")) AccentGreen else if (traceLine.contains("[MOTOR]") || traceLine.contains("[PERCEPTION]")) AccentCyan else TextPrimary
                                )
                            )
                        }
                    }
                }
            } ?: run {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Loading task trace...", style = Typography.bodySmall.copy(color = TextMuted))
                }
            }
        }
    }
}
