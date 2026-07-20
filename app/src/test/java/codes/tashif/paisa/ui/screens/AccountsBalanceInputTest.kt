package codes.tashif.paisa.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountsBalanceInputTest {
    @Test
    fun parsesSupportedBalances() {
        assertEquals(125.5, parseManualBalanceInput("125.50")!!, 0.0)
        assertEquals(-42.75, parseManualBalanceInput(" -42.75 ")!!, 0.0)
        assertEquals(0.0, parseManualBalanceInput("0")!!, 0.0)
    }

    @Test
    fun rejectsIncompleteInvalidAndNonFiniteBalances() {
        listOf("", "-", "abc", "NaN", "Infinity", "-Infinity").forEach {
            assertNull(it, parseManualBalanceInput(it))
        }
    }

    @Test
    fun formatsStoredBalanceForEditingWithoutScientificNotation() {
        assertEquals("125.5", manualBalanceInputText(125.5))
        assertEquals("0", manualBalanceInputText(0.0))
        assertEquals("-42", manualBalanceInputText(-42.0))
    }
}
