package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun AccountListView(
    accounts: List<Account>,
    currency: String,
    selectedIds: Set<Int>,
    onToggleSelect: (Int) -> Unit,
    onOpenActions: (Account) -> Unit,
    onReorder: (List<Int>) -> Unit,
    onClearSelection: () -> Unit,
    onMergeClick: () -> Unit,
    onAutoMerge: () -> Unit
) {
    val selectionMode = selectedIds.isNotEmpty()
    val haptics = rememberHaptics()

    // Local visual order during a drag; re-synced from the DB order between drags.
    val localOrder = remember { accounts.map { it.id }.toMutableStateList() }
    val dragState = remember { AccountDragState() }
    // Assigned every composition so the drag always mutates the live list,
    // never a stale capture from when the state object was first created.
    dragState.onMove = { from, to ->
        if (from in localOrder.indices && to in localOrder.indices) {
            haptics.tick()
            localOrder.add(to, localOrder.removeAt(from))
        }
    }
    LaunchedEffect(accounts) {
        if (dragState.draggingIndex == null) {
            localOrder.clear()
            localOrder.addAll(accounts.map { it.id })
        }
    }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val displayAccounts = localOrder.mapNotNull { accountsById[it] } +
        accounts.filter { it.id !in localOrder }

    val listState = rememberLazyListState()
    val count = displayAccounts.size

    Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                    end = MaterialTheme.spacing.medium,
                    top = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.smaller
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // The floating toolbar shows the count while picking.
                text = if (selectionMode) "Pick accounts to merge" else "Your accounts",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            // While picking, cancel and merge move to the floating toolbar.
            if (!selectionMode) {
                TextButton(onClick = {
                    haptics.click()
                    onAutoMerge()
                }) {
                    Text("Auto-merge")
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.extraSmall,
                bottom = if (selectionMode) 120.dp else MaterialTheme.spacing.extraLarge
            ),
            // 2dp gaps match SettingsGroup / Pixel settings stacks
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(displayAccounts, key = { _, account -> account.id }) { index, account ->
                val isDragging = dragState.draggingIndex == index
                AccountListRow(
                    account = account,
                    currency = currency,
                    position = groupPositionOf(index, count),
                    selectionMode = selectionMode,
                    selected = account.id in selectedIds,
                    isDragging = isDragging,
                    dragOffset = if (isDragging) dragState.offset else 0f,
                    onClick = {
                        if (selectionMode) {
                            haptics.toggle(on = account.id !in selectedIds)
                            onToggleSelect(account.id)
                        } else {
                            haptics.click()
                            onOpenActions(account)
                        }
                    },
                    // combinedClickable already plays the system long-press haptic
                    onLongClick = { onToggleSelect(account.id) },
                    onDragStart = {
                        dragState.start(
                            displayAccounts.indexOfFirst { it.id == account.id },
                            listState
                        )
                    },
                    onDrag = { dy ->
                        dragState.drag(dy, displayAccounts.size)
                    },
                    onDragEnd = {
                        dragState.end()
                        onReorder(localOrder.toList())
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .then(if (isDragging) Modifier else Modifier.animateItem())
                )
            }
        }
    }
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MaterialTheme.spacing.medium)
        ) {
            SelectionToolbar(
                count = selectedIds.size,
                onCancel = onClearSelection,
                onMerge = {
                    haptics.click()
                    onMergeClick()
                }
            )
        }
    }
}

/**
 * Shown while picking accounts to merge: how many are picked, a way out,
 * and merge as the toolbar's FAB once there are two to merge.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionToolbar(count: Int, onCancel: () -> Unit, onMerge: () -> Unit) {
    val content: @Composable RowScope.() -> Unit = {
        IconButton(onClick = onCancel) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel")
        }
        Text(
            text = if (count >= 2) "$count selected" else "Pick one more",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = MaterialTheme.spacing.small)
        )
    }
    if (count >= 2) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onMerge) {
                    Icon(Icons.AutoMirrored.Rounded.MergeType, contentDescription = "Merge $count accounts")
                }
            },
            content = content
        )
    } else {
        HorizontalFloatingToolbar(expanded = true, content = content)
    }
}

/**
 * Tracks the actively dragged row. Positions are computed arithmetically from
 * the accumulated drag distance and a fixed slot height captured at drag start,
 * so item placement animations can't feed back into the math.
 */
private class AccountDragState {
    /** Reassigned on every composition so it always sees the current list. */
    var onMove: (Int, Int) -> Unit = { _, _ -> }

    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    private var startIndex = 0
    private var slotPx = 0f
    private var rawDelta by mutableFloatStateOf(0f)

    /** Visual translation for the dragged row relative to its current slot. */
    val offset: Float
        get() {
            val current = draggingIndex ?: return 0f
            return rawDelta - (current - startIndex) * slotPx
        }

    fun start(index: Int, listState: LazyListState) {
        if (index < 0) return
        val items = listState.layoutInfo.visibleItemsInfo
        // Slot height = distance between consecutive item offsets (incl. spacing)
        slotPx = items.zipWithNext { a, b -> (b.offset - a.offset).toFloat() }
            .firstOrNull { it > 0f }
            ?: items.firstOrNull()?.size?.toFloat()
            ?: return
        startIndex = index
        draggingIndex = index
        rawDelta = 0f
    }

    fun drag(dy: Float, itemCount: Int) {
        val current = draggingIndex ?: return
        rawDelta += dy
        if (slotPx <= 0f) return
        val target = (startIndex + (rawDelta / slotPx).roundToInt())
            .coerceIn(0, itemCount - 1)
        if (target != current) {
            onMove(current, target)
            draggingIndex = target
        }
    }

    fun end() {
        draggingIndex = null
        rawDelta = 0f
    }
}
