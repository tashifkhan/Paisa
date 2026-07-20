package codes.tashif.paisa.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import codes.tashif.paisa.R

/**
 * One-tap entry points, styled like a maps “search + shortcuts” stack:
 * a full-width primary pill on top, and a rounded strip of equal icon tiles
 * underneath for the remaining shortcuts.
 */
class QuickActionsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent {
            PaisaGlanceTheme(context, snapshot.palette) {
                QuickActionsContent(context)
            }
        }
    }

    companion object {
        val NARROW = DpSize(180.dp, 130.dp)
        val WIDE = DpSize(280.dp, 130.dp)
    }
}

@Composable
private fun QuickActionsContent(context: Context) {
    val width = LocalSize.current.width
    // Drop the least-critical shortcuts first when the placement is tight.
    val actionCount = when {
        width >= QuickActionsWidget.WIDE.width -> 4
        else -> 3
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        // Primary pill — maps-style search bar, but for “Add transaction”.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(
                    actionStartActivity(
                        WidgetDeepLink.intent(context, WidgetDeepLink.DEST_ADD_TRANSACTION)
                    )
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                modifier = GlanceModifier.size(20.dp)
            )
            Spacer(GlanceModifier.width(10.dp))
            Text(
                text = "Add transaction",
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        // Shortcut strip — equal rounded tiles in one soft container.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .cornerRadius(22.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionTile(
                iconRes = R.drawable.ic_widget_home,
                description = "Open Paisa",
                background = GlanceTheme.colors.widgetBackground,
                foreground = GlanceTheme.colors.primary,
                onClick = actionStartActivity(
                    WidgetDeepLink.intent(context, WidgetDeepLink.DEST_HOME)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(6.dp))
            ActionTile(
                iconRes = R.drawable.ic_widget_sync,
                description = "Rescan bank SMS",
                background = GlanceTheme.colors.widgetBackground,
                foreground = GlanceTheme.colors.primary,
                onClick = actionRunCallback<RescanSmsAction>(),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(6.dp))
            ActionTile(
                iconRes = R.drawable.ic_widget_ai,
                description = "Import a statement with AI",
                background = GlanceTheme.colors.widgetBackground,
                foreground = GlanceTheme.colors.primary,
                onClick = actionStartActivity(
                    WidgetDeepLink.intent(context, WidgetDeepLink.DEST_STATEMENT_IMPORT)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (actionCount >= 4) {
                Spacer(GlanceModifier.width(6.dp))
                ActionTile(
                    iconRes = R.drawable.ic_widget_accounts,
                    description = "Open accounts",
                    background = GlanceTheme.colors.widgetBackground,
                    foreground = GlanceTheme.colors.primary,
                    onClick = actionStartActivity(
                        WidgetDeepLink.intent(context, WidgetDeepLink.DEST_ACCOUNTS)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    iconRes: Int,
    description: String,
    background: ColorProvider,
    foreground: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .cornerRadius(14.dp)
            .background(background)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = description,
            colorFilter = ColorFilter.tint(foreground),
            modifier = GlanceModifier.size(24.dp)
        )
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()
}
