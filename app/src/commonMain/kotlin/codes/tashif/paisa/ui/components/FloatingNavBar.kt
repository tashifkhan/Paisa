package codes.tashif.paisa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.platform.isImeVisible
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * What a tab's icon does when it's tapped in the floating bar. One small
 * move per tab, after the dock in Expressive Clock, where the alarm bell
 * shakes and the stopwatch spins.
 */
enum class TabMotion { NONE, WIGGLE, HOP, HALF_TURN, FULL_TURN }

data class NavTab(val label: String, val icon: ImageVector, val motion: TabMotion = TabMotion.NONE)

/** Height of the capsule. FABs clear it through the Scaffold's bottomBar slot. */
val FloatingNavBarHeight = 64.dp
private val ItemHeight = FloatingNavBarHeight - 16.dp
private val Margin = 14.dp

/**
 * How much of the bottom of a tab the floating bar covers, gesture bar
 * included. Tabs draw under the bar, so each scrolling screen adds this to
 * its bottom padding to let its last row scroll clear. Zero with the docked bar.
 */
val LocalFloatingNavInset = compositionLocalOf { 0.dp }

/**
 * Pill-shaped bottom bar that floats above the screen edge, after Google
 * Photos (and Patchwork's Android floating navbar). The screen keeps drawing
 * behind and around it.
 *
 * Its motion follows the dock in Expressive Clock: the bar gives under a
 * finger and lights up there, one indicator slides between the tabs, and each
 * icon makes its own small move when tapped.
 *
 * By default the selected tab shows its label beside its icon and the others
 * show only an icon, so five tabs fit on a narrow phone. With [showLabels]
 * every tab has a small title under its icon and the bar spans the screen.
 *
 * A non-null [hazeState] makes the bar frosted: it blurs whatever the matching
 * `hazeSource` draws behind it. Before Android 12 there is no blur, and the
 * bar gets a nearly opaque tinted fill instead.
 *
 * It slides away while the keyboard is open so it never covers a text field.
 */
@Composable
fun FloatingNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
    hazeState: HazeState? = null
) {
    val keyboardOpen = isImeVisible()
    val settle = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    AnimatedVisibility(
        visible = !keyboardOpen,
        enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it / 2 } +
            scaleIn(settle, initialScale = 0.92f) + fadeIn(),
        exit = slideOutVertically { it / 2 } + scaleOut(targetScale = 0.92f) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Margin, end = Margin, bottom = Margin),
            contentAlignment = Alignment.Center
        ) {
            val colors = MaterialTheme.colorScheme
            val dark = colors.surface.luminance() < 0.5f
            // A little of the accent in the fill, so the bar belongs to the palette
            // instead of sitting on the screen as a gray slab.
            val fill = colors.primary.copy(alpha = 0.08f).compositeOver(colors.surfaceContainerHigh)
            // The whole bar gives under a finger and leans toward it. The wide
            // bar moves less: the same percentage of it is a lot more pixels.
            val press = rememberElasticPress()
            val bar = (if (showLabels) Modifier.widthIn(max = 520.dp).fillMaxWidth() else Modifier)
                .elasticPress(
                    state = press,
                    maxStretch = if (showLabels) 0.03f else 0.06f,
                    pressedScale = if (showLabels) 0.975f else 0.95f
                )
            val items = (if (showLabels) Modifier.fillMaxWidth() else Modifier).pressGlow(press)
            if (hazeState != null) {
                Box(
                    modifier = bar
                        .clip(CircleShape)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = colors.surface,
                                // Denser at the top left, as if lit from there.
                                tints = listOf(
                                    HazeTint(
                                        Brush.linearGradient(
                                            listOf(fill.copy(alpha = 0.7f), fill.copy(alpha = 0.5f))
                                        )
                                    )
                                ),
                                blurRadius = 24.dp,
                                noiseFactor = 0.06f,
                                // No blur available: nearly opaque, or rows show through sharp.
                                fallbackTint = HazeTint(fill.copy(alpha = 0.9f))
                            )
                        )
                        // A shadow would show through the glass; a bright edge marks it instead.
                        .border(1.25.dp, Color.White.copy(alpha = if (dark) 0.14f else 0.7f), CircleShape)
                ) {
                    NavItems(tabs, selected, showLabels, colors.secondaryContainer.copy(alpha = 0.8f), onSelect, items)
                }
            } else {
                Surface(
                    // Light themes get a shadow in the accent colour. On a dark
                    // screen a coloured shadow can't be seen, so it stays black.
                    modifier = bar.shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = if (dark) Color.Black else colors.primary.copy(alpha = 0.24f),
                        spotColor = if (dark) Color.Black else colors.primary.copy(alpha = 0.4f)
                    ),
                    shape = CircleShape,
                    color = fill
                ) {
                    NavItems(tabs, selected, showLabels, colors.secondaryContainer, onSelect, items)
                }
            }
        }
    }
}

/** Where a tab sits in the row, in pixels. */
private data class Span(val left: Float, val width: Float)

