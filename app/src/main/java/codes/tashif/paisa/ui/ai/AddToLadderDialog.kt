package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.UploadFile

/**
 * Asks which ladder a model goes on: Default or one of the services. A
 * service that needs images is turned off for a model known not to read them.
 */
@Composable
internal fun AddToLadderDialog(
    model: ModelInfo,
    onPick: (AiService?) -> Unit,
    onDismiss: () -> Unit
) {
    val targets = ladderTargets()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${model.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                Text("Which ladder should it go on? It's added at the bottom.")
                SettingsGroup {
                    targets.forEachIndexed { index, service ->
                        val blocked = service?.needsImage == true && model.acceptsImage == false
                        SettingsItem(
                            title = ladderTitle(service),
                            subtitle = when {
                                blocked -> "This model doesn't read images"
                                service == null -> "Used by every service without its own"
                                else -> service.description
                            },
                            icon = when {
                                service == null -> Icons.Rounded.Layers
                                service.needsImage -> Icons.Rounded.Image
                                else -> Icons.Rounded.UploadFile
                            },
                            position = groupPositionOf(index, targets.size),
                            onClick = if (blocked) null else ({ onPick(service) })
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
