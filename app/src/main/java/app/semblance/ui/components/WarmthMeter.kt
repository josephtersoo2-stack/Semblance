package app.semblance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.ui.theme.AccentAmber
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentRed
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography

fun warmthColor(warmth: Int): Color = when {
    warmth < 30 -> AccentRed
    warmth < 65 -> AccentAmber
    warmth < 85 -> AccentCyan
    else -> AccentGreen
}

@Composable
fun WarmthMeter(
    warmth: Int,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    showLabel: Boolean = true,
    compact: Boolean = false
) {
    val clampedWarmth = warmth.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedWarmth / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "warmthProgress"
    )
    val color = warmthColor(clampedWarmth)

    if (compact) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ConsoleSurfaceElevated)
                    .border(0.5.dp, ConsoleBorder, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentAmber, color)
                            )
                        )
                )
            }
            if (showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${clampedWarmth}%",
                    style = Typography.labelSmall.copy(
                        color = color,
                        fontSize = 9.sp
                    )
                )
            }
        }
    } else {
        Column(modifier = modifier) {
            if (showLabel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "WARMTH",
                        style = Typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${clampedWarmth}/100",
                        style = Typography.labelSmall.copy(
                            color = color,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ConsoleSurfaceElevated)
                    .border(0.5.dp, ConsoleBorder, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentAmber, color)
                            )
                        )
                )
            }
        }
    }
}
