package sh.taf.billi.ui.focus

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import sh.taf.billi.ui.app.rememberShellKeyboardFocus
import sh.taf.billi.ui.components.markdown.MarkdownField
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun formattingMovesFocusFromTitleToDescription() {
        var title by mutableStateOf("")
        var description by mutableStateOf("")
        compose.setContent {
            MaterialTheme {
                Column {
                    BasicTextField(title, { title = it }, Modifier.testTag("title"))
                    MarkdownField(description, { description = it }, "Description", "Add a note", alwaysOpen = true)
                }
            }
        }
        compose.onNodeWithTag("title").performClick().performTextInput("Dinner")
        compose.onNodeWithContentDescription("Bold").performClick()
        // Send a character to the focused field, as a hardware keyboard would.
        compose.onNode(isFocused()).performTextInput("note")
        compose.onNodeWithTag("title").assertTextEquals("Dinner")
        compose.runOnIdle { assertEquals("**note**", description) }
    }

    @Test
    fun removingFocusedShellAllowsNextScreenToTakeFocus() {
        var showShell by mutableStateOf(true)
        var shellFocused = false
        val nextFocus = FocusRequester()
        compose.setContent {
            Column {
                if (showShell) Box(
                    Modifier.size(40.dp)
                        .onFocusChanged { shellFocused = it.isFocused }
                        .then(rememberShellKeyboardFocus(true))
                )
                Box(Modifier.size(40.dp).testTag("next").focusRequester(nextFocus).focusable())
            }
        }
        compose.runOnIdle {
            assertTrue("Shell must hold focus before it is removed", shellFocused)
            showShell = false
        }
        compose.runOnIdle { nextFocus.requestFocus() }
        compose.onNodeWithTag("next").assertIsFocused()
    }

    @Test
    fun shellDoesNotStealFocusFromTextFieldOnWindowReturn() {
        var windowFocused by mutableStateOf(true)
        val window = object : WindowInfo {
            override val isWindowFocused get() = windowFocused
        }
        var text by mutableStateOf("")
        compose.setContent {
            CompositionLocalProvider(LocalWindowInfo provides window) {
                Column(rememberShellKeyboardFocus(true)) {
                    BasicTextField(text, { text = it }, Modifier.testTag("input"))
                }
            }
        }
        compose.onNodeWithTag("input").performClick().performTextInput("Keep typing")
        compose.runOnIdle { windowFocused = false }
        compose.runOnIdle { windowFocused = true }
        compose.onNodeWithTag("input").assertIsFocused().performTextInput(" here")
        compose.onNodeWithTag("input").assertTextEquals("Keep typing here")
    }

    @Test
    fun shellDoesNotReclaimFocusWhileWindowIsInactive() {
        var windowFocused by mutableStateOf(true)
        val window = object : WindowInfo {
            override val isWindowFocused get() = windowFocused
        }
        var shellFocused = false
        val otherFocus = FocusRequester()
        compose.setContent {
            CompositionLocalProvider(LocalWindowInfo provides window) {
                Column {
                    Box(
                        Modifier.size(40.dp)
                            .onFocusChanged { shellFocused = it.isFocused }
                            .then(rememberShellKeyboardFocus(true))
                    )
                    Box(Modifier.size(40.dp).testTag("other").focusRequester(otherFocus).focusable())
                }
            }
        }
        compose.runOnIdle {
            assertTrue(shellFocused)
            windowFocused = false
        }
        compose.runOnIdle { otherFocus.requestFocus() }
        compose.onNodeWithTag("other").assertIsFocused()
        compose.runOnIdle {
            assertFalse("Do not reclaim focus behind the system dialog", shellFocused)
            windowFocused = true
        }
        compose.runOnIdle { assertTrue("Shortcuts need focus after return", shellFocused) }
    }
}
