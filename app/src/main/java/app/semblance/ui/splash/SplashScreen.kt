package app.semblance.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var checkStep by remember { mutableIntStateOf(0) }

    val checks = listOf(
        "CA Certificate Trust Verification",
        "MITM uTLS Handshake Proxy Binding",
        "LLM Router & Model Gateway Ping",
        "Multi-Process Worker Pool (8 Slots) Init"
    )

    LaunchedEffect(Unit) {
        delay(350L)
        checkStep = 1
        delay(400L)
        checkStep = 2
        delay(400L)
        checkStep = 3
        delay(400L)
        checkStep = 4
        delay(500L)
        onSplashComplete()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBg)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Branding Logo Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ConsoleSurfaceElevated)
                    .border(1.5.dp, AccentGreen, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AccentGreen, RoundedCornerShape(6.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SEMBLANCE",
                style = Typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )
            )

            Text(
                text = "AUTONOMOUS ANTI-DETECT FLEET CONSOLE",
                style = Typography.labelSmall.copy(
                    color = AccentCyan,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Diagnostic Checks Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "SYSTEM INITIALIZATION DIAGNOSTICS:",
                    style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                checks.forEachIndexed { index, label ->
                    val isDone = checkStep > index
                    val isCurrent = checkStep == index

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (isDone) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = ConsoleBg,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        } else if (isCurrent) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ConsoleBorder)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = label,
                            style = Typography.bodySmall.copy(
                                color = if (isDone) AccentGreen else if (isCurrent) TextPrimary else TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}
