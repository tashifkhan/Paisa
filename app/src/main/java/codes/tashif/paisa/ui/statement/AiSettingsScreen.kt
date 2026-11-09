package codes.tashif.paisa.ui.statement

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val saved by viewModel.aiCredentials.collectAsState()
    val testResult by viewModel.aiTestResult.collectAsState()

    var provider by remember(saved.provider) { mutableStateOf(saved.provider) }
    val haptics = rememberHaptics()
    var apiKey by remember(saved.apiKey) { mutableStateOf(saved.apiKey) }
    var baseUrl by remember(saved.baseUrl) { mutableStateOf(saved.baseUrl) }
    var model by remember(saved.model) { mutableStateOf(saved.model) }

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

            AiProviderPicker(
                provider = provider,
                baseUrl = baseUrl,
                model = model,
                onProviderChange = { provider = it },
                onBaseUrlChange = { baseUrl = it },
                onModelChange = { model = it }
            )

            AiConnectionCard(
                provider = provider,
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                baseUrl = baseUrl,
                onBaseUrlChange = { baseUrl = it },
                model = model,
                onModelChange = { model = it }
            )

            testResult?.let { result ->
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                AiTestResultCard(result)
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
