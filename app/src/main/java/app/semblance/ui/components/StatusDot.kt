package app.semblance.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.ProfileStatus
import app.semblance.ui.theme.StatusBrowsingColor
import app.semblance.ui.theme.StatusErrorColor
import app.semblance.ui.theme.StatusIdleColor
import app.semblance.ui.theme.StatusSleepColor
import app.semblance.ui.theme.StatusTypingColor
import app.semblance.ui.theme.StatusWakingColor
import app.semblance.ui.theme.StatusWatchingColor
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography

fun ProfileStatus.toColor(): Color = when (this) {
    ProfileStatus.SLEEPING -> StatusSleepColor
    ProfileStatus.WAKING -> StatusWakingColor
    ProfileStatus.IDLE -> StatusIdleColor
    ProfileStatus.BROWSING -> StatusBrowsingColor
    ProfileStatus.WATCHING -> StatusWatchingColor
    ProfileStatus.TYPING -> StatusTypingColor
    ProfileStatus.ERROR -> StatusErrorColor
}

fun ProfileStatus.toActivityIcon(): String = when (this) {
    ProfileStatus.WATCHING -> "▶"
    ProfileStatus.BROWSING -> "🔎"
    ProfileStatus.TYPING -> "💬"
    ProfileStatus.SLEEPING -> "zZ"
    ProfileStatus.WAKING -> "⚡"
    ProfileStatus.IDLE -> "⏸"
    ProfileStatus.ERROR -> "⚠"
}

@Composable
fun StatusDot(
    status: ProfileStatus,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    showLabel: Boolean = false
) {
    val color = status.toColor()
    val isAnimated = status in listOf(
        ProfileStatus.BROWSING,
        ProfileStatus.WATCHING,
        ProfileStatus.TYPING,
        ProfileStatus.WAKING
    )

    val scale by if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(dotSize * 1.6f)
        ) {
            if (isAnimated) {
                Box(
                    modifier = Modifier
                        .size(dotSize * 1.5f)
                        .scale(scale)
                        .background(color.copy(alpha = 0.35f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.name,
                style = Typography.labelSmall.copy(
                    color = color,
                    fontSize = 10.sp
                )
            )
        }
    }
}
