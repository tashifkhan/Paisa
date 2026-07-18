package codes.tashif.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Pixel-settings-style list: each group is a stack of rows with large rounded
 * corners on the group's ends and small corners between rows, separated by a
 * 2dp gap.
 */
enum class GroupPosition { Single, Top, Middle, Bottom }

fun groupPositionOf(index: Int, count: Int): GroupPosition = when {
    count == 1 -> GroupPosition.Single
    index == 0 -> GroupPosition.Top
    index == count - 1 -> GroupPosition.Bottom
    else -> GroupPosition.Middle
}

@Composable
fun groupShape(position: GroupPosition): Shape {
    val large = 24.dp
    val small = 6.dp
    return when (position) {
        GroupPosition.Single -> RoundedCornerShape(large)
        GroupPosition.Top -> RoundedCornerShape(
            topStart = large, topEnd = large, bottomStart = small, bottomEnd = small
        )
        GroupPosition.Middle -> RoundedCornerShape(small)
        GroupPosition.Bottom -> RoundedCornerShape(
            topStart = small, topEnd = small, bottomStart = large, bottomEnd = large
        )
    }
}

/** Section label shown above a settings group. */
@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.small,
            top = MaterialTheme.spacing.medium,
            bottom = MaterialTheme.spacing.smaller
        )
    )
}

/** Column wrapper that spaces group rows by 2dp. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
}

/** Tinted circular icon used at the start of a settings row. */
@Composable
fun SettingsItemIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    position: GroupPosition,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable {
                            haptics.click()
                            onClick()
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small + 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsItemIcon(
                icon = icon,
                containerColor = iconContainerColor,
                contentColor = iconContentColor
            )
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.size(MaterialTheme.spacing.smaller))
                trailing()
            }
        }
    }
}
