package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Sync
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val smsPage = OnboardingPage(
    eyebrow = "SMS wizardry",
    title = "Bank texts → real transactions",
    body = "One permission. Paisa reads financial SMS, pulls amount, merchant, account, " +
        "balance — then files it like a tidy accountant who likes Material You.",
    icon = Icons.Rounded.Sms,
    heroTone = Tone.Secondary,
    highlights = listOf(
        FeatureHighlight(
            title = "Full scan / scan new",
            description = "Big first pass, then tiny incremental sips. Progress UI " +
                "while WorkManager hustles.",
            icon = Icons.Rounded.Sync,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Live catch",
            description = "New bank alert? Receiver can import it in the background " +
                "so Home stays fresh.",
            icon = Icons.Rounded.Sms,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "No double fries",
            description = "Hash dedup stops clones. Weird financial SMS lands in " +
                "“unrecognized” for you to judge later.",
            icon = Icons.Rounded.FilterList,
            tone = Tone.Tertiary
        )
    ),
    tips = listOf("Start at More → SMS import. Emulators are boring — real phones shine.")
)
