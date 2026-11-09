package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VisibilityOff
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val youVibesPage = OnboardingPage(
    eyebrow = "You vibes",
    title = "Lock it. Hide it. Theme it.",
    body = "Biometrics, default-hidden balances, Material You colors, AMOLED black, " +
        "and springy expressive motion — all on brand with m3.material.io energy.",
    icon = Icons.Rounded.Fingerprint,
    heroTone = Tone.Error,
    highlights = listOf(
        FeatureHighlight(
            title = "App lock",
            description = "Fingerprint / face / device PIN when opening. Relocks when " +
                "you bounce to another app.",
            icon = Icons.Rounded.Fingerprint,
            tone = Tone.Error
        ),
        FeatureHighlight(
            title = "Hide by default",
            description = "Shoulder-surfers see dots. You see the eye on the balance tile.",
            icon = Icons.Rounded.VisibilityOff,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "Appearance playground",
            description = "System/light/dark · dynamic wallpaper colors · fixed palettes · " +
                "AMOLED · expressive motion.",
            icon = Icons.Rounded.Palette,
            tone = Tone.Tertiary
        )
    ),
    tips = listOf("Miss this tour? More → About → Feature tour. Instant encore.")
)
