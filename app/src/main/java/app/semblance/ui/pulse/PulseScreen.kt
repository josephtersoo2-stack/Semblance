package app.semblance.ui.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.ProfileUiState
import app.semblance.ui.components.LogEventItem
import app.semblance.ui.components.StatusDot
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
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PulseScreen(
    viewModel: PulseViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val events by viewModel.events.collectAsState()
    val drifts = viewModel.interestDrifts
    val stats = viewModel.stats

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBg)
            .padding(12.dp)
    ) {
        // Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "PULSE & TELEMETRY OBSERVATORY",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("REAL-TIME", style = Typography.labelSmall.copy(color = AccentGreen, fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aggregate Stats Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(label = "WATCH TIME", value = "${stats.totalWatchMin}m", color = AccentCyan, modifier = Modifier.weight(1f))
            StatCard(label = "COMMENTS", value = "${stats.totalComments}", color = AccentPurple, modifier = Modifier.weight(1f))
            StatCard(label = "SESSIONS", value = "${stats.totalSessions}", color = AccentAmber, modifier = Modifier.weight(1f))
            StatCard(label = "AVG WARMTH", value = "${stats.avgWarmth}%", color = AccentGreen, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 24H Session Timeline Bars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ConsoleSurface)
                .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "24H CIRCADIAN TIMELINES (N=${profiles.size})",
                    style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                )
                Text(
                    text = "00:00 ── 06:00 ── 12:00 ── 18:00 ── 24:00",
                    style = Typography.labelSmall.copy(color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            profiles.take(6).forEach { profile ->
                ProfileTimelineRow(profile = profile)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interest Graph Drift Chips
        Text(
            text = "PERSONA INTEREST GRAPH & WEIGHT DRIFT:",
            style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            drifts.forEach { item ->
                val isUp = item.deltaPercent > 0
                val arrow = if (isUp) "↑" else if (item.deltaPercent < 0) "↓" else "→"
                val deltaColor = if (isUp) AccentGreen else if (item.deltaPercent < 0) AccentRed else TextSecondary

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ConsoleSurfaceElevated)
                        .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.topic,
                            style = Typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$arrow ${if (isUp) "+" else ""}${item.deltaPercent}%",
                            style = Typography.labelSmall.copy(color = deltaColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Event Feed
        Text(
            text = "LIVE FLEET TELEMETRY FEED (${events.size} EVENTS):",
            style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(events) { event ->
                LogEventItem(event = event)
            }
        }
    }
}

@Composable
fun ProfileTimelineRow(profile: ProfileUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        StatusDot(status = profile.status, dotSize = 6.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "@${profile.alias.take(8)}",
            style = Typography.labelSmall.copy(fontSize = 9.sp, color = TextPrimary),
            modifier = Modifier.width(64.dp)
        )

        // 24H segmented timeline bar
        Row(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ConsoleBg)
                .border(0.5.dp, ConsoleBorder, RoundedCornerShape(2.dp))
        ) {
            val statusColor = profile.status.toColor()
            // Synthetic 24-hour activity distribution
            Box(modifier = Modifier.weight(2f).fillMaxHeight().background(ConsoleBg))
            Box(modifier = Modifier.weight(1.5f).fillMaxHeight().background(statusColor.copy(alpha = 0.4f)))
            Box(modifier = Modifier.weight(3f).fillMaxHeight().background(ConsoleBg))
            Box(modifier = Modifier.weight(2.5f).fillMaxHeight().background(statusColor))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(statusColor.copy(alpha = 0.7f)))
            Box(modifier = Modifier.weight(2f).fillMaxHeight().background(ConsoleBg))
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ConsoleSurface)
            .border(0.5.dp, ConsoleBorder, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(text = label, style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 8.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = Typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp))
    }
}
