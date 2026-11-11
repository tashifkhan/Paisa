package codes.tashif.paisa.ui.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollbarFor(state: LazyGridState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier) = Unit

actual fun setWindowTitle(title: String) = Unit

@Composable
actual fun PageKeys(onKey: (key: String, command: Boolean) -> Boolean) = Unit
