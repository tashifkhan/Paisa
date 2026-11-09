package codes.tashif.paisa.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Content model for the feature tour. The pages themselves live in pages/.

internal enum class Tone {
    Primary, Secondary, Tertiary, Error, Surface
}

internal data class FeatureHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tone: Tone = Tone.Primary
)

internal data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val heroTone: Tone = Tone.Primary,
    val highlights: List<FeatureHighlight> = emptyList(),
    val tips: List<String> = emptyList()
)

@Composable
internal fun Tone.container(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.primaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.secondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.Error -> MaterialTheme.colorScheme.errorContainer
    Tone.Surface -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
internal fun Tone.content(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.Error -> MaterialTheme.colorScheme.onErrorContainer
    Tone.Surface -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun FeatureHighlight.container(): Color = tone.container()

@Composable
internal fun FeatureHighlight.content(): Color = tone.content()
