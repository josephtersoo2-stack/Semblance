package app.semblance.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.ProfileUiState
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentPurple
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleBorderBright
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskComposer(
    profiles: List<ProfileUiState>,
    onDispatch: (targets: List<Int>, text: String, runAt: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var instructionText by remember { mutableStateOf("") }
    var selectedTargetIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var targetAll by remember { mutableStateOf(true) }
    var scheduleLater by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ConsoleSurface)
            .border(1.dp, ConsoleBorderBright, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "NATURAL LANGUAGE INSTRUCTION COMPOSER",
            style = Typography.labelMedium.copy(color = AccentCyan)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Target profile chips
        Text(text = "TARGET PROFILES:", style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = targetAll,
                onClick = {
                    targetAll = true
                    selectedTargetIds = emptySet()
                },
                label = { Text("ALL FLEET (${profiles.size})", style = Typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                    selectedLabelColor = AccentCyan
                )
            )

            profiles.forEach { p ->
                val isSelected = !targetAll && selectedTargetIds.contains(p.id)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        targetAll = false
                        selectedTargetIds = if (selectedTargetIds.contains(p.id)) {
                            selectedTargetIds - p.id
                        } else {
                            selectedTargetIds + p.id
                        }
                        if (selectedTargetIds.isEmpty()) {
                            targetAll = true
                        }
                    },
                    label = { Text("@${p.alias}", style = Typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                        selectedLabelColor = AccentGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Instruction Text Field
        OutlinedTextField(
            value = instructionText,
            onValueChange = { instructionText = it },
            placeholder = {
                Text(
                    text = "e.g. 'Search for top indie mechanical keyboards, scroll to result #3, watch video for 2 minutes, and leave a positive comment about the switches'",
                    color = TextMuted,
                    style = Typography.bodySmall
                )
            },
            minLines = 2,
            maxLines = 4,
            textStyle = Typography.bodyMedium.copy(color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ConsoleBg,
                unfocusedContainerColor = ConsoleBg,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = ConsoleBorder,
                cursorColor = AccentCyan
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dispatch Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (scheduleLater) AccentPurple.copy(alpha = 0.2f) else ConsoleSurfaceElevated)
                        .clickable { scheduleLater = !scheduleLater }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (scheduleLater) "SCHEDULE: IN 10 MIN" else "EXECUTE: IMMEDIATE",
                        style = Typography.labelSmall.copy(
                            color = if (scheduleLater) AccentPurple else TextSecondary,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Button(
                onClick = {
                    if (instructionText.isNotBlank()) {
                        val targets = if (targetAll) profiles.map { it.id } else selectedTargetIds.toList()
                        val runAt = if (scheduleLater) System.currentTimeMillis() + 600000L else null
                        onDispatch(targets, instructionText.trim(), runAt)
                        instructionText = ""
                    }
                },
                enabled = instructionText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = ConsoleBg,
                    disabledContainerColor = ConsoleSurfaceElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Dispatch", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DISPATCH TASK", style = Typography.labelLarge.copy(fontSize = 11.sp))
            }
        }
    }
}
