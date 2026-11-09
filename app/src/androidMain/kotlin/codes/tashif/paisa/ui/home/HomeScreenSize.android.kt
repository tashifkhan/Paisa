package codes.tashif.paisa.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun screenHeightDp(): Int = LocalConfiguration.current.screenHeightDp
