package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * The start of an import: what it does, which model runs it, what files work,
 * and one button pinned to the bottom. Without a model the button sets one up
 * instead of sitting there disabled.
 */
@Composable
internal fun StatementIdleContent(
    providerLabel: String?,
    onOpenAiSettings: () -> Unit,
    onChooseFile: () -> Unit,
    canChooseFile: Boolean
) {
    val haptics = rememberHaptics()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.medium)
        ) {
            IntroHero()

            SettingsSectionLabel("AI model")
            SettingsGroup {
                SettingsItem(
                    title = providerLabel ?: "No AI model yet",
                    subtitle = if (providerLabel != null) {
                        "Reads your statements. Tap to change."
                    } else {
                        "Import needs a model with your own API key"
                    },
                    icon = Icons.Rounded.AutoAwesome,
                    position = groupPositionOf(0, 1),
                    iconContainerColor = if (providerLabel != null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    iconContentColor = if (providerLabel != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    onClick = onOpenAiSettings,
                    trailing = {
                        Text(
                            if (providerLabel != null) "Change" else "Set up",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            SettingsSectionLabel("Files that work")
            SettingsGroup {
                val formats = listOf(
                    Triple(Icons.Rounded.PictureAsPdf, "PDF statements", "Up to 15 MB. Scanned PDFs need a model that reads PDFs, like Gemini."),
                    Triple(Icons.Rounded.TableChart, "CSV and text exports", "The download your bank or UPI app gives you"),
                    Triple(Icons.Rounded.Image, "Screenshots and photos", "JPG, PNG or WebP up to 5 MB. Needs a model that reads images.")
                )
                formats.forEachIndexed { index, (icon, title, subtitle) ->
                    SettingsItem(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        position = groupPositionOf(index, formats.size),
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                Text(
                    "Files go straight from this phone to the AI provider you picked, and nowhere else.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pinned action: always in thumb reach, however long the page gets.
        Surface(color = MaterialTheme.colorScheme.surface) {
            Button(
                onClick = {
                    haptics.click()
                    if (canChooseFile) onChooseFile() else onOpenAiSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    if (canChooseFile) Icons.Rounded.UploadFile else Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                Text(
                    if (canChooseFile) "Choose files" else "Set up AI to import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Big icon, one-line promise, and how the flow goes. */
@Composable
private fun IntroHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            // Same lopsided-corner language as the Home balance cluster.
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 36.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Text(
            "Bring in a bank or UPI statement",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            "Pick one file or several. Paisa pulls out every transaction and shows them " +
                "to you first. Nothing is saved until you import.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
