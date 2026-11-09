package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/** What the connection card shows. The screen owns the drafts. */
data class ProviderKeyState(
    val key: String,
    val hasKey: Boolean,
    /** Null hides the base URL field. */
    val baseUrl: String?,
    val baseUrlHint: String,
    val baseUrlError: String?,
    val keyLabel: String,
    val testResult: AiTestResult?,
)

/** The key with show and hide, the base URL when it's editable, then Save, Test connection and Remove. */
@Composable
internal fun ProviderKeyCard(
    state: ProviderKeyState,
    onKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit
) {
    val haptics = rememberHaptics()
    var showKey by remember { mutableStateOf(false) }
    SettingsSectionLabel("Connection")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            OutlinedTextField(
                value = state.key,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(state.keyLabel) },
                supportingText = { Text(if (state.hasKey) "Key saved on this device, encrypted" else "Stays on this device, encrypted") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                // Password type turns off suggestions that corrupt keys.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        haptics.toggle(on = !showKey)
                        showKey = !showKey
                    }) {
                        Icon(
                            imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                }
            )
            if (state.baseUrl != null) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    isError = state.baseUrlError != null,
                    supportingText = { Text(state.baseUrlError ?: state.baseUrlHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                Button(
                    onClick = {
                        haptics.confirm()
                        onSave()
                    },
                    enabled = state.key.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                FilledTonalButton(
                    onClick = {
                        haptics.click()
                        onTest()
                    },
                    enabled = state.hasKey,
                    modifier = Modifier.weight(1f)
                ) { Text("Test connection") }
            }
            if (state.hasKey) {
                OutlinedButton(
                    onClick = {
                        haptics.reject()
                        onRemove()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove key", color = MaterialTheme.colorScheme.error) }
            }
            state.testResult?.let { AiResultCard(it) }
        }
    }
}
