package codes.tashif.paisa.ui.onboarding.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import codes.tashif.paisa.ui.onboarding.FeatureHighlight
import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.onboarding.Tone

internal val insightsPage = OnboardingPage(
    eyebrow = "Insights",
    title = "Charts that slap (gently)",
    body = "Analytics turns chaos into donuts, bars, and heatmaps. Budgets turn “I’ll " +
        "be good” into an actual number.",
    icon = Icons.Rounded.BarChart,
    heroTone = Tone.Primary,
    highlights = listOf(
        FeatureHighlight(
            title = "Analytics tab",
            description = "Flip months. Income vs expense. Category donut. Daily bars. " +
                "Calendar heatmap. Top categories with share %.",
            icon = Icons.Rounded.BarChart,
            tone = Tone.Primary
        ),
        FeatureHighlight(
            title = "Budgets tab",
            description = "Monthly caps per category or overall. Progress that doesn’t " +
                "lie to you at 11pm.",
            icon = Icons.Rounded.DonutLarge,
            tone = Tone.Secondary
        ),
        FeatureHighlight(
            title = "Zoom with filters",
            description = "Budget spooked you? Home filters isolate one category fast.",
            icon = Icons.Rounded.Category,
            tone = Tone.Tertiary
        )
    )
)
