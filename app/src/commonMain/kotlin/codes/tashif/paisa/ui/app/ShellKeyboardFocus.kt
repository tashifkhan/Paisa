package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalWindowInfo

/** Keeps shortcuts available without requesting focus while a focus target is detaching. */
@Composable
internal fun rememberShellKeyboardFocus(enabled: Boolean): Modifier {
    if (!enabled) return Modifier
    val requester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    // onFocusChanged also runs during removal. Requesting focus there can leave
    // Compose pointing at the detached shell when a lock screen replaces it.
    // This effect runs after attachment and is canceled when the shell leaves.
    LaunchedEffect(hasFocus, windowFocused) {
        if (!hasFocus && windowFocused) requester.requestFocus()
    }
    return Modifier
        .onFocusChanged { hasFocus = it.hasFocus }
        .focusRequester(requester)
        // No extra semantics, so accessibility still exposes the child controls.
        .focusTarget()
}
