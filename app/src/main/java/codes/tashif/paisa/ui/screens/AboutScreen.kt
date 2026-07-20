package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing
import codes.tashif.paisa.R

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
        }.getOrNull() ?: "1.0.0"
    }

    BackHandler(onBack = onBack)

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
            // App identity card
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_legacy),
                        contentDescription = "Paisa app icon",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "Paisa",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSectionLabel("Privacy")
            SettingsGroup {
                SettingsItem(
                    title = "Offline-first",
                    subtitle = "All data stays in a local database on your phone. " +
                        "No account, no sync, no analytics.",
                    icon = Icons.Rounded.Lock,
                    position = groupPositionOf(0, 3),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                SettingsItem(
                    title = "SMS parsed on-device",
                    subtitle = "Bank SMS never leave your phone. Parsing runs " +
                        "entirely locally.",
                    icon = Icons.Rounded.Sms,
                    position = groupPositionOf(1, 3),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                SettingsItem(
                    title = "Optional BYOK AI",
                    subtitle = "Statement import uses your own API key. Only files " +
                        "you upload are sent to the endpoint you choose.",
                    icon = Icons.Rounded.AutoAwesome,
                    position = groupPositionOf(2, 3),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            SettingsSectionLabel("Help")
            SettingsGroup {
                SettingsItem(
                    title = "Feature tour",
                    subtitle = "Replay the onboarding walkthrough of every Paisa feature",
                    icon = Icons.Rounded.Tour,
                    position = groupPositionOf(0, 1),
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onReplayOnboarding
                )
            }

            SettingsSectionLabel("Project")
            SettingsGroup {
                SettingsItem(
                    title = "Open source",
                    subtitle = "Built with Kotlin, Jetpack Compose, and Room",
                    icon = Icons.Rounded.Code,
                    position = groupPositionOf(0, 2),
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                SettingsItem(
                    title = "License",
                    subtitle = "AGPL-3.0",
                    icon = Icons.Rounded.Gavel,
                    position = groupPositionOf(1, 2),
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
