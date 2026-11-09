package codes.tashif.paisa.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = MaterialTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(MaterialTheme.spacing.small))

        ExpressiveHero(
            icon = page.icon,
            tone = page.heroTone,
            pageIndex = pageIndex
        )

        Spacer(Modifier.height(MaterialTheme.spacing.medium))

        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 4.dp,
                bottomStart = 4.dp,
                bottomEnd = 12.dp
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = page.eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.smaller)
        )

        if (page.highlights.isNotEmpty()) {
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                page.highlights.forEachIndexed { index, highlight ->
                    FeatureHighlightCard(
                        highlight = highlight,
                        shapeIndex = index + pageIndex
                    )
                }
            }
        }

        if (page.tips.isNotEmpty()) {
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            TipsCard(tips = page.tips)
        }

        Spacer(Modifier.height(MaterialTheme.spacing.small))
    }
}
