package sh.taf.billi.ui.ai

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * False when the user turned AI off in More. Screens that offer an AI
 * feature hide it. [sh.taf.billi.ui.app.BilliApp] provides it from settings.
 */
val LocalAiEnabled = staticCompositionLocalOf { true }
