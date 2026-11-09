package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val statementsPage = OnboardingPage(
    eyebrow = "Statements + AI",
    title = "PDFs welcome (your key, your rules)",
    body = "When SMS isn’t enough, upload statements. You bring the AI key — Paisa never " +
        "ships a secret cloud brain.",
    icon = Icons.Rounded.PictureAsPdf,
    heroTone = Tone.Tertiary,
    highlights = listOf(
        FeatureHighlight(
            title = "Provider playground",
            description = "OpenAI-compatible, Gemini, Anthropic. Encrypted key storage. " +
                "Hit test before you import a novel-length PDF.",
            icon = Icons.Rounded.AutoAwesome,
            tone = Tone.Tertiary
        ),
        FeatureHighlight(
            title = "Preview party",
            description = "Review rows, skip dupes, commit only the good stuff.",
            icon = Icons.Rounded.PictureAsPdf,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Export the whole saga",
            description = "CSV for spreadsheets, PDF for “look I have adult documents”.",
            icon = Icons.Rounded.Share,
            tone = Tone.Secondary
        )
    ),
    tips = listOf("SMS still never phones home — only statement files you choose can leave.")
)
