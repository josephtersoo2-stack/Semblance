package app.semblance.ui.maximized

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.ui.components.AgentLogDrawer
import app.semblance.ui.components.InstructionBar
import app.semblance.ui.components.StatusDot
import app.semblance.ui.components.ThumbImage
import app.semblance.ui.components.WarmthMeter
import app.semblance.ui.components.toColor
import app.semblance.ui.theme.AccentAmber
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentPurple
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaximizedScreen(
    viewModel: MaximizedViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val events by viewModel.events.collectAsState()
    val thumbJpeg by viewModel.latestThumb.collectAsState()
    val snapshotResult by viewModel.snapshotResult.collectAsState()

    var showCommentDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }

    Scaffold(
        containerColor = ConsoleBg,
        topBar = {
            // Header: Alias · Device · Volume · Reload · Close
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        profile?.let { p ->
                            StatusDot(status = p.status, dotSize = 8.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "@${p.alias}",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(p.status.toColor().copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = p.status.name,
                                            style = Typography.labelSmall.copy(color = p.status.toColor(), fontSize = 8.sp)
                                        )
                                    }
                                }
                                Text(
                                    text = p.deviceLabel,
                                    style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    // Header quick icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.executeQuickAction("volume") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { viewModel.executeQuickAction("navigate", profile?.currentHost ?: "https://youtube.com") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = AccentGreen, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.close()
                                onNavigateBack()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Viewport", tint = AccentRed, modifier = Modifier.size(18.dp))
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Simulated WebView Viewport Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorderBright, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Address Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ConsoleSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AccentGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "https://${profile?.currentHost ?: "youtube.com/watch?v=sample123"}",
                            style = Typography.labelSmall.copy(
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "TLS: uTLS H2",
                            style = Typography.labelSmall.copy(
                                color = AccentCyan,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Main Viewport Surface (Mock WebView Canvas)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ThumbImage(
                            jpegBytes = thumbJpeg,
                            placeholderLabel = "LIVE WORKER VIEWPORT",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Visual element overlay tags (indicating perception layer)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PERCEPTION: 38 nodes | MOTOR: armed | SUFFIX: p${profile?.id ?: 1}",
                                style = Typography.labelSmall.copy(color = AccentGreen, fontSize = 8.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Action Buttons Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionButton(
                    icon = Icons.Default.Pause,
                    label = "PAUSE",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.executeQuickAction("pause") }
                )
                QuickActionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "LIKE",
                    color = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.executeQuickAction("like") }
                )
                QuickActionButton(
                    icon = Icons.Default.Comment,
                    label = "COMMENT",
                    color = AccentPurple,
                    modifier = Modifier.weight(1.1f),
                    onClick = { showCommentDialog = true }
                )
                QuickActionButton(
                    icon = Icons.Default.Language,
                    label = "OPEN URL",
                    color = AccentGreen,
                    modifier = Modifier.weight(1.1f),
                    onClick = { showUrlDialog = true }
                )
                QuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "SNAPSHOT",
                    color = TextPrimary,
                    modifier = Modifier.weight(1.1f),
                    onClick = { viewModel.requestSnapshot() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instruction Bar
            InstructionBar(
                onSendInstruction = { text, runAt ->
                    viewModel.sendInstruction(text, runAt)
                },
                placeholder = "Send prompt to @${profile?.alias ?: "agent"}..."
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Collapsible Agent Log Drawer
            AgentLogDrawer(
                events = events,
                title = "AGENT LOG DRAWER (@${profile?.alias ?: "agent"})",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Comment Dialog
    if (showCommentDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            containerColor = ConsoleSurfaceElevated,
            title = { Text("Simulate Human Comment", style = Typography.titleMedium) },
            text = {
                Column {
                    Text("Motor layer types text with persona cadence & natural typos:", style = Typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("great breakdown, learned a lot from this setup", color = TextMuted) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = ConsoleBg,
                            unfocusedContainerColor = ConsoleBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeQuickAction("comment", commentText)
                        commentText = ""
                        showCommentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = ConsoleBg)
                ) {
                    Text("TYPE & POST")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCommentDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // Open URL Dialog
    if (showUrlDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            containerColor = ConsoleSurfaceElevated,
            title = { Text("Navigate to URL", style = Typography.titleMedium) },
            text = {
                Column {
                    Text("Worker process proxy routing will resolve host:", style = Typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.TextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = { Text("https://reddit.com/r/technology", color = TextMuted) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = ConsoleBg,
                            unfocusedContainerColor = ConsoleBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeQuickAction("navigate", customUrl)
                        customUrl = ""
                        showUrlDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = ConsoleBg)
                ) {
                    Text("NAVIGATE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUrlDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // Snapshot Result Dialog
    snapshotResult?.let { snapshotJson ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearSnapshot() },
            containerColor = ConsoleSurfaceElevated,
            title = { Text("Perception DOM Snapshot", style = Typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = snapshotJson,
                        style = Typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AccentGreen
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearSnapshot() }) {
                    Text("DISMISS")
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = ConsoleSurfaceElevated,
            contentColor = color
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier
            .height(34.dp)
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
    }
}
