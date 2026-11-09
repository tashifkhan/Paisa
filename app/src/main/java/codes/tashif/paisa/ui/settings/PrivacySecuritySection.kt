package codes.tashif.paisa.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.security.BiometricAuth
import codes.tashif.paisa.security.BiometricStatus
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * Hide balances and app lock toggles. Turning the lock on asks for biometrics
 * first; [onShowMessage] gets the success and error text for a snackbar.
 */
@Composable
fun PrivacySecuritySection(
    hideByDefault: Boolean,
    biometricEnabled: Boolean,
    onSetHideBalancesByDefault: (Boolean) -> Unit,
    onSetBiometricLockEnabled: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val biometricStatus = remember(context) { BiometricAuth.status(context) }
    val biometricAvailable = biometricStatus == BiometricStatus.Available
    val activity = context as? FragmentActivity

    fun enableWithBiometrics(activity: FragmentActivity) {
        BiometricAuth.authenticate(
            activity = activity,
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
            subtitle = "Home amounts start hidden until you reveal them",
            icon = Icons.Rounded.VisibilityOff,
            position = groupPositionOf(0, 2),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                haptics.toggle(on = !hideByDefault)
                onSetHideBalancesByDefault(!hideByDefault)
            },
            trailing = {
                Switch(
                    checked = hideByDefault,
                    onCheckedChange = {
                        haptics.toggle(on = it)
                        onSetHideBalancesByDefault(it)
                    }
                )
            }
        )
        SettingsItem(
            title = "App lock",
            subtitle = if (biometricAvailable) {
                if (biometricEnabled) {
                    "Require biometrics when opening Paisa"
                } else {
                    BiometricAuth.statusMessage(biometricStatus)
                }
            } else {
                BiometricAuth.statusMessage(biometricStatus)
            },
            icon = if (biometricEnabled) Icons.Rounded.Lock else Icons.Rounded.Fingerprint,
            position = groupPositionOf(1, 2),
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
                    } else if (activity != null) {
                        enableWithBiometrics(activity)
                    }
                }
            } else {
                {
                    onShowMessage(BiometricAuth.statusMessage(biometricStatus))
                }
            },
            trailing = {
                Switch(
                    checked = biometricEnabled,
                    enabled = biometricAvailable,
                    onCheckedChange = { enabled ->
                        if (!biometricAvailable) {
                            onShowMessage(BiometricAuth.statusMessage(biometricStatus))
                            return@Switch
                        }
                        if (!enabled) {
                            haptics.toggle(on = false)
                            onSetBiometricLockEnabled(false)
                            return@Switch
                        }
                        if (activity == null) return@Switch
                        enableWithBiometrics(activity)
                    }
                )
            }
        )
    }
}
