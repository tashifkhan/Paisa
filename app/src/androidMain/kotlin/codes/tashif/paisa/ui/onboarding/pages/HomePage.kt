package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VisibilityOff
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val homePage = OnboardingPage(
    eyebrow = "Home base",
    title = "Balances, but make them cute",
    body = "Home is the snack bar of your finances: multi-shape tiles for total, income, " +
        "and expense — plus a day-by-day feed you can actually search.",
    icon = Icons.Rounded.Home,
    heroTone = Tone.Primary,
    highlights = listOf(
        FeatureHighlight(
            title = "Shape-y balance cluster",
            description = "Big hero tile for total cash-like balances. Expand for " +
                "per-account crumbs. Credit cards hang out separately (no debt cosplay).",
            icon = Icons.Rounded.AccountBalanceWallet,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Peek-a-boo amounts",
            description = "Numbers start as ••••••. Tap the eye on the balance tile " +
                "to reveal. Change the default under Privacy & security.",
            icon = Icons.Rounded.VisibilityOff,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "Find anything, fast",
            description = "Search merchants & notes. Chip-filter by type, category, " +
                "account, or “from SMS”.",
            icon = Icons.Rounded.Search,
            tone = Tone.Tertiary
        ),
        FeatureHighlight(
            title = "+ and ↻",
            description = "FAB adds a manual tx. Mini sync rescans SMS without " +
                "leaving the vibe.",
            icon = Icons.Rounded.Sync,
            tone = Tone.Surface
        )
    ),
    tips = listOf(
        "Tap a row → full detail, edit, delete.",
        "Credit panel = available-to-spend + “how cooked is my limit”."
    )
)
