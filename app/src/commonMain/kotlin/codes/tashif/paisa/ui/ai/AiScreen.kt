package codes.tashif.paisa.ui.ai

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import codes.tashif.paisa.ai.ladder.AiService

/**
 * The AI feature: the overview, plus a provider page and a ladder page that
 * open on top of it. Pages are kept as a short route string so they survive
 * rotation; back goes to the overview, then closes.
 */
@Composable
fun AiScreen(onBack: () -> Unit) {
    var route by rememberSaveable { mutableStateOf(AiRoute.OVERVIEW) }
    val toOverview = { route = AiRoute.OVERVIEW }
    PlatformBackHandler(onBack = { if (route == AiRoute.OVERVIEW) onBack() else toOverview() })

    when {
        route.startsWith(AiRoute.PROVIDER) -> AiProviderScreen(
            providerId = route.removePrefix(AiRoute.PROVIDER),
            onBack = toOverview
        )
        route.startsWith(AiRoute.LADDER) -> AiLadderScreen(
            service = AiService.fromId(route.removePrefix(AiRoute.LADDER)),
            onBack = toOverview
        )
        else -> AiOverviewScreen(
            onBack = onBack,
            onOpenProvider = { route = AiRoute.PROVIDER + it },
            onOpenLadder = { route = AiRoute.LADDER + (it?.id ?: AiRoute.DEFAULT) }
        )
    }
}

private object AiRoute {
    const val OVERVIEW = "overview"
    const val PROVIDER = "provider:"
    const val LADDER = "ladder:"
    const val DEFAULT = "default"
}
