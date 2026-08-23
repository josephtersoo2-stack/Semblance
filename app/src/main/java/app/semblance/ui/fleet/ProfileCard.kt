package app.semblance.ui.fleet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.engine.model.ProfileUiState
import app.semblance.ui.components.StatusDot
import app.semblance.ui.components.ThumbImage
import app.semblance.ui.components.WarmthMeter
import app.semblance.ui.components.toActivityIcon
import app.semblance.ui.components.toColor
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentRed
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileCard(
    profile: ProfileUiState,
    thumbJpeg: ByteArray?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = profile.status.toColor()
    val activityIcon = profile.status.toActivityIcon()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ConsoleSurface)
            .border(
                width = if (profile.isLive) 1.5.dp else 1.dp,
                color = if (profile.isLive) statusColor.copy(alpha = 0.6f) else ConsoleBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        // Top Header: Alias & Status Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                StatusDot(status = profile.status, dotSize = 7.dp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "@${profile.alias}",
                    style = Typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Activity Icon badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ConsoleSurfaceElevated)
                    .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = activityIcon,
                    style = Typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = statusColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live Thumbnail viewport frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
        ) {
            ThumbImage(
                jpegBytes = thumbJpeg,
                placeholderLabel = if (profile.isLive) "INITIALIZING..." else "SLEEPING",
                modifier = Modifier.matchParentSize()
            )

            // Live tag badge on thumbnail
            if (profile.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.5.dp, statusColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = profile.status.name,
                        style = Typography.labelSmall.copy(
                            color = statusColor,
                            fontSize = 8.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Device Model & Current Host
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = profile.deviceLabel,
                style = Typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val hostText = profile.currentHost ?: if (profile.isLive) "idle" else "offline"
            Text(
                text = hostText,
                style = Typography.bodySmall.copy(
                    color = if (profile.currentHost != null) AccentCyan else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Proxy health & Next wake
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Proxy badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(if (profile.proxyOk) AccentGreen else AccentRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (profile.proxyOk) "MITM✓" else "PROXY ERR",
                    style = Typography.labelSmall.copy(
                        color = if (profile.proxyOk) AccentGreen else AccentRed,
                        fontSize = 8.sp
                    )
                )
            }

            // Next Wake
            val wakeText = if (profile.isLive) {
                "Active"
            } else if (profile.nextWakeAt != null && profile.nextWakeAt > 0) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                "Wake ${timeFormat.format(Date(profile.nextWakeAt))}"
            } else {
                "Resting"
            }

            Text(
                text = wakeText,
                style = Typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 8.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Warmth Meter
        WarmthMeter(
            warmth = profile.warmth,
            compact = true,
            showLabel = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
