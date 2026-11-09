package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val readyPage = OnboardingPage(
    eyebrow = "Ready set",
    title = "You’re good. Go make a mess (tracked).",
    body = "Pick a lane: import SMS, add one manual transaction, or just poke the empty " +
        "charts until data appears. Paisa gets better every row.",
    icon = Icons.Rounded.Check,
    heroTone = Tone.Primary,
    highlights = listOf(
        FeatureHighlight(
            title = "① SMS import",
            description = "More → SMS import → allow → full rescan. Watch accounts bloom.",
            icon = Icons.Rounded.Sms,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "② Manual snack",
            description = "Home → + · amount · category · account · optional “remember me”.",
            icon = Icons.Rounded.Payments,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "③ Tour the tabs",
            description = "Analytics after data · Budgets for limits · Accounts for stacks · " +
                "More for the kitchen sink.",
            icon = Icons.Rounded.BarChart,
            tone = Tone.Tertiary
        )
    ),
    tips = listOf(
        "This tour hides after “Let’s go” — replay from About whenever.",
        "May your balances always be green-ish."
    )
)
