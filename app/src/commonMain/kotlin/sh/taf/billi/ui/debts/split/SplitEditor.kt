package sh.taf.billi.ui.debts.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import sh.taf.billi.ai.BillScanService
import sh.taf.billi.core.AppLockGuard
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.DebtPersonBalance
import sh.taf.billi.ui.format.formatMoney
import sh.taf.billi.statement.StatementSource
import sh.taf.billi.ui.statement.rememberCameraCapture
import sh.taf.billi.ui.statement.rememberPhotoPicker
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Who's in a split and how it's divided. Takes the [plan] and reports every
 * change through [onPlanChange]. [outcome] is the plan worked out against the total.
 * [onScannedTotal] gets the total printed on a bill, when a scan finds one.
 */
@Composable
fun SplitEditor(
    people: List<DebtPersonBalance>,
    plan: SplitPlan,
    onPlanChange: (SplitPlan) -> Unit,
    outcome: SplitOutcome,
    currency: String,
    modifier: Modifier = Modifier,
    onScannedTotal: (Double) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scanner = koinInject<BillScanService>()
    val snackbar = koinInject<SnackbarManager>()
    val lock = koinInject<AppLockGuard>()
    val latestPlan by rememberUpdatedState(plan)
    val latestOnPlan by rememberUpdatedState(onPlanChange)
    val latestOnTotal by rememberUpdatedState(onScannedTotal)
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    fun readBill(file: StatementSource) {
        if (!scanner.canRun()) {
            scanMessage = "Add a model in Settings before a bill can be read"
            return
        }
        scope.launch {
            scanning = true
            scanMessage = null
            try {
                val bill = scanner.scan(file)
                val items = bill.items.map { BillItem(it.name, plain(it.amount)) }
                latestOnPlan(latestPlan.copy(mode = SplitMode.ITEMS, items = items))
                bill.total?.let(latestOnTotal)
                snackbar.show(if (items.size == 1) "Read 1 item" else "Read ${items.size} items")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                scanMessage = e.message?.takeIf { it.isNotBlank() } ?: "Couldn't read that bill"
            } finally {
                scanning = false
            }
        }
    }
    val openPhotos = rememberPhotoPicker { files ->
        files.firstOrNull()?.let(::readBill)
    }
    val openCamera = rememberCameraCapture(
        onCaptured = ::readBill,
        onUnavailable = { scanMessage = "No camera on this device. Choose a photo instead." },
        onPermissionDenied = { scanMessage = "Camera permission is needed to take a photo of the bill" }
    )
    fun beginScan(camera: Boolean) {
        if (scanning) return
        lock.onExternalActivityLaunched()
        if (camera) openCamera() else openPhotos()
    }

    val haptics = rememberHaptics()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        SplitSectionLabel("Split between")
        SplitPeoplePicker(
            people = people,
            plan = plan,
            onPlanChange = onPlanChange,
            currency = currency
        )

        SplitSectionLabel("Divide by", Modifier.padding(top = MaterialTheme.spacing.smaller))
        val modes = listOf(
            "Equal" to SplitMode.EQUAL,
            "Amount" to SplitMode.CUSTOM,
            "%" to SplitMode.PERCENT,
            "Items" to SplitMode.ITEMS
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (label, mode) ->
                SegmentedButton(
                    selected = plan.mode == mode,
                    onClick = {
                        if (mode != plan.mode) {
                            haptics.tick()
                            val next = when (mode) {
                                SplitMode.CUSTOM -> plan.toCustom(outcome)
                                SplitMode.PERCENT -> plan.toPercent(outcome, outcome.total())
                                else -> plan.copy(mode = mode)
                            }
                            onPlanChange(next)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    // Four segments on a phone leave no room for the check icon.
                    icon = {},
                    label = { Text(label, maxLines = 1) }
                )
            }
        }

        if (plan.mode == SplitMode.ITEMS) {
            SplitItemsEditor(
                plan = plan,
                outcome = outcome,
                currency = currency,
                scanning = scanning,
                scanMessage = scanMessage,
                onPlanChange = onPlanChange,
                onTakePhoto = { beginScan(camera = true) },
                onChoosePhoto = { beginScan(camera = false) }
            )
        }

        SplitShareList(plan = plan, outcome = outcome, currency = currency, onPlanChange = onPlanChange)

        when (plan.mode) {
            SplitMode.CUSTOM -> ShareRemainder(outcome, currency)
            SplitMode.PERCENT -> {
                val over = plan.percentTotal > 100.005
                SplitCaption("${percentText(plan.percentTotal)}% of 100% assigned", isError = over)
                ShareRemainder(outcome, currency)
            }
            else -> Unit
        }
    }
}

