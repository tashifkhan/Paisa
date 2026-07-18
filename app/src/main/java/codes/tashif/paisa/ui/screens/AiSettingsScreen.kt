package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

private fun providerSubtitle(provider: AiProvider): String = when (provider) {
    AiProvider.OPENAI_COMPATIBLE ->
        "OpenAI, Groq, OpenRouter, Ollama — any /v1 endpoint"
    else -> "Official ${provider.label} API"
}

@Composable
fun AiSettingsScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val saved by viewModel.aiCredentials.collectAsState()
    val testResult by viewModel.aiTestResult.collectAsState()

    var provider by remember(saved.provider) { mutableStateOf(saved.provider) }
    val haptics = rememberHaptics()
    var apiKey by remember(saved.apiKey) { mutableStateOf(saved.apiKey) }
    var baseUrl by remember(saved.baseUrl) { mutableStateOf(saved.baseUrl) }
    var model by remember(saved.model) { mutableStateOf(saved.model) }
    var showKey by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "AI provider", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            // Privacy banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Bring your own key for optional statement import. SMS stays " +
                            "100% on-device — only files you upload are sent to the " +
                            "endpoint you choose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Provider picker
            SettingsSectionLabel("Provider")
            SettingsGroup {
                val providers = AiProvider.entries
                providers.forEachIndexed { index, p ->
                    val selected = provider == p
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupShape(groupPositionOf(index, providers.size)),
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (provider != p) haptics.tick()
                                    provider = p
                                    if (baseUrl.isBlank() ||
                                        AiProvider.entries.any { it.defaultBaseUrl == baseUrl }
                                    ) {
                                        baseUrl = p.defaultBaseUrl
                                    }
                                    if (model.isBlank() ||
                                        AiProvider.entries.any { it.defaultModel == model }
                                    ) {
                                        model = p.defaultModel
                                    }
                                }
                                .padding(
                                    horizontal = MaterialTheme.spacing.medium,
                                    vertical = MaterialTheme.spacing.small
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = providerSubtitle(p),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        }
                    }
                }
            }

            // Connection details
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
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API key") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        // Password type disables autocorrect/suggestions that corrupt keys.
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                haptics.toggle(on = !showKey)
                                showKey = !showKey
                            }) {
                                Icon(
                                    imageVector = if (showKey) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (showKey) {
                                        "Hide key"
                                    } else {
                                        "Show key"
                                    }
                                )
                            }
                        }
                    )

                    if (provider.requiresBaseUrl) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Base URL") },
                            supportingText = { Text("Endpoint ending in /v1") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model") },
                        supportingText = { Text("Default: ${provider.defaultModel}") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }

            // Test result
            testResult?.let { result ->
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (result.isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(
                            MaterialTheme.spacing.small
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val contentColor = if (result.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        }
                        Icon(
                            imageVector = if (result.isError) {
                                Icons.Rounded.ErrorOutline
                            } else {
                                Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            tint = contentColor
                        )
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            val draft = AiCredentials(
                provider = provider,
                apiKey = apiKey,
                baseUrl = baseUrl.ifBlank { provider.defaultBaseUrl },
                model = model.ifBlank { provider.defaultModel }
            )

            Button(
                onClick = {
                    haptics.confirm()
                    viewModel.saveAiCredentials(draft)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = draft.apiKey.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            FilledTonalButton(
                onClick = {
                    haptics.click()
                    viewModel.testAiConnection(draft)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = draft.isConfigured
            ) {
                Text("Test connection", fontWeight = FontWeight.SemiBold)
            }
            if (saved.apiKey.isNotBlank()) {
                Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                TextButton(
                    onClick = {
                        haptics.reject()
                        viewModel.clearAiCredentials()
                        apiKey = ""
                        viewModel.clearAiTestResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                    Text("Clear key", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
