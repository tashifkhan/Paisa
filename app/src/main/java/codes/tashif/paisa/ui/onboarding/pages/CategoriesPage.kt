package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Payments
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val categoriesPage = OnboardingPage(
    eyebrow = "Brain food",
    title = "Categories & merchant memory",
    body = "Rich defaults out of the box. Teach Paisa “Swiggy = Food” once — it " +
        "remembers like a loyal raccoon.",
    icon = Icons.Rounded.Category,
    heroTone = Tone.Tertiary,
    highlights = listOf(
        FeatureHighlight(
            title = "Category atelier",
            description = "Colors, icons, income vs expense — used in txs, budgets, charts.",
            icon = Icons.Rounded.Category,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Merchant rules",
            description = "Save mappings; apply to past rows if you’re feeling thorough.",
            icon = Icons.AutoMirrored.Rounded.Rule,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "Currency flex",
            description = "More → Currency. Pick the symbol your soul (and salary) uses.",
            icon = Icons.Rounded.Payments,
            tone = Tone.Tertiary
        )
    )
)
