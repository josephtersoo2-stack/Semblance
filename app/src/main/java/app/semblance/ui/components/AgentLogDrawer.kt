package app.semblance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.AgentEvent
import app.semblance.ui.theme.AccentAmber
import app.semblance.ui.theme.AccentBlue
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentPurple
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

fun agentKindColor(kind: String): Color = when (kind.lowercase()) {
    "llm" -> AccentPurple
    "motor" -> AccentCyan
    "nav" -> AccentBlue
    "mitm" -> AccentGreen
    else -> AccentAmber
}

@Composable
fun AgentLogDrawer(
    events: List<AgentEvent>,
    modifier: Modifier = Modifier,
    title: String = "AGENT TELEMETRY DRAWER",
    isInitiallyExpanded: Boolean = true
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }
    val listState = rememberLazyListState()

    LaunchedEffect(events.size) {
        if (events.isNotEmpty() && expanded) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(ConsoleSurface)
            .border(1.dp, ConsoleBorderBright, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
    ) {
        // Drawer Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentCyan, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = Typography.labelLarge.copy(fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${events.size} frames)",
                    style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .background(ConsoleSurfaceVariant)
            ) {
                if (events.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text(
                            text = "Awaiting agent telemetry stream...",
                            style = Typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(events, key = { "${it.profileId}_${it.ts}_${it.text.hashCode()}" }) { event ->
                            LogEventItem(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogEventItem(event: AgentEvent) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val timeStr = remember(event.ts) { timeFormatter.format(Date(event.ts)) }
    val color = agentKindColor(event.kind)

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleSurfaceElevated)
            .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = timeStr,
            style = Typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        )
        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = event.kind.uppercase(),
                style = Typography.labelSmall.copy(
                    color = color,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = event.text,
            style = Typography.bodySmall.copy(
                color = TextPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
