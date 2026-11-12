package codes.tashif.paisa.ui.transaction

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.TextFieldDefaults

import androidx.compose.material3.TextField

import androidx.compose.material3.SuggestionChipDefaults

import androidx.compose.material3.SuggestionChip

import androidx.compose.material3.Surface

import androidx.compose.material.icons.rounded.AddPhotoAlternate

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.horizontalScroll

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import codes.tashif.paisa.ui.voice.VoiceClip
import codes.tashif.paisa.ui.voice.VoiceResult
import codes.tashif.paisa.ui.voice.rememberMicPermission
import codes.tashif.paisa.ui.voice.rememberVoiceRecorder
import codes.tashif.paisa.ui.voice.voiceCaptureSupported

/** The note and attachments that go to the model. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiNoteStep(
    state: AiAddUiState,
    aiReady: Boolean,
    canHear: Boolean,
    onNoteChange: (String) -> Unit,
    onAddPhotos: () -> Unit,
    onAddFiles: () -> Unit,
    onRemoveFile: (Int) -> Unit,
    onVoiceRecorded: (VoiceClip) -> Unit,
    onRemoveVoice: () -> Unit,
    onBeforeMicPrompt: () -> Unit,
    onDraft: () -> Unit,
    onCancel: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val drafting = state.busy == AiBusy.DRAFTING
    AnimatedContent(
        targetState = drafting,
        transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
        label = "ai note step"
    ) { reading ->
        if (reading) {
            AiReadingPanel(state = state, onCancel = onCancel)
        } else {
            AiNoteForm(
                state = state,
                aiReady = aiReady,
                canHear = canHear,
                onNoteChange = onNoteChange,
                onAddPhotos = onAddPhotos,
                onAddFiles = onAddFiles,
                onRemoveFile = onRemoveFile,
                onVoiceRecorded = onVoiceRecorded,
                onRemoveVoice = onRemoveVoice,
                onBeforeMicPrompt = onBeforeMicPrompt,
                onDraft = onDraft,
                onOpenAiSettings = onOpenAiSettings
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiNoteForm(
    state: AiAddUiState,
    aiReady: Boolean,
    canHear: Boolean,
    onNoteChange: (String) -> Unit,
    onAddPhotos: () -> Unit,
    onAddFiles: () -> Unit,
    onRemoveFile: (Int) -> Unit,
    onVoiceRecorded: (VoiceClip) -> Unit,
    onRemoveVoice: () -> Unit,
    onBeforeMicPrompt: () -> Unit,
    onDraft: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val haptics = rememberHaptics()
    val recorder = rememberVoiceRecorder()
    var micError by remember { mutableStateOf<String?>(null) }
    fun finishRecording() {
        when (val result = recorder.finish()) {
            is VoiceResult.Clip -> {
                haptics.confirm()
                onVoiceRecorded(result.clip)
            }
            VoiceResult.TooShort -> micError = "That was too short to hear. Hold on a little longer."
            VoiceResult.Silent -> {
                haptics.reject()
                micError = "Didn't catch anything. Check that the mic isn't covered or muted, and speak up a little."
            }
            null -> Unit
        }
    }
    fun startRecording() {
        micError = null
        haptics.click()
        recorder.start(onLimit = ::finishRecording, onError = { micError = it })
    }
    val micPermission = rememberMicPermission { granted ->
        if (granted) {
            startRecording()
        } else {
            micError = "Paisa needs the microphone for voice notes. You can allow it in Android settings."
        }
    }
    val showMic = voiceCaptureSupported && canHear && aiReady
    val recording = recorder.recording
    val empty = state.note.isBlank() && state.files.isEmpty() && state.voice == null && !recording
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        // Examples to start from; gone once there's anything to read.
        AnimatedVisibility(visible = empty) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AI_EXAMPLES.forEach { example ->
                    SuggestionChip(
                        onClick = {
                            haptics.tick()
                            onNoteChange(example)
                        },
                        label = { Text(example, maxLines = 1) },
                        icon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(SuggestionChipDefaults.IconSize)) },
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        }

        // The composer: files, the note and the actions in one card, like a chat box.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(MaterialTheme.spacing.smaller)) {
                if ((state.files.isNotEmpty() || state.voice != null) && !recording) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                    ) {
                        state.voice?.let { VoiceTile(it, onRemove = onRemoveVoice) }
                        state.files.forEach { file -> FileTile(file) { onRemoveFile(file.id) } }
                    }
                }
                AnimatedContent(
                    targetState = recording,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                    label = "voice composer"
                ) { isRecording ->
                if (isRecording) {
                    VoiceRecordingPanel(
                        recorder = recorder,
                        onDiscard = {
                            haptics.reject()
                            recorder.cancel()
                        },
                        onDone = ::finishRecording
                    )
                } else {
                Column {
                TextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    placeholder = {
                        Text(
                            if (state.files.isEmpty()) "Describe one or more payments…"
                            else "Add anything the files don't show, like which card paid…"
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddPhotos, modifier = Modifier.handCursor()) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add photos")
                    }
                    IconButton(onClick = onAddFiles, modifier = Modifier.handCursor()) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = "Add a PDF or file")
                    }
                    if (showMic) {
                        IconButton(
                            onClick = {
                                if (micPermission.granted) {
                                    startRecording()
                                } else {
                                    onBeforeMicPrompt()
                                    micPermission.request()
                                }
                            },
                            modifier = Modifier.handCursor()
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = if (state.voice == null) "Record a voice note" else "Record again"
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onDraft,
                        enabled = state.canDraft && aiReady,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                        Text("Draft", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                }
                }
                }
            }
        }

        Text(
            text = when {
                recording -> "Say the amount, what it was for and how you paid. Several payments are fine."
                state.files.isNotEmpty() && state.voice != null -> "Your note and voice note go along with every file."
                state.files.isNotEmpty() -> "Your note goes along with every file."
                showMic -> "One payment or several, typed or spoken. Attach receipts, bills or screenshots too. " +
                    "You'll check every draft before it's saved."
                else -> "One payment or several, in your own words. Attach receipts, bills or screenshots too. " +
                    "You'll check every draft before it's saved."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
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

        micError?.let { ErrorNote(it) }
        state.error?.let { ErrorNote(it) }
    }
}

private val AI_EXAMPLES = listOf(
    "Lunch 240 at Chaayos yesterday",
    "Uber 180 on my HDFC card",
    "Got 5000 back from Ravi",
    "Rent 28000 on the 1st"
)

/** An attached file inside the composer: type icon, name, and a remove button. */
@Composable
private fun FileTile(file: AttachedFile, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Text(
                file.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove ${file.name}", modifier = Modifier.size(18.dp))
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

        state.transcript?.let { TranscriptNote(it) }
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
