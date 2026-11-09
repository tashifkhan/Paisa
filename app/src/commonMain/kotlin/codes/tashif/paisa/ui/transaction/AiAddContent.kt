package codes.tashif.paisa.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.QuickAddFailure
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.parseDate
import codes.tashif.paisa.ui.components.AccountPicker
import codes.tashif.paisa.ui.components.CategoryPicker
import codes.tashif.paisa.ui.components.PillTabs
import codes.tashif.paisa.ui.format.formatMoney
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.platform.handCursor
import codes.tashif.paisa.ui.theme.ExpenseRed
import codes.tashif.paisa.ui.theme.IncomeGreen
import codes.tashif.paisa.ui.theme.spacing

/** The note and attachments that go to the model. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiNoteStep(
    state: AiAddUiState,
    aiReady: Boolean,
    onNoteChange: (String) -> Unit,
    onAddPhotos: () -> Unit,
    onAddFiles: () -> Unit,
    onRemoveFile: (Int) -> Unit,
    onDraft: () -> Unit,
    onCancel: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val drafting = state.busy == AiBusy.DRAFTING
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            enabled = !drafting,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 10,
            label = { Text("Describe it in your own words") },
            placeholder = {
                Text("Lunch 240 at Chaayos yesterday, Uber 180 on my HDFC card, got 5000 back from Ravi")
            },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
            AssistChip(
                onClick = onAddPhotos,
                enabled = !drafting,
                label = { Text("Photos") },
                leadingIcon = {
                    Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(AssistChipDefaults.IconSize))
                },
                modifier = Modifier.handCursor()
            )
            AssistChip(
                onClick = onAddFiles,
                enabled = !drafting,
                label = { Text("PDF or file") },
                leadingIcon = {
                    Icon(Icons.Rounded.AttachFile, null, Modifier.size(AssistChipDefaults.IconSize))
                },
                modifier = Modifier.handCursor()
            )
        }

        if (state.files.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                state.files.forEach { file ->
                    val isImage = file.name.lowercase().let { n ->
                        listOf(".jpg", ".jpeg", ".png", ".webp", ".heic").any(n::endsWith)
                    } || file.source.mimeType?.startsWith("image/") == true
                    InputChip(
                        selected = false,
                        onClick = { if (!drafting) onRemoveFile(file.id) },
                        label = {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(140.dp))
                        },
                        leadingIcon = {
                            Icon(
                                if (isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                                null,
                                Modifier.size(InputChipDefaults.IconSize)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove ${file.name}",
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.handCursor()
                    )
                }
            }
        }

        Text(
            text = if (state.files.isEmpty()) {
                "Write one or several payments, attach receipts, bills, payment screenshots or PDFs, or both. " +
                    "You'll check everything before it's saved."
            } else {
                "Your note goes along with every file, so it can fill in what they don't show, like which card paid."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!aiReady) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "This needs an AI model with your own key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenAiSettings) { Text("Set up AI") }
                }
            }
        }

        state.error?.let { ErrorNote(it) }

        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Button(
            onClick = onDraft,
            enabled = (state.canDraft && aiReady) || drafting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (drafting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(
                    if (state.files.size > 1) "Reading ${state.files.size} files…" else "Reading…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text("Draft with AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (drafting) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel")
            }
        }
    }
}

/** The drafts, each editable, with a box to ask the model for changes. */
@Composable
internal fun AiReviewStep(
    state: AiAddUiState,
    categories: List<Category>,
    accounts: List<Account>,
    currency: String,
    onBack: () -> Unit,
    onUpdate: (AiDraftRow) -> Unit,
    onRemove: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onRefine: (String) -> Unit,
    onSave: () -> Unit,
) {
    var expandedKey by remember { mutableStateOf(state.rows.singleOrNull()?.key) }
    var instruction by rememberSaveable { mutableStateOf("") }
    var pickDateFor by remember { mutableStateOf<AiDraftRow?>(null) }
    var pickTimeFor by remember { mutableStateOf<AiDraftRow?>(null) }
    val busy = state.busy != null
    val allSelected = state.rows.all { it.selected }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.handCursor()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to your note")
            }
            Text(
                text = if (state.rows.size == 1) "Check this draft" else "Check ${state.rows.size} drafts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (state.rows.size > 1) {
                TextButton(onClick = { onSelectAll(!allSelected) }, enabled = !busy) {
                    Text(if (allSelected) "Select none" else "Select all")
                }
            }
        }

        if (state.failures.isNotEmpty()) FailureNote(state.failures)

        state.rows.forEach { row ->
            DraftCard(
                row = row,
                expanded = expandedKey == row.key,
                enabled = !busy,
                categories = categories,
                accounts = accounts,
                currency = currency,
                onExpand = { expandedKey = if (expandedKey == row.key) null else row.key },
                onUpdate = onUpdate,
                onRemove = { onRemove(row.key) },
                onPickDate = { pickDateFor = row },
                onPickTime = { pickTimeFor = row },
            )
        }

        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        OutlinedTextField(
            value = instruction,
            onValueChange = { instruction = it },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask AI to change something") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        onRefine(instruction)
                        instruction = ""
                    },
                    enabled = instruction.isNotBlank() && !busy,
                    modifier = Modifier.handCursor()
                ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ask AI") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (instruction.isNotBlank()) {
                    onRefine(instruction)
                    instruction = ""
                }
            })
        )
        if (state.busy == AiBusy.REFINING) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        state.error?.let { ErrorNote(it) }

        Spacer(Modifier.height(MaterialTheme.spacing.small))
        val count = state.selectedCount
        Button(
            onClick = onSave,
            enabled = count > 0 && !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (state.busy == AiBusy.SAVING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = when (count) {
                        0 -> "Nothing picked"
                        1 -> "Save transaction"
                        else -> "Save $count transactions"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    pickDateFor?.let { row ->
        val (y, m, d) = row.date.split("-").map { it.toIntOrNull() ?: 1 }.let { Triple(it[0], it[1] - 1, it[2]) }
        TransactionDatePickerDialog(
            year = y,
            month = m,
            day = d,
            onDismiss = { pickDateFor = null },
            onPicked = { py, pm, pd ->
                val date = "$py-${(pm + 1).toString().padStart(2, '0')}-${pd.toString().padStart(2, '0')}"
                state.rows.firstOrNull { it.key == row.key }?.let { onUpdate(it.copy(date = date)) }
            }
        )
    }
    pickTimeFor?.let { row ->
        val (h, min) = row.time.split(":").map { it.toIntOrNull() ?: 0 }.let { it[0] to it.getOrElse(1) { 0 } }
        TransactionTimePickerDialog(
            hour = h,
            minute = min,
            onDismiss = { pickTimeFor = null },
            onPicked = { ph, pm ->
                val time = "${ph.toString().padStart(2, '0')}:${pm.toString().padStart(2, '0')}"
                state.rows.firstOrNull { it.key == row.key }?.let { onUpdate(it.copy(time = time)) }
            }
        )
    }
}

@Composable
private fun DraftCard(
    row: AiDraftRow,
    expanded: Boolean,
    enabled: Boolean,
    categories: List<Category>,
    accounts: List<Account>,
    currency: String,
    onExpand: () -> Unit,
    onUpdate: (AiDraftRow) -> Unit,
    onRemove: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    val haptics = rememberHaptics()
    val isIncome = row.type == "income"
    val category = categories.firstOrNull { it.id == row.categoryId }
    val account = accounts.firstOrNull { it.id == row.accountId }
    val ofType = categories.filter { it.type == row.type }

    Card(
        onClick = onExpand,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().animateContentSize().handCursor(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = row.selected,
                enabled = enabled,
                onCheckedChange = {
                    haptics.toggle(it)
                    onUpdate(row.copy(selected = it))
                },
                modifier = Modifier.semantics { contentDescription = "Include ${row.title}" }
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(dateLabel(row.date), category?.name, account?.name).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                row.duplicateNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = row.amount?.let { (if (isIncome) "+" else "−") + formatMoney(currency, it) } ?: "No amount",
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    row.amount == null -> MaterialTheme.colorScheme.error
                    isIncome -> IncomeGreen
                    else -> ExpenseRed
                },
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                PillTabs(
                    options = listOf("Expense" to "expense", "Income" to "income"),
                    selected = row.type,
                    onSelect = { type ->
                        if (type != row.type) {
                            val sameName = categories.firstOrNull {
                                it.type == type && it.name.equals(category?.name, ignoreCase = true)
                            }
                            val next = sameName ?: categories.firstOrNull { it.type == type }
                            onUpdate(row.copy(type = type, categoryId = next?.id))
                        }
                    },
                    height = 44.dp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    OutlinedTextField(
                        value = row.amountText,
                        onValueChange = { text ->
                            if (text.isEmpty() || text.matches(Regex("""\d*\.?\d{0,2}"""))) {
                                onUpdate(row.copy(amountText = text))
                            }
                        },
                        label = { Text("Amount") },
                        prefix = { Text(currency) },
                        singleLine = true,
                        isError = row.amount == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(0.42f)
                    )
                    OutlinedTextField(
                        value = row.title,
                        onValueChange = { onUpdate(row.copy(title = it)) },
                        label = { Text("Title") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(0.58f)
                    )
                }
                OutlinedTextField(
                    value = row.note,
                    onValueChange = { onUpdate(row.copy(note = it)) },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AddDateTimeRow(
                    dateLabel = dateLabel(row.date),
                    timeLabel = timeLabel(row.time),
                    onPickDate = onPickDate,
                    onPickTime = onPickTime
                )
                CategoryPicker(
                    categories = ofType,
                    selectedId = row.categoryId,
                    onSelect = { onUpdate(row.copy(categoryId = it)) },
                    emptyText = "No ${row.type} categories yet"
                )
                AccountPicker(
                    accounts = accounts,
                    selectedId = row.accountId,
                    onSelect = { onUpdate(row.copy(accountId = it)) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "From ${row.origin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRemove, modifier = Modifier.handCursor()) {
                        Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                    TextButton(onClick = onExpand, modifier = Modifier.handCursor()) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun ErrorNote(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun FailureNote(failures: List<QuickAddFailure>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium)) {
            Icon(
                Icons.Rounded.WarningAmber,
                null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = "Nothing came from " + failures.joinToString("; ") { "${it.origin} (${it.message})" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private fun dateLabel(date: String): String =
    parseDate(date, "yyyy-MM-dd", machine = true)?.let { formatDate(it, "EEE, d MMM yyyy") } ?: date

private fun timeLabel(time: String): String {
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return time
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return "$hour12:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}
