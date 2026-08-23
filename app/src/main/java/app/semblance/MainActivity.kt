package app.semblance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.semblance.ui.nav.AppNav
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.SemblanceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SemblanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ConsoleBg
                ) {
                    AppNav()
                }
            }
        }
    }
}
