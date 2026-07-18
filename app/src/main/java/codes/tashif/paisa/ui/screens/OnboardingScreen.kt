package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * First-run Material You feature tour — multi-shape, expressive, and a little playful.
 * Shape language inspired by [Material Design 3](https://m3.material.io/) expressive UI:
 * varied corner radii, tonal containers, and springy motion.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pages = onboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val isLast = pagerState.currentPage == pages.lastIndex

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Soft ambient blobs (M3 color roles, multi-shape)
            AmbientBlobs(pageIndex = pagerState.currentPage)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        .padding(top = MaterialTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 4.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 12.dp
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} · ${pages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!isLast) {
                        TextButton(
                            onClick = {
                                haptics.click()
                                onFinished()
                            }
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
                    pageSpacing = MaterialTheme.spacing.small
                ) { page ->
                    OnboardingPageContent(
                        page = pages[page],
                        pageIndex = page
                    )
                }

                // Expressive pill indicators — morph width + shape
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val selected = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (selected) 32.dp else 10.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dot_w"
                        )
                        val height by animateDpAsState(
                            targetValue = if (selected) 10.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dot_h"
                        )
                        val color by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "dot_c"
                        )
                        // Alternate corner radii for a playful M3 shape scale
                        val corner = when (index % 3) {
                            0 -> 20.dp
                            1 -> 6.dp
                            else -> 12.dp
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(height)
                                .width(width)
                                .clip(RoundedCornerShape(corner))
                                .background(color)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.medium)
                        .padding(bottom = MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        FilledTonalButton(
                            onClick = {
                                haptics.tick()
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 12.dp,
                                bottomStart = 28.dp,
                                bottomEnd = 12.dp
                            )
                        ) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            haptics.click()
                            if (isLast) {
                                onFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(if (pagerState.currentPage > 0) 1.5f else 1f),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 28.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 28.dp
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            text = if (isLast) "Let’s go ✨" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.extraSmall))
                        Icon(
                            imageVector = if (isLast) {
                                Icons.Rounded.RocketLaunch
                            } else {
                                Icons.AutoMirrored.Rounded.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientBlobs(pageIndex: Int) {
    val primary = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val secondary = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f)
    // Rotate which blob is “loudest” per page for motion without heavy animation
    val rot by animateFloatAsState(
        targetValue = pageIndex * 12f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "blob_rot"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-28).dp, y = 56.dp)
                .size(110.dp)
                .rotate(rot)
                .clip(
                    RoundedCornerShape(
                        topStart = 56.dp,
                        topEnd = 28.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 56.dp
                    )
                )
                .background(primary)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = 28.dp)
                .size(88.dp)
                .rotate(-rot * 0.6f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 44.dp,
                        bottomStart = 44.dp,
                        bottomEnd = 14.dp
                    )
                )
                .background(secondary)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = (-96).dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(tertiary)
        )
    }
}

@Composable
private fun OnboardingPageContent(
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

/**
 * Stacked multi-shape hero — cookie-cutter layers like M3 expressive demos.
 */
@Composable
private fun ExpressiveHero(
    icon: ImageVector,
    tone: Tone,
    pageIndex: Int
) {
    val outer = tone.container()
    val inner = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val accent = when (pageIndex % 3) {
        0 -> MaterialTheme.colorScheme.secondaryContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    // Compact multi-shape hero — closer to home-screen icon scale
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .offset(x = 6.dp, y = 5.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 16.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 32.dp
                    )
                )
                .background(accent.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 30.dp,
                        bottomStart = 26.dp,
                        bottomEnd = 14.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(outer, outer.copy(alpha = 0.75f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 10.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(inner),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone.content(),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 6.dp),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 3.dp,
                bottomStart = 3.dp,
                bottomEnd = 10.dp
            ),
            color = MaterialTheme.colorScheme.tertiary,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onTertiary)
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    highlight: FeatureHighlight,
    shapeIndex: Int
) {
    // Cycle through M3-inspired multi-corner tiles
    val shape: Shape = when (shapeIndex % 4) {
        0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 24.dp
        )
        1 -> RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 28.dp,
            bottomStart = 28.dp,
            bottomEnd = 12.dp
        )
        2 -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
    }
    val iconShape: Shape = when (shapeIndex % 3) {
        0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 6.dp,
            bottomStart = 6.dp,
            bottomEnd = 16.dp
        )
        1 -> CircleShape
        else -> RoundedCornerShape(14.dp)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(highlight.container()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    tint = highlight.content(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TipsCard(tips: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 28.dp,
            bottomStart = 28.dp,
            bottomEnd = 8.dp
        ),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Pro tip snack",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            tips.forEach { tip ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 2.dp,
                                    bottomStart = 2.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─── Content model ───────────────────────────────────────────────────────────

private enum class Tone {
    Primary, Secondary, Tertiary, Error, Surface
}

private data class FeatureHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tone: Tone = Tone.Primary
)

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val heroTone: Tone = Tone.Primary,
    val highlights: List<FeatureHighlight> = emptyList(),
    val tips: List<String> = emptyList()
)

