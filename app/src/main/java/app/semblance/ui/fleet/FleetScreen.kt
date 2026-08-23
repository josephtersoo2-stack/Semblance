package app.semblance.ui.fleet

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.ProfileUiState
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentOrange
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
fun FleetScreen(
    viewModel: FleetViewModel,
    onNavigateToMaximized: (id: Int) -> Unit,
    onNavigateToWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val thumbsMap by viewModel.thumbsMap.collectAsState()

    var selectedProfileForMenu by remember { mutableStateOf<ProfileUiState?>(null) }
    var showInstructionDialog by remember { mutableStateOf(false) }
    var instructionText by remember { mutableStateOf("") }

    val liveCount = profiles.count { it.isLive }
    val totalCount = profiles.size

    Scaffold(
        containerColor = ConsoleBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToWizard,
                containerColor = AccentGreen,
                contentColor = ConsoleBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Profile", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar: Telemetry Status & Fleet Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // System status badges
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgeItem(text = "MITM✓", color = AccentGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeItem(text = "LLM✓", color = AccentPurple)
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeItem(text = "POOL $liveCount/8", color = AccentCyan)
                    }

                    // Total Fleet Count
                    Text(
                        text = "FLEET: $totalCount",
                        style = Typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons: Start Day & Pause Fleet
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.startDayAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConsoleSurfaceElevated,
                            contentColor = AccentGreen
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Day", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START DAY", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }

                    OutlinedButton(
                        onClick = { viewModel.pauseFleetAll() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentOrange
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause Fleet", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAUSE FLEET", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }

            // 2-Column Fleet Grid
            if (profiles.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO PROFILES IN CONSOLE",
                            style = Typography.titleMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ NEW' to synthesize a browser persona",
                            style = Typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            thumbJpeg = thumbsMap[profile.id],
                            onClick = { onNavigateToMaximized(profile.id) },
                            onLongClick = { selectedProfileForMenu = profile }
                        )
                    }
                }
            }
        }
    }

    // Long-Press Context Bottom Sheet (7 Actions)
    selectedProfileForMenu?.let { profile ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedProfileForMenu = null },
            sheetState = sheetState,
            containerColor = ConsoleSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ACTIONS: @${profile.alias}",
                        style = Typography.titleMedium.copy(color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = profile.deviceLabel,
                        style = Typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action 1: Maximize
                ActionMenuItem(
                    icon = Icons.Default.Fullscreen,
                    title = "Maximize Profile",
                    subtitle = "Open dedicated operator viewport & agent log drawer",
                    color = AccentCyan,
                    onClick = {
                        selectedProfileForMenu = null
                        onNavigateToMaximized(profile.id)
                    }
                )

                // Action 2: Send Instruction
                ActionMenuItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Send Instruction",
                    subtitle = "Dispatch prompt to profile's tactical LLM loop",
                    color = AccentPurple,
                    onClick = {
                        showInstructionDialog = true
                    }
                )

                // Action 3: Wake Now
                ActionMenuItem(
                    icon = Icons.Default.WbSunny,
                    title = "Wake Now",
                    subtitle = "Force circadian wake & spawn worker process",
                    color = AccentGreen,
                    onClick = {
                        viewModel.wakeNow(profile.id)
                        selectedProfileForMenu = null
                    }
                )

                // Action 4: Sleep Now
                ActionMenuItem(
                    icon = Icons.Default.NightlightRound,
                    title = "Sleep Now",
                    subtitle = "Force circadian sleep & park worker process",
                    color = TextSecondary,
                    onClick = {
                        viewModel.sleepNow(profile.id)
                        selectedProfileForMenu = null
                    }
                )

                // Action 5: Run QA
                ActionMenuItem(
                    icon = Icons.Default.Security,
                    title = "Run Detection QA",
                    subtitle = "Run JA3/JA4, Client Hints & leak test suite",
                    color = AccentOrange,
                    onClick = {
                        viewModel.runQa(profile.id)
                        selectedProfileForMenu = null
                    }
                )

                // Action 6: Edit Profile
                ActionMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Modify persona, device hints, proxy, or interests",
                    color = AccentCyan,
                    onClick = {
                        selectedProfileForMenu = null
                        onNavigateToWizard()
                    }
                )

                // Action 7: Delete Profile
                ActionMenuItem(
                    icon = Icons.Default.DeleteOutline,
                    title = "Delete Profile",
                    subtitle = "Purge cookies, storage suffix & identity record",
                    color = AccentRed,
                    onClick = {
                        viewModel.deleteProfile(profile.id)
                        selectedProfileForMenu = null
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Dialog for Instruction Input
    if (showInstructionDialog && selectedProfileForMenu != null) {
        val targetProfile = selectedProfileForMenu!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showInstructionDialog = false },
            containerColor = ConsoleSurfaceElevated,
            title = {
                Text(
                    text = "Instruction for @${targetProfile.alias}",
                    style = Typography.titleMedium.copy(color = TextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter tactical or strategic intent for the browser agent:",
                        style = Typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = instructionText,
                        onValueChange = { instructionText = it },
                        placeholder = { Text("e.g. Search 'best mechanical keyboards', watch 2min", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ConsoleBg,
                            unfocusedContainerColor = ConsoleBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (instructionText.isNotBlank()) {
                            viewModel.sendInstruction(targetProfile.id, instructionText.trim())
                            instructionText = ""
                            showInstructionDialog = false
                            selectedProfileForMenu = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = ConsoleBg)
                ) {
                    Text("DISPATCH", style = Typography.labelLarge)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showInstructionDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )
            Text(
                text = subtitle,
                style = Typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun BadgeItem(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = Typography.labelSmall.copy(
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
