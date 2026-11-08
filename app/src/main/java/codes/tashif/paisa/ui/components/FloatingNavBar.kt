package codes.tashif.paisa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class NavTab(val label: String, val icon: ImageVector)

/** Height of the capsule. FABs and content clear it through the Scaffold's bottomBar slot. */
val FloatingNavBarHeight = 64.dp
private val Margin = 14.dp

/**
 * Pill-shaped bottom bar that floats above the screen edge, after Google
 * Photos (and Patchwork's Android floating navbar). The selected tab sits in
 * a tonal pill with its icon and label; the others show only their icon so
 * five tabs fit on a narrow phone.
 *
 * It slides away while the keyboard is open so it never covers a text field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardOpen = WindowInsets.isImeVisible
    AnimatedVisibility(
        visible = !keyboardOpen,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Margin, end = Margin, bottom = Margin),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(FloatingNavBarHeight)
                        .padding(horizontal = 8.dp)
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        FloatingNavItem(
                            tab = tab,
                            selected = index == selected,
                            onClick = { onSelect(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else Color.Transparent,
        tween(SELECT_MS),
        label = "pill"
    )
    val content = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    val horizontal by animateDpAsState(if (selected) 18.dp else 14.dp, tween(SELECT_MS), label = "pad")
    Row(
        modifier = Modifier
            .height(FloatingNavBarHeight - 16.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .semantics { contentDescription = tab.label }
            .background(container)
            .padding(horizontal = horizontal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(SELECT_MS)) + fadeIn(tween(SELECT_MS)),
            exit = shrinkHorizontally(tween(SELECT_MS)) + fadeOut(tween(120))
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

private const val SELECT_MS = 240
