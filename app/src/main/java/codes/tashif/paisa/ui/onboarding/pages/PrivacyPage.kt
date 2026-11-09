package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Sms
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val privacyPage = OnboardingPage(
    eyebrow = "Privacy first",
    title = "Your data never leaves the building",
    body = "Everything lives in a local database on this phone. SMS is parsed on-device. " +
        "We don’t even have a place to put your money stories.",
    icon = Icons.Rounded.PrivacyTip,
    heroTone = Tone.Secondary,
    highlights = listOf(
        FeatureHighlight(
            title = "Room DB, home-cooked",
            description = "Accounts, txs, budgets — SQLite on your device. Backup? " +
                "That’s your business (and Export later).",
            icon = Icons.Rounded.Lock,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "SMS stays in the pocket",
            description = "~140 bank parsers run offline. Your inbox is not a product " +
                "feature for someone else.",
            icon = Icons.Rounded.Sms,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "AI is BYOK (bring your own key)",
            description = "Statement magic only if you plug in OpenAI-compatible, Gemini, " +
                "or Anthropic. Your key, your endpoint.",
            icon = Icons.Rounded.AutoAwesome,
            tone = Tone.Tertiary
        )
    ),
    tips = listOf("Re-read the privacy love letters anytime in More → About.")
)
