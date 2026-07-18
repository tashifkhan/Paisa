package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp

private data class CurrencyOption(
    val symbol: String,
    val code: String,
    val name: String
)

private val currencyOptions = listOf(
    CurrencyOption("₹", "INR", "Indian Rupee"),
    CurrencyOption("$", "USD", "US Dollar"),
    CurrencyOption("€", "EUR", "Euro"),
    CurrencyOption("£", "GBP", "British Pound"),
    CurrencyOption("¥", "JPY", "Japanese Yen"),
    CurrencyOption("د.إ", "AED", "UAE Dirham"),
    CurrencyOption("﷼", "SAR", "Saudi Riyal"),
    CurrencyOption("S$", "SGD", "Singapore Dollar"),
    CurrencyOption("A$", "AUD", "Australian Dollar"),
    CurrencyOption("C$", "CAD", "Canadian Dollar"),
    CurrencyOption("Fr", "CHF", "Swiss Franc"),
    CurrencyOption("₩", "KRW", "South Korean Won"),
    CurrencyOption("৳", "BDT", "Bangladeshi Taka"),
    CurrencyOption("₨", "PKR", "Pakistani Rupee"),
    CurrencyOption("रू", "NPR", "Nepalese Rupee")
)

@Composable
fun CurrencyScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Currency", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            val current = settings ?: return@Column

            Text(
                "Symbol shown next to every amount in the app. Existing " +
                    "transactions are not converted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.extraSmall,
                    bottom = MaterialTheme.spacing.medium
                )
            )

            val haptics = rememberHaptics()
            SettingsGroup {
                currencyOptions.forEachIndexed { index, option ->
                    val selected = current.currency == option.symbol
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupShape(groupPositionOf(index, currencyOptions.size)),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!selected) haptics.tick()
                                    viewModel.updateSettings(
                                        current.copy(currency = option.symbol)
                                    )
                                }
                                .padding(
                                    horizontal = MaterialTheme.spacing.medium,
                                    vertical = MaterialTheme.spacing.small
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Spacer(Modifier.size(MaterialTheme.spacing.small))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = option.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    if (!selected) haptics.tick()
                                    viewModel.updateSettings(
                                        current.copy(currency = option.symbol)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
