package codes.tashif.paisa.ui.app

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * The tabs in the bottom bar as pages you can swipe between. Tapping a tab
 * jumps straight to it, without sliding through the ones in between. Lists
 * and carousels that scroll sideways keep their swipe until they reach an end.
 */
@Composable
internal fun SwipeableTabs(
    navTabs: List<AppTab>,
    currentTab: AppTab,
    onSwipeTo: (AppTab) -> Unit,
    page: @Composable (AppTab) -> Unit
) {
    val haptics = rememberHaptics()
    val pagerState = rememberPagerState(
        initialPage = navTabs.indexOf(currentTab).coerceAtLeast(0)
    ) { navTabs.size }
    val latestTab by rememberUpdatedState(currentTab)
    val latestTabs by rememberUpdatedState(navTabs)
    val latestOnSwipe by rememberUpdatedState(onSwipeTo)

    // A tap in the bottom bar (or a shortcut) moves the pager.
    LaunchedEffect(currentTab, navTabs) {
        val target = navTabs.indexOf(currentTab)
        if (target >= 0 && target != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }
    // A swipe that settles on another page selects that tab.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            val tab = latestTabs.getOrNull(settled) ?: return@collect
            if (tab != latestTab) {
                haptics.tick()
                latestOnSwipe(tab)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { navTabs.getOrNull(it)?.name ?: it }
    ) { index ->
        navTabs.getOrNull(index)?.let { page(it) }
    }
}
