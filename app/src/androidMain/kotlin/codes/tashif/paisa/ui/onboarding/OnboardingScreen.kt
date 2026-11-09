package codes.tashif.paisa.ui.onboarding

import androidx.activity.compose.BackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.onboarding.pages.onboardingPages
import codes.tashif.paisa.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * First-run Material You feature tour — multi-shape, expressive, and a little playful.
 * Shape language inspired by [Material Design 3](https://m3.material.io/) expressive UI:
 * varied corner radii, tonal containers, and springy motion.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit = {},
    onRestoreBackup: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    // Marks onboarding done in settings, then lets the host react if it wants to.
    val finish = {
        viewModel.completeOnboarding()
        onFinished()
    }
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
                    if (onRestoreBackup != null && pagerState.currentPage == 0) {
                        TextButton(
                            onClick = {
                                haptics.click()
                                onRestoreBackup()
                            }
                        ) {
                            Text(
                                text = "Restore backup",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    if (!isLast) {
                        TextButton(
                            onClick = {
                                haptics.click()
                                finish()
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
                                finish()
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
