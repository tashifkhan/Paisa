package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.WavingHand
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val helloPage = OnboardingPage(
    eyebrow = "Hello money",
    title = "Paisa says hi 👋",
    body = "Your pocket-sized finance buddy. Offline-first, SMS-smart, chart-happy — " +
        "and zero creepy cloud accounts.",
    icon = Icons.Rounded.WavingHand,
    heroTone = Tone.Primary,
    highlights = listOf(
        FeatureHighlight(
            title = "Made for how you actually pay",
            description = "UPI pings, bank SMS, credit cards, wallets, cash — all in one " +
                "calm place with ₹ vibes by default.",
            icon = Icons.Rounded.Payments,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "No signup. No drama.",
            description = "Open → use. No email, no ads, no “sync with our servers” " +
                "plot twists.",
            icon = Icons.Rounded.Lock,
            tone = Tone.Secondary
        )
    ),
    tips = listOf(
        "Five tabs. That’s the whole universe: Home · Analytics · Budgets · Accounts · More."
    )
)
