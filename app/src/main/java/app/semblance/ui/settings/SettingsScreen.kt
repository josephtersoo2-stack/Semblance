package app.semblance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val qaReport by viewModel.qaReport.collectAsState()
    val isQaRunning by viewModel.isQaRunning.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "FLEET CONSOLE SETTINGS",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Network & MITM
        SettingsCard(title = "MITM & uTLS NETWORK STACK") {
            var endpointText by remember(settings.mitmEndpoint) { mutableStateOf(settings.mitmEndpoint) }
            OutlinedTextField(
                value = endpointText,
                onValueChange = {
                    endpointText = it
                    viewModel.updateMitmEndpoint(it)
                },
                label = { Text("Local MITM Host:Port", style = Typography.labelSmall) },
                textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ConsoleBg,
                    unfocusedContainerColor = ConsoleBg,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = ConsoleBorder
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CA Root Certificate: INSTALLED & TRUSTED",
                    style = Typography.labelSmall.copy(color = AccentGreen, fontSize = 9.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Worker Pool & Concurrency
        SettingsCard(title = "CONCURRENCY & WORKER POOL") {
            Text(
                text = "Live Process Worker Cap: ${settings.workerPoolSize} concurrent profiles",
                style = Typography.bodySmall.copy(color = TextPrimary)
            )
            Slider(
                value = settings.workerPoolSize.toFloat(),
                onValueChange = { viewModel.updateWorkerPoolSize(it.toInt()) },
                valueRange = 2f..16f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Per-Profile Cache Budget: ${settings.storageBudgetMb} MB",
                style = Typography.bodySmall.copy(color = TextPrimary)
            )
            Slider(
                value = settings.storageBudgetMb.toFloat(),
                onValueChange = { viewModel.updateStorageBudget(it.toInt()) },
                valueRange = 100f..2000f,
                steps = 18,
                colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. LLM Router Configuration
        SettingsCard(title = "LLM INTELLIGENCE GATEWAY") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = "Tactical Loop: ${settings.tacticalModel}", style = Typography.bodySmall.copy(color = TextPrimary))
                    Text(text = "Strategic Planner: ${settings.strategicModel}", style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ONLINE", style = Typography.labelSmall.copy(color = AccentPurple, fontSize = 9.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Detection QA Test Suite Runner
        SettingsCard(title = "DETECTION QA & STEALTH HARNESS") {
            Text(
                text = "Run synthetic JA3/JA4, Client Hints & WebRTC leak diagnostics across all profiles:",
                style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.runFullQaSuite() },
                enabled = !isQaRunning,
                colors = ButtonDefaults.buttonColors(containerColor = ConsoleSurfaceElevated, contentColor = AccentGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            ) {
                if (isQaRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PROBING FLEET FP SIGNATURES...", style = Typography.labelLarge)
                } else {
                    Icon(Icons.Default.Security, contentDescription = "QA", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RUN FLEET DETECTION QA SUITE", style = Typography.labelLarge)
                }
            }

            qaReport?.let { report ->
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ConsoleBg)
                        .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "QA AUDIT: ${report.overallStatus}",
                            style = Typography.labelMedium.copy(color = AccentGreen, fontWeight = FontWeight.Bold)
                        )
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
                        Text(
                            text = timeFormat.format(Date(report.timestamp)),
                            style = Typography.labelSmall.copy(color = TextMuted)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    report.logLines.forEach { line ->
                        Text(
                            text = line,
                            style = Typography.bodySmall.copy(
                                color = if (line.contains("PASS") || line.contains("MATCH") || line.contains("AGREE")) AccentGreen else TextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Danger Zone / Housekeeping
        SettingsCard(title = "HOUSEKEEPING & DATA") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = "Automated Circadian QA", style = Typography.bodyMedium.copy(color = TextPrimary))
                    Text(text = "Schedule daily background fingerprint checks", style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
                }
                Switch(
                    checked = settings.autoQaEnabled,
                    onCheckedChange = { viewModel.updateAutoQa(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ConsoleSurface)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = title, style = Typography.labelMedium.copy(color = AccentCyan, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
