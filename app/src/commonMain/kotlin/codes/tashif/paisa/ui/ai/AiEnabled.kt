package codes.tashif.paisa.ui.ai

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * False when the user turned AI off in More. Screens that offer an AI
 * feature hide it. [codes.tashif.paisa.ui.app.PaisaApp] provides it from settings.
 */
val LocalAiEnabled = staticCompositionLocalOf { true }