/** Small heading over a part of the split editor. */
@Composable
internal fun SplitSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun SplitCaption(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall)
    )
}

/**
 * One list item per person in the split, you last, with their share at the
 * end. In Amount and % modes the share is a field.
 */
@Composable
private fun SplitShareList(
    plan: SplitPlan,
    outcome: SplitOutcome,
    currency: String,
    onPlanChange: (SplitPlan) -> Unit
) {
    if (plan.people.isEmpty()) return
    val rows = outcome.shares.map { Triple(it.person.key, it.person.name, it.amount) } +
        if (plan.includeMe) listOf(Triple(SplitPlan.ME_KEY, "You", outcome.myShare)) else emptyList()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            rows.forEachIndexed { index, (key, name, amount) ->
                if (index > 0) RowDivider()
                ListItem(
                    headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = if (plan.mode == SplitMode.PERCENT) {
                        { Text(formatMoney(currency, amount)) }
                    } else {
                        null
                    },
                    leadingContent = { SplitAvatar(name) },
                    trailingContent = {
                        when (plan.mode) {
                            SplitMode.CUSTOM -> ShareField(
                                value = plan.customText[key].orEmpty(),
                                label = "$name's amount",
                                prefix = currency
                            ) { onPlanChange(plan.copy(customText = plan.customText + (key to it))) }
                            SplitMode.PERCENT -> ShareField(
                                value = plan.percentText[key].orEmpty(),
                                label = "$name's percent",
                                suffix = "%"
                            ) { onPlanChange(plan.copy(percentText = plan.percentText + (key to it))) }
                            else -> Text(
                                text = formatMoney(currency, amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun ShareField(
    value: String,
    label: String,
    prefix: String? = null,
    suffix: String? = null,
    onValue: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            if (text.isEmpty() || text.toDoubleOrNull() != null) onValue(text)
        },
        placeholder = { Text("0") },
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .width(128.dp)
            .semantics { contentDescription = label }
    )
}

@Composable
private fun ShareRemainder(outcome: SplitOutcome, currency: String) {
    if (abs(outcome.left) < 0.005) return
    if (outcome.left > 0) {
        SplitCaption("${formatMoney(currency, outcome.left)} left to assign")
    } else {
        SplitCaption("${formatMoney(currency, -outcome.left)} over the total", isError = true)
    }
}

/**
 * "Radhika owes ₹600" lines and "Your share ₹600". When [payer] paid, only
 * what you owe them is shown, since that's all that gets recorded.
 */
@Composable
fun SplitPreview(
    outcome: SplitOutcome,
    currency: String,
    modifier: Modifier = Modifier,
    payer: SplitPerson? = null
) {
    val lines = buildList {
        if (payer == null) {
            outcome.shares.filter { it.amount > 0.0 }.forEach {
                add("${it.person.name} owes" to it.amount)
            }
        } else if (outcome.myShare > 0.0) {
            add("You owe ${payer.name}" to outcome.myShare)
        }
        if (outcome.myShare > 0.0) add("Your share" to outcome.myShare)
    }
    if (lines.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { (label, amount) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    formatMoney(currency, amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Short reason the split can't be saved yet, or null. */
fun splitProblemText(problem: SplitProblem?): String? = when (problem) {
    null -> null
    SplitProblem.NO_TOTAL -> "Enter the total first"
    SplitProblem.NO_PEOPLE -> "Pick who you're splitting with"
    SplitProblem.NOTHING_ASSIGNED -> "Nothing is assigned yet"
    SplitProblem.OVER_TOTAL -> "The shares are more than the total"
    SplitProblem.NEEDS_ME -> "Turn on Include me when someone else paid"
}
