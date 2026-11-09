package codes.tashif.paisa.ui.ai

import codes.tashif.paisa.ui.components.AdaptiveSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BaseUrlSafety
import codes.tashif.paisa.ai.store.ProviderResolution
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Name, API style and base URL for a new provider. The URL goes through the
 * same safety check the repository applies, so the error shows under the field.
 * [onAdd] reports a later failure through its last argument.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddCustomProviderSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, style: ApiStyle, baseUrl: String, onError: (String) -> Unit) -> Unit
) {
    val haptics = rememberHaptics()
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(ApiStyle.OPENAI) }
    var baseUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AdaptiveSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.larger),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text("Add custom provider", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "A server such as LM Studio, vLLM, llama.cpp or a hosted proxy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("My server") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Text("API style", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                ApiStyle.customStyles.forEach { option ->
                    FilterChip(
                        selected = option == style,
                        onClick = {
                            haptics.tick()
                            style = option
                        },
                        label = { Text(option.label) }
                    )
                }
            }
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    error = null
                },
                label = { Text("Base URL") },
                placeholder = { Text(placeholderFor(style)) },
                isError = error != null,
                supportingText = {
                    Text(error ?: "https anywhere. Plain http only for this device or your local network.")
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val check = runCatching { BaseUrlSafety.check(ProviderResolution.normalize(baseUrl)) }
                    error = check.exceptionOrNull()?.message
                    if (error == null) {
                        haptics.confirm()
                        onAdd(name, style, baseUrl) { error = it }
                    } else {
                        haptics.reject()
                    }
                },
                enabled = baseUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add provider") }
        }
    }
}

private fun placeholderFor(style: ApiStyle): String = when (style) {
    ApiStyle.ANTHROPIC -> "https://proxy.example.com"
    ApiStyle.GEMINI -> "https://proxy.example.com/v1beta"
    else -> "http://192.168.1.20:1234/v1"
}
