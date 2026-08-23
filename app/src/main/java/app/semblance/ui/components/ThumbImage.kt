package app.semblance.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.Typography

@Composable
fun ThumbImage(
    jpegBytes: ByteArray?,
    modifier: Modifier = Modifier,
    placeholderLabel: String = "NO SIGNAL"
) {
    val bitmap = remember(jpegBytes) {
        if (jpegBytes != null && jpegBytes.isNotEmpty()) {
            try {
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ConsoleSurface)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Live Profile Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = placeholderLabel,
                    style = Typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}