@Composable
private fun NavItems(
    tabs: List<NavTab>,
    selected: Int,
    showLabels: Boolean,
    pillColor: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spans = remember(tabs.size) {
        mutableStateListOf<Span?>().apply { repeat(tabs.size) { add(null) } }
    }
    // One pill slides between the tabs instead of each tab fading its own.
    // A tab opened from More isn't in the bar: the pill fades where it was.
    val target = spans.getOrNull(selected)
    val last = remember { arrayOfNulls<Span>(1) }
    if (target != null) last[0] = target
    val shown = target ?: last[0]

    // The pill appears in place the first time; after that it travels.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(shown != null) {
        if (shown != null) {
            withFrameNanos { }
            settled = true
        }
    }
    // The edge facing the new tab leads and the other one follows, so the
    // pill stretches a little on the way over.
    val move = remember { intArrayOf(selected, 1) }
    if (selected >= 0 && move[0] != selected) {
        move[1] = if (selected > move[0]) 1 else -1
        move[0] = selected
    }
    val lead = spring<Float>(dampingRatio = 0.84f, stiffness = 600f)
    val trail = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)
    val startEdge by animateFloatAsState(
        targetValue = shown?.left ?: 0f,
        animationSpec = if (!settled) snap() else if (move[1] < 0) lead else trail,
        label = "pill_start"
    )
    val endEdge by animateFloatAsState(
        targetValue = shown?.let { it.left + it.width } ?: 0f,
        animationSpec = if (!settled) snap() else if (move[1] > 0) lead else trail,
        label = "pill_end"
    )
    val pillAlpha by animateFloatAsState(if (target != null) 1f else 0f, label = "pill_alpha")

    Row(
        modifier = modifier
            .height(FloatingNavBarHeight)
            .padding(horizontal = 8.dp)
            // After the padding, so the pill and the tabs share one origin.
            .drawBehind {
                if (pillAlpha > 0f && endEdge > startEdge) {
                    val height = ItemHeight.toPx()
                    drawRoundRect(
                        color = pillColor.copy(alpha = pillColor.alpha * pillAlpha),
                        topLeft = Offset(startEdge, (size.height - height) / 2),
                        size = Size(endEdge - startEdge, height),
                        cornerRadius = CornerRadius(height / 2)
                    )
                }
            }
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            FloatingNavItem(
                tab = tab,
                selected = index == selected,
                stacked = showLabels,
                onClick = { onSelect(index) },
                onPlaced = { span -> if (spans[index] != span) spans[index] = span }
            )
        }
    }
}

@Composable
private fun RowScope.FloatingNavItem(
    tab: NavTab,
    selected: Boolean,
    stacked: Boolean,
    onClick: () -> Unit,
    onPlaced: (Span) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Icons you aren't on sit a touch smaller.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = tween(180),
        label = "icon"
    )
    // Every tap plays the tab's own move, on the tab you're already on too.
    var taps by remember { mutableIntStateOf(0) }
    val turn = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        turn.snapTo(0f)
        hop.snapTo(0f)
        when (tab.motion) {
            TabMotion.WIGGLE -> {
                turn.animateTo(-5f, tween(65))
                turn.animateTo(5f, tween(85))
                turn.animateTo(-3f, tween(65))
                turn.animateTo(0f, tween(65))
            }
            TabMotion.HOP -> {
                hop.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 500f))
            }
            TabMotion.HALF_TURN -> turn.animateTo(180f, tween(320))
            TabMotion.FULL_TURN -> turn.animateTo(360f, tween(420))
            TabMotion.NONE -> Unit
        }
        // A finished turn looks the same as no turn, so this doesn't show.
        turn.snapTo(0f)
    }
    val content by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        label = "content"
    )
    val horizontal by animateDpAsState(
        targetValue = if (stacked) 4.dp else if (selected) 18.dp else 14.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pad"
    )
    // A tab showing its label reads that; an icon alone needs a name for TalkBack.
    val labeled = stacked || selected
    val item = Modifier
        .then(if (stacked) Modifier.weight(1f) else Modifier)
        .height(ItemHeight)
        .onGloballyPositioned {
            onPlaced(Span(left = it.positionInParent().x, width = it.size.width.toFloat()))
        }
        .clip(CircleShape)
        .selectable(
            selected = selected,
            onClick = {
                taps++
                onClick()
            },
            role = Role.Tab,
            interactionSource = remember { MutableInteractionSource() },
            // The bar lights up under the finger, so no ripple per tab on top of that.
            indication = null
        )
        .then(if (labeled) Modifier else Modifier.semantics { contentDescription = tab.label })
        .padding(horizontal = horizontal)
    val icon = Modifier.graphicsLayer {
        scaleX = iconScale
        scaleY = iconScale
        rotationZ = turn.value
        translationY = -hop.value * 3.dp.toPx()
    }

    if (stacked) {
        Column(
            modifier = item,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(tab.icon, contentDescription = null, tint = content, modifier = icon.size(22.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }
    Row(modifier = item, verticalAlignment = Alignment.CenterVertically) {
        Icon(tab.icon, contentDescription = null, tint = content, modifier = icon.size(24.dp))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                fadeIn(),
            exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
        ) {
            Row {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1
                )
            }
        }
    }
}
