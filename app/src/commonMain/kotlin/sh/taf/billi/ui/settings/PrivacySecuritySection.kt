package sh.taf.billi.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import sh.taf.billi.ui.components.SettingsGroup
import sh.taf.billi.ui.components.SettingsItem
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.components.groupPositionOf
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.lock.authenticateToShowBalances
import sh.taf.billi.ui.lock.rememberBiometrics
import sh.taf.billi.assistant.AssistantAccess
import org.koin.compose.koinInject

@Composable
fun PrivacySecuritySection(
    hideByDefault: Boolean,
    biometricEnabled: Boolean,
    onSetHideBalancesByDefault: (Boolean) -> Unit,
    onSetBiometricLockEnabled: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = rememberHaptics()
    val biometrics = rememberBiometrics()
    val biometricAvailable = biometrics.available
    val assistant = koinInject<AssistantAccess>()
    val assistantEnabled by assistant.enabled.collectAsState()
    val rows = if (assistant.supported) 3 else 2

    fun changeHideByDefault(hide: Boolean) {
        if (hide) {
            haptics.toggle(on = true)
            onSetHideBalancesByDefault(true)
        } else {
            biometrics.authenticateToShowBalances(
                onSuccess = {
                    haptics.toggle(on = false)
                    onSetHideBalancesByDefault(false)
                },
                onError = onShowMessage
            )
        }
    }

    fun enableWithBiometrics() {
        biometrics.authenticate(
            title = "Enable app lock",
            subtitle = "Confirm it's you to require unlock next time",
            onSuccess = {
                haptics.toggle(on = true)
                onSetBiometricLockEnabled(true)
                onShowMessage("App lock enabled")
            },
            onError = { msg ->
                onShowMessage(msg)
            }
        )
    }

    SettingsSectionLabel("Privacy & security")
    SettingsGroup {
        SettingsItem(
            title = "Hide balances by default",
            subtitle = if (biometrics.supported) {
                "Home amounts start hidden. Authenticate to reveal them."
            } else {
                "Home amounts start hidden until you reveal them"
            },
            icon = Icons.Rounded.VisibilityOff,
            position = groupPositionOf(0, rows),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { changeHideByDefault(!hideByDefault) },
            trailing = {
                Switch(
                    checked = hideByDefault,
                    onCheckedChange = ::changeHideByDefault
                )
            }
        )
        SettingsItem(
            title = "App lock",
            subtitle = if (biometricAvailable) {
                if (biometricEnabled) {
                    "Require biometrics when opening Billi"
                } else {
                    biometrics.statusMessage
                }
            } else {
                biometrics.statusMessage
            },
            icon = if (biometricEnabled) Icons.Rounded.Lock else Icons.Rounded.Fingerprint,
            position = groupPositionOf(1, rows),
            iconContainerColor = if (biometricEnabled) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
            iconContentColor = if (biometricEnabled) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
            onClick = if (biometricAvailable) {
                {
                    if (biometricEnabled) {
                        haptics.toggle(on = false)
                        onSetBiometricLockEnabled(false)
                    } else {
                        enableWithBiometrics()
                    }
                }
            } else {
                {
                    onShowMessage(biometrics.statusMessage)
                }
            },
            trailing = {
                Switch(
                    checked = biometricEnabled,
                    enabled = biometricAvailable,
                    onCheckedChange = { enabled ->
                        if (!biometricAvailable) {
                            onShowMessage(biometrics.statusMessage)
                            return@Switch
                        }
                        if (!enabled) {
                            haptics.toggle(on = false)
                            onSetBiometricLockEnabled(false)
                            return@Switch
                        }
                        enableWithBiometrics()
                    }
                )
            }
        )
        if (assistant.supported) {
            SettingsItem(
                title = "Gemini and assistants",
                subtitle = if (assistantEnabled) {
                    "Connected assistants can add transactions and read finance data, even with app lock on. Gemini requires Google Connected Apps."
                } else {
                    "Off. Assistants can't add or read anything in Billi"
                },
                icon = Icons.Rounded.AutoAwesome,
                position = groupPositionOf(2, rows),
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    haptics.toggle(on = !assistantEnabled)
                    assistant.setEnabled(!assistantEnabled)
                },
                trailing = {
                    Switch(
                        checked = assistantEnabled,
                        onCheckedChange = {
                            haptics.toggle(on = it)
                            assistant.setEnabled(it)
                        }
                    )
                }
            )
        }
    }
}
