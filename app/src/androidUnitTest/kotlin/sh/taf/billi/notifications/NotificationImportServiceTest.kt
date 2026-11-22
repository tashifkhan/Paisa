package sh.taf.billi.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pennywiseai.parser.notification.NotificationApps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The app list and the user-added apps survive a new service instance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationImportServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageName = "com.example.custombank"

    @Before
    fun setUp() {
        NotificationApps.customApps.forEach { NotificationApps.removeCustom(it.packageName) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        NotificationApps.removeCustom(packageName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun canaraIsBuiltIn() {
        val canara = NotificationApps.find("com.canarabank.mobility")
        assertEquals("Canara ai1", canara?.name)
        assertTrue(canara?.custom == false)
    }

    @Test
    fun aUserAddedAppIsStoredAndRestored() {
        val service = AndroidNotificationImportService(context)
        service.setEnabled(true)
        service.addApp(packageName, "Example Bank", isBank = true)

        val added = service.state.value.apps.first { it.packageName == packageName }
        assertEquals("Example Bank", added.name)
        assertTrue(added.isBank)
        assertTrue(added.custom)
        assertTrue(added.enabled)
        assertTrue(service.isReading(packageName))

        val restored = AndroidNotificationImportService(context)
        val again = restored.state.value.apps.first { it.packageName == packageName }
        assertTrue(again.custom)
        assertTrue(again.isBank)
        assertTrue(restored.isReading(packageName))
    }

    @Test
    fun aRemovedAppStopsBeingRead() {
        val service = AndroidNotificationImportService(context)
        service.setEnabled(true)
        service.addApp(packageName, "Example Bank", isBank = false)
        assertTrue(service.isReading(packageName))

        service.removeApp(packageName)
        assertFalse(service.isReading(packageName))
        assertTrue(service.state.value.apps.none { it.packageName == packageName })

        val restored = AndroidNotificationImportService(context)
        assertTrue(restored.state.value.apps.none { it.packageName == packageName })
    }

    @Test
    fun aDisabledAppIsNotReadUntilTurnedBackOn() {
        val service = AndroidNotificationImportService(context)
        service.setEnabled(true)
        service.addApp(packageName, "Example Bank", isBank = true)

        service.setAppEnabled(packageName, false)
        assertFalse(service.isReading(packageName))
        service.setAppEnabled(packageName, true)
        assertTrue(service.isReading(packageName))
    }

    private companion object {
        const val PREFS = "notification_import"
    }
}
