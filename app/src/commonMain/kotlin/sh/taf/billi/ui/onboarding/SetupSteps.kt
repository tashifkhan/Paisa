package sh.taf.billi.ui.onboarding

import androidx.compose.foundation.layout.offset
import sh.taf.billi.ui.components.BilliMood
import sh.taf.billi.ui.components.BilliMascot
import sh.taf.billi.ui.ai.LocalAiEnabled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.taf.billi.core.AppLockGuard
import sh.taf.billi.ui.app.AppDestinations
import sh.taf.billi.ui.platform.handCursor
import sh.taf.billi.ui.platform.isWebPlatform
import sh.taf.billi.ui.settings.PalettePicker
import sh.taf.billi.ui.settings.ThemeModePicker
import sh.taf.billi.ui.settings.currencyOptions
import sh.taf.billi.ui.sms.SmsPermissions
import sh.taf.billi.ui.sms.SmsViewModel
import sh.taf.billi.ui.sms.rememberSmsPermissions
import sh.taf.billi.ui.sms.smsImportSupported
import sh.taf.billi.ui.theme.ExpenseRed
import sh.taf.billi.ui.theme.spacing
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WelcomeStep() {
    Spacer(Modifier.height(MaterialTheme.spacing.large))
    BilliMascot(
        mood = BilliMood.Happy,
        size = 136.dp,
        animate = true,
        contentDescription = "Billi, a ginger cat who is also a wallet",
        modifier = Modifier.offset(x = (-12).dp)
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    Text(
        text = "Billi",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Budgeting and expense manager",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    Text(
        text = "Track what you spend without handing your money data to anyone.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))

    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
        WelcomePoint(
            icon = Icons.Rounded.Lock,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            title = "Stays on this device",
            body = "No account to make. Your transactions live in a database here, not on a server."
        )
        if (smsImportSupported) {
            WelcomePoint(
                icon = Icons.Rounded.Sms,
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                title = "Reads your bank SMS",
                body = "Bank and UPI alerts turn into transactions on the phone, about 140 banks covered."
            )
        } else {
            WelcomePoint(
                icon = Icons.Rounded.Language,
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                title = "The same app as on Android",
                body = "Data stays in this browser. Turn on sync later to share it with your phone."
            )
        }
        WelcomePoint(
            icon = Icons.Rounded.CloudSync,
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
            title = "Backups where you choose",
            body = "Google Drive, Nextcloud or a file, encrypted with your password if you set one."
        )
        WelcomePoint(
            icon = Icons.Rounded.AutoAwesome,
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
            title = "AI only if you want it",
            body = if (smsImportSupported) {
                "On supported phones Gemini Nano runs on the device. Or bring a cloud model and key you pick."
            } else {
                "Describe a payment or snap a receipt, using a model and key you pick."
            }
        )
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, container: Color, content: Color, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        StepIcon(icon, container, content)
        Spacer(Modifier.width(MaterialTheme.spacing.medium))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun CurrencyStep(selected: String?, onSelect: (String) -> Unit) {
    StepHeading(
        title = "Which currency do you use?",
        body = "It's shown next to every amount. You can change it later under More, in Currency."
    )
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
        currencyOptions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                row.forEach { option ->
                    val isSelected = option.symbol == selected
                    OutlinedCard(
                        onClick = { onSelect(option.symbol) },
                        modifier = Modifier.weight(1f).handCursor(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val symbol = displaySymbol(option.symbol, option.code)
                            Text(
                                text = symbol,
                                style = if (symbol.length > 2) {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.headlineSmall
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(44.dp)
                            )
                            Spacer(Modifier.width(MaterialTheme.spacing.small))
                            Column(Modifier.weight(1f)) {
                                Text(option.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    option.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * The web build has no font for Arabic, Bengali or Devanagari, so those
 * symbols would draw as boxes there. Show the currency code instead.
 */
private fun displaySymbol(symbol: String, code: String): String {
    val unsupported = isWebPlatform && symbol.any { c ->
        c.code in 0x0600..0x06FF || c.code in 0xFB50..0xFDFF || c.code in 0x0900..0x09FF
    }
    return if (unsupported) code else symbol
}

@Composable
internal fun LookStep(
    themeMode: String,
    paletteId: String?,
    onThemeMode: (String) -> Unit,
    onPalette: (String) -> Unit
) {
    StepHeading(
        title = "Make it yours",
        body = "The app changes as you pick. There are more options under More, in Appearance."
    )
    ThemeModePicker(selectedId = themeMode, onSelect = onThemeMode)
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    PalettePicker(selectedPaletteId = paletteId, onSelect = { onPalette(it.id) })
}

/** SMS permission for the setup flow. Granting it starts a full scan. */
@Composable
internal fun rememberSetupSmsPermissions(): SmsPermissions {
    val smsViewModel: SmsViewModel = koinViewModel()
    val lockGuard = koinInject<AppLockGuard>()
    return rememberSmsPermissions(
        onExternalActivityLaunched = lockGuard::onExternalActivityLaunched,
        onReadSmsGranted = { smsViewModel.startSmsScan(forceFull = true) }
    )
}

internal val SmsPermissions.granted: Boolean get() = hasReadSms && hasReceiveSms

@Composable
internal fun SmsStep(permissions: SmsPermissions) {
    val granted = permissions.granted

    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    BilliMascot(mood = BilliMood.Curious, size = 80.dp, animate = true)
    StepHeading(
        title = "Fill it in from bank SMS",
        body = "Billi can read bank and UPI alerts and add them as transactions, so you don't type them in."
    )
    SmsPreview()
    Spacer(Modifier.height(MaterialTheme.spacing.large))
    Text(
        text = "Messages are read and parsed on this phone and never uploaded. Only money " +
            "messages are kept, and new alerts are added as they arrive.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    if (granted) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(
                    "SMS access is on. Your inbox is being scanned in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        Button(
            onClick = permissions::request,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(Icons.Rounded.Sms, null, Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text("Allow SMS access")
        }
        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        Text(
            text = "Rather not? Add things by hand, or turn this on later under More, in SMS import.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A bank alert and the transaction Billi makes from it. */
@Composable
private fun SmsPreview() {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(4.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                Text("HDFCBK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rs.240.00 debited from A/c XX1234 to CHAAYOS on 09-11-26. UPI Ref 516912345678.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.smaller), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.SouthEast, null, tint = MaterialTheme.colorScheme.primary)
        }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(0.88f).align(Alignment.End)
        ) {
            Row(Modifier.padding(MaterialTheme.spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                StepIcon(
                    Icons.Rounded.Restaurant,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    size = 40.dp
                )
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Column(Modifier.weight(1f)) {
                    Text("Chaayos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Food & Dining · HDFC",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("−₹240", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
            }
        }
    }
}

@Composable
internal fun DoneStep(onOpen: (String) -> Unit) {
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    BilliMascot(mood = BilliMood.Wink, size = 96.dp, animate = true)
    StepHeading(
        title = "You're set",
        body = "Pick something to start with, or go straight to the app."
    )
    val scheme = MaterialTheme.colorScheme
    val aiEnabled = LocalAiEnabled.current
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
        NextStepCard(
            icon = Icons.Rounded.Add,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            title = "Add a transaction",
            body = if (aiEnabled) "By hand, or describe it and let AI fill it in" else "Log an expense or income by hand",
            onClick = { onOpen(AppDestinations.ADD_TRANSACTION) }
        )
        if (aiEnabled) {
            NextStepCard(
                icon = Icons.Rounded.AutoAwesome,
                container = scheme.tertiaryContainer,
                content = scheme.onTertiaryContainer,
                title = "Set up AI",
                body = "Add a model and key for receipts, notes and statements",
                onClick = { onOpen(AppDestinations.AI_SETTINGS) }
            )
        }
        NextStepCard(
            icon = Icons.Rounded.CloudSync,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            title = "Back up or sync",
            body = "Google Drive, Nextcloud, or a file",
            onClick = { onOpen(AppDestinations.BACKUP) }
        )
        if (aiEnabled) {
            NextStepCard(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                container = scheme.surfaceContainerHighest,
                content = scheme.onSurfaceVariant,
                title = "Import a bank statement",
                body = "PDF, CSV or a photo, read by your AI model",
                onClick = { onOpen(AppDestinations.STATEMENT_IMPORT) }
            )
        }
    }
}

@Composable
private fun NextStepCard(
    icon: ImageVector,
    container: Color,
    content: Color,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().handCursor(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(MaterialTheme.spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            StepIcon(icon, container, content)
            Spacer(Modifier.width(MaterialTheme.spacing.medium))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