@Composable
private fun Tone.container(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.primaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.secondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.Error -> MaterialTheme.colorScheme.errorContainer
    Tone.Surface -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
private fun Tone.content(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.Error -> MaterialTheme.colorScheme.onErrorContainer
    Tone.Surface -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun FeatureHighlight.container(): Color = tone.container()

@Composable
private fun FeatureHighlight.content(): Color = tone.content()

// Playful copy + full feature coverage
private val onboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage(
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
    ),
    OnboardingPage(
        eyebrow = "Privacy first",
        title = "Your data never leaves the building",
        body = "Everything lives in a local database on this phone. SMS is parsed on-device. " +
            "We don’t even have a place to put your money stories.",
        icon = Icons.Rounded.PrivacyTip,
        heroTone = Tone.Secondary,
        highlights = listOf(
            FeatureHighlight(
                title = "Room DB, home-cooked",
                description = "Accounts, txs, budgets — SQLite on your device. Backup? " +
                    "That’s your business (and Export later).",
                icon = Icons.Rounded.Lock,
                tone = Tone.Primary
            ),
            FeatureHighlight(
                title = "SMS stays in the pocket",
                description = "~140 bank parsers run offline. Your inbox is not a product " +
                    "feature for someone else.",
                icon = Icons.Rounded.Sms,
                tone = Tone.Secondary
            ),
            FeatureHighlight(
                title = "AI is BYOK (bring your own key)",
                description = "Statement magic only if you plug in OpenAI-compatible, Gemini, " +
                    "or Anthropic. Your key, your endpoint.",
                icon = Icons.Rounded.AutoAwesome,
                tone = Tone.Tertiary
            )
        ),
        tips = listOf("Re-read the privacy love letters anytime in More → About.")
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
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
    ),
    OnboardingPage(
        eyebrow = "You vibes",
        title = "Lock it. Hide it. Theme it.",
        body = "Biometrics, default-hidden balances, Material You colors, AMOLED black, " +
            "and springy expressive motion — all on brand with m3.material.io energy.",
        icon = Icons.Rounded.Fingerprint,
        heroTone = Tone.Error,
        highlights = listOf(
            FeatureHighlight(
                title = "App lock",
                description = "Fingerprint / face / device PIN when opening. Relocks when " +
                    "you bounce to another app.",
                icon = Icons.Rounded.Fingerprint,
                tone = Tone.Error
            ),
            FeatureHighlight(
                title = "Hide by default",
                description = "Shoulder-surfers see dots. You see the eye on the balance tile.",
                icon = Icons.Rounded.VisibilityOff,
                tone = Tone.Secondary
            ),
            FeatureHighlight(
                title = "Appearance playground",
                description = "System/light/dark · dynamic wallpaper colors · fixed palettes · " +
                    "AMOLED · expressive motion.",
                icon = Icons.Rounded.Palette,
                tone = Tone.Tertiary
            )
        ),
        tips = listOf("Miss this tour? More → About → Feature tour. Instant encore.")
    ),
    OnboardingPage(
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
)
