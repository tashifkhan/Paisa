package sh.taf.billi.assistant

import android.content.Context
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.testing.AppFunctionTestRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Calls Billi the way Gemini does: finds the functions through
 * AppFunctionManager, builds parameters from their metadata, and reads the
 * typed response. Runs the generated BilliAppFunctionService, so it also
 * checks the compiler output and the XML it describes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BilliAppFunctionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val rule = AppFunctionTestRule(context)

    private val manager get() = rule.getAppFunctionManager()

    @After
    fun turnAccessBackOn() {
        GlobalContext.get().get<AssistantAccess>().setEnabled(true)
    }

    private suspend fun function(name: String): AppFunctionMetadata =
        manager.searchAppFunctions(AppFunctionSearchSpec(packageNames = setOf(context.packageName)))
            .single { it.id.endsWith("#$name") }

    private suspend fun call(name: String, fill: AppFunctionData.Builder.() -> Unit = {}): ExecuteAppFunctionResponse {
        val meta = function(name)
        val params = AppFunctionData.Builder(meta.parameters, meta.components).apply(fill).build()
        return manager.executeAppFunction(ExecuteAppFunctionRequest(context.packageName, meta.id, params))
    }

    private fun ExecuteAppFunctionResponse.value(): AppFunctionData {
        if (this is ExecuteAppFunctionResponse.Error) throw error
        return (this as ExecuteAppFunctionResponse.Success).returnValue
            .getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)!!
    }

    @Test
    fun exposesTheFourActions(): Unit = runBlocking {
        val ids = manager.searchAppFunctions(AppFunctionSearchSpec(packageNames = setOf(context.packageName)))
            .map { it.id.substringAfter('#') }
            .toSet()
        assertEquals(setOf("addTransaction", "getSpendingSummary", "getBudgets", "getRecentTransactions"), ids)
    }

    @Test
    fun addsATransactionAndReadsItBack(): Unit = runBlocking {
        val saved = call("addTransaction") {
            setDouble("amount", 240.0)
            setString("title", "Chaayos")
            setString("category", "Food & Dining")
        }.value()
        assertEquals("Food & Dining", saved.getString("category"))
        assertEquals("expense", saved.getString("type"))

        val recent = call("getRecentTransactions") { setInt("limit", 5) }.value()
        val first = recent.getAppFunctionDataList("transactions")!!.first()
        assertEquals("Chaayos", first.getString("title"))
        assertEquals(240.0, first.getDouble("amount"), 0.001)
        assertEquals("assistant", first.getString("source"))

        val summary = call("getSpendingSummary").value()
        assertTrue(summary.getDouble("expense") >= 240.0)
    }

    @Test
    fun badInputComesBackAsAnInvalidArgument(): Unit = runBlocking {
        val error = (call("addTransaction") { setDouble("amount", -5.0) } as ExecuteAppFunctionResponse.Error).error
        assertTrue(error is AppFunctionInvalidArgumentException)
        assertTrue(error.errorMessage!!.contains("above zero"))
    }

    @Test
    fun theSwitchUnderPrivacyRefusesEveryCall(): Unit = runBlocking {
        GlobalContext.get().get<AssistantAccess>().setEnabled(false)
        val error = (call("getBudgets") as ExecuteAppFunctionResponse.Error).error
        assertTrue(error is AppFunctionDeniedException)
    }
}
