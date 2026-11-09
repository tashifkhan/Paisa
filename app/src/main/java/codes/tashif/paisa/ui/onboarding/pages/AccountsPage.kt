package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val accountsPage = OnboardingPage(
    eyebrow = "Accounts",
    title = "Banks, wallets, cards — lined up",
    body = "Auto from SMS or add by hand. Carousel for drama, list for control. Merge " +
        "the twins. Star a default.",
    icon = Icons.Rounded.AccountBalance,
    heroTone = Tone.Secondary,
    highlights = listOf(
        FeatureHighlight(
            title = "Two vibes",
            description = "Swipe-y cards or drag-to-reorder list. Your order, your rules.",
            icon = Icons.Rounded.AccountBalanceWallet,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Credit card honesty",
            description = "Outstanding, optional limit, utilization — so “available” " +
                "isn’t a mystery.",
            icon = Icons.Rounded.CreditCard,
            tone = Tone.Error
        ),
        FeatureHighlight(
            title = "Default · rename · merge",
            description = "Star for new txs. Rename SMS gibberish. Merge when HDFC " +
                "shows up twice like a sitcom twin.",
            icon = Icons.AutoMirrored.Rounded.MergeType,
            tone = Tone.Tertiary
        )
    )
)
