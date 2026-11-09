package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

internal fun providerSubtitle(provider: AiProvider): String = when (provider) {
    AiProvider.OPENAI_COMPATIBLE ->
        "OpenAI, Groq, OpenRouter, Ollama — any /v1 endpoint"
    else -> "Official ${provider.label} API"
}

/**
 * Provider list. Switching provider also swaps in its default base URL and model,
 * unless the user typed their own.
 */
@Composable
internal fun AiProviderPicker(
    provider: AiProvider,
    baseUrl: String,
    model: String,
    onProviderChange: (AiProvider) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val haptics = rememberHaptics()
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
                            onProviderChange(p)
                            if (baseUrl.isBlank() ||
                                AiProvider.entries.any { it.defaultBaseUrl == baseUrl }
                            ) {
                                onBaseUrlChange(p.defaultBaseUrl)
                            }
                            if (model.isBlank() ||
                                AiProvider.entries.any { it.defaultModel == model }
                            ) {
                                onModelChange(p.defaultModel)
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
}
