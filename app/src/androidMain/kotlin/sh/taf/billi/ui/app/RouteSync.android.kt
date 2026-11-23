package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable

/** Android has no address bar. Deep links arrive through MainActivity's intents instead. */
@Composable
actual fun RouteSync(route: AppRoute, onNavigate: (AppRoute) -> Unit) = Unit
