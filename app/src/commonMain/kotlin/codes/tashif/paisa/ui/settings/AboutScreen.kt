package codes.tashif.paisa.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.app.AppPlatform
import codes.tashif.paisa.ui.app.appIconPainter
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.rowTone
import codes.tashif.paisa.ui.platform.PlatformBackHandler
import codes.tashif.paisa.ui.platform.isWebPlatform
import codes.tashif.paisa.ui.sms.smsImportSupported
import codes.tashif.paisa.ui.theme.spacing
import org.koin.compose.koinInject

private const val REPO = "https://github.com/tashifkhan/Paisa"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    val versionName = koinInject<AppPlatform>().versionName
    val uri = LocalUriHandler.current
    val open: (String) -> Unit = { runCatching { uri.openUri(it) } }

    PlatformBackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "About", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            AppIdentity(versionName)

            SettingsSectionLabel("Where your data goes")
            val privacy = buildList {
                add(Triple(Icons.Rounded.Storage, "On this device", if (isWebPlatform) {
                    "Transactions live in this browser's private storage. No account, no analytics."
                } else {
                    "Transactions live in a database on this phone. No account, no analytics."
                }))
                if (smsImportSupported) {
                    add(Triple(Icons.Rounded.Sms, "Bank SMS", "Read and parsed on the phone. The messages never leave it."))
                }
                add(Triple(Icons.Rounded.CloudSync, "Backup and sync", "Only to the Google Drive, Nextcloud or file you pick. AES-256-GCM encrypted when you set a password."))
                add(Triple(Icons.Rounded.AutoAwesome, "AI", "Off until you add a key. Only what you send, like a note, receipt or statement, goes to the provider you chose."))
            }
            SettingsGroup {
                privacy.forEachIndexed { i, (icon, title, body) ->
                    SettingsItem(
                        title = title,
                        subtitle = body,
                        icon = icon,
                        position = groupPositionOf(i, privacy.size),
                        iconContainerColor = rowTone(i).container,
                        iconContentColor = rowTone(i).content
                    )
                }
            }

            SettingsSectionLabel("Help")
            SettingsGroup {
                SettingsItem(
                    title = "Run setup again",
                    subtitle = "Currency, theme and SMS access, then shortcuts to get going",
                    icon = Icons.Rounded.Replay,
                    position = groupPositionOf(0, 2),
                    iconContainerColor = rowTone(0).container,
                    iconContentColor = rowTone(0).content,
                    onClick = onReplayOnboarding
                )
                SettingsItem(
                    title = "Report a problem",
                    subtitle = "Open an issue on GitHub",
                    icon = Icons.Rounded.BugReport,
                    position = groupPositionOf(1, 2),
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { open("$REPO/issues") },
                    trailing = { ExternalIcon() }
                )
            }

            SettingsSectionLabel("Project")
            val project = listOf(
                LinkRow(Icons.Rounded.Code, "Source code", "github.com/tashifkhan/Paisa", REPO),
                LinkRow(Icons.Rounded.SystemUpdate, "Updates", "Releases on GitHub, or the F-Droid repo", "$REPO/releases/latest"),
                LinkRow(Icons.Rounded.Gavel, "License", "AGPL-3.0", "$REPO/blob/master/LICENSE"),
                LinkRow(
                    Icons.Rounded.Handshake,
                    "Bank SMS parsers",
                    "From PennyWise AI Tracker by sarim2000",
                    "https://github.com/sarim2000/pennywiseai-tracker"
                )
            )
            SettingsGroup {
                project.forEachIndexed { i, row ->
                    SettingsItem(
                        title = row.title,
                        subtitle = row.subtitle,
                        icon = row.icon,
                        position = groupPositionOf(i, project.size),
                        // Offset by one so the group doesn't start on the same tone as Help.
                        iconContainerColor = rowTone(i + 1).container,
                        iconContentColor = rowTone(i + 1).content,
                        onClick = { open(row.url) },
                        trailing = { ExternalIcon() }
                    )
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))
            Text(
                text = "Built with Kotlin Multiplatform and Compose Multiplatform. " +
                    "One codebase for Android and the web.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
            )
        }
    }
}

private data class LinkRow(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val url: String
)

@Composable
private fun ExternalIcon() {
    Icon(
        Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = "Opens in the browser",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun AppIdentity(versionName: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.size(64.dp)) {
                Image(appIconPainter(), contentDescription = "Paisa app icon", modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.width(MaterialTheme.spacing.medium))
            Column(Modifier.weight(1f)) {
                Text(
                    "Paisa",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isWebPlatform) Icons.Rounded.Code else Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Version $versionName · ${if (isWebPlatform) "Web" else "Android"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    "An expense tracker that keeps your data with you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
