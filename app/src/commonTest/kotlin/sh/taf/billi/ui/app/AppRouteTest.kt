package sh.taf.billi.ui.app

import sh.taf.billi.core.AppTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppRouteTest {
    @Test
    fun everyRouteReadsBackFromItsPath() {
        val routes = AppTab.entries.map { AppRoute(tab = it) } +
            FullScreen.entries.map { AppRoute(tab = AppTab.MORE, screen = it) } +
            AppRoute(transactionId = 42)
        for (route in routes) assertEquals(route, AppRoute.parse(route.path), route.path)
    }

    @Test
    fun homeIsTheEmptyPath() {
        assertEquals("", AppRoute().path)
        assertEquals(AppRoute(), AppRoute.parse("/"))
    }

    @Test
    fun slashesAroundThePathDontMatter() {
        assertEquals(AppRoute(tab = AppTab.MORE, screen = FullScreen.BACKUP), AppRoute.parse("/settings/backup/"))
    }

    @Test
    fun unknownPathsAreNull() {
        assertNull(AppRoute.parse("nope"))
        assertNull(AppRoute.parse("settings/nope"))
        assertNull(AppRoute.parse("transactions/abc"))
        assertNull(AppRoute.parse("analytics/extra/parts"))
    }

    @Test
    fun aTransactionWinsOverTheScreenUnderIt() {
        assertEquals("transactions/7", AppRoute(screen = FullScreen.AI, transactionId = 7).path)
    }
}
