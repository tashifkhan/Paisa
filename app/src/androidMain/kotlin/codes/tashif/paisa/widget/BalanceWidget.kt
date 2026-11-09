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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import codes.tashif.paisa.R
import codes.tashif.paisa.data.Account

/**
 * Home-screen total-balance widget.
 *
 * Layout mirrors common finance “summary” cards: a soft total-balance pill with
 * privacy + expand controls, then a side-by-side Income / Expense breakdown for
 * the current month. Expanding reveals the top accounts by balance.
 */
class BalanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(COMPACT, REGULAR, EXPANDED)
    )

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent {
            // No stored preference yet means the widget was just placed, so fall
            // back to the user's app-wide "hide balances" choice.
            val hidden = currentState(BalanceHiddenKey) ?: snapshot.hideBalancesByDefault
            val expanded = currentState(BalanceExpandedKey) ?: false
            PaisaGlanceTheme(context, snapshot.palette) {
                BalanceContent(
                    context = context,
                    snapshot = snapshot,
                    hidden = hidden,
                    expanded = expanded
                )
            }
        }
    }

    companion object {
        /** Balance + income/expense row (matches the reference card stack). */
        val COMPACT = DpSize(180.dp, 160.dp)

        /** Wider total card, same breakdown. */
        val REGULAR = DpSize(280.dp, 160.dp)

        /** Room for account rows under the breakdown. */
        val EXPANDED = DpSize(280.dp, 260.dp)
    }
}

@Composable
private fun BalanceContent(
    context: Context,
    snapshot: WidgetSnapshot,
    hidden: Boolean,
    expanded: Boolean
) {
    val density = LocalContext.current.resources.displayMetrics.density
    // Account rows only after the chevron expand — keeps the default layout matching
    // the reference (total + income/expense) on every size.
    val showAccounts = expanded

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        TotalBalanceCard(
            context = context,
            snapshot = snapshot,
            hidden = hidden,
            expanded = showAccounts,
            density = density
        )

        Spacer(GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            BreakdownCard(
                title = "Income",
                iconRes = R.drawable.ic_widget_trending_up,
                amount = snapshot.monthIncome,
                currency = snapshot.currency,
                hidden = hidden,
                density = density,
                background = GlanceTheme.colors.secondaryContainer,
                foreground = GlanceTheme.colors.onSecondaryContainer,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(8.dp))
            BreakdownCard(
                title = "Expense",
                iconRes = R.drawable.ic_widget_trending_down,
                amount = snapshot.monthExpense,
                currency = snapshot.currency,
                hidden = hidden,
                density = density,
                background = GlanceTheme.colors.tertiaryContainer,
                foreground = GlanceTheme.colors.onTertiaryContainer,
                modifier = GlanceModifier.defaultWeight()
            )
        }

        if (showAccounts && snapshot.accounts.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            snapshot.accounts
                .sortedByDescending { it.currentBalance }
                .take(3)
                .forEach { account ->
                    AccountRow(
                        account = account,
                        currency = snapshot.currency,
                        hidden = hidden,
                        density = density
                    )
                    Spacer(GlanceModifier.height(6.dp))
                }
        } else if (showAccounts && snapshot.creditCardDebt > 0.0) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = if (hidden) {
                    "Cards $MASKED_AMOUNT"
                } else {
                    "Cards ${formatWidgetMoney(snapshot.currency, snapshot.creditCardDebt)} due"
                },
                style = TextStyle(
                    color = GlanceTheme.colors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TotalBalanceCard(
    context: Context,
    snapshot: WidgetSnapshot,
    hidden: Boolean,
    expanded: Boolean,
    density: Float
) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(28.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(
                actionStartActivity(
                    WidgetDeepLink.intent(context, WidgetDeepLink.DEST_HOME)
                )
            )
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total balance",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            // Soft rounded control chips — eye for privacy, chevron for expand.
            SoftIconButton(
                iconRes = if (hidden) {
                    R.drawable.ic_widget_visibility
                } else {
                    R.drawable.ic_widget_visibility_off
                },
                contentDescription = if (hidden) "Show balance" else "Hide balance",
                // Soft tonal chip on the same family as the card (like the reference).
                background = GlanceTheme.colors.secondaryContainer,
                foreground = GlanceTheme.colors.onSecondaryContainer,
                onClick = actionRunCallback<ToggleBalanceVisibilityAction>()
            )
            Spacer(GlanceModifier.width(6.dp))
            SoftIconButton(
                iconRes = if (expanded) {
                    R.drawable.ic_widget_collapse
                } else {
                    R.drawable.ic_widget_expand
                },
                contentDescription = if (expanded) "Hide accounts" else "Show accounts",
                background = GlanceTheme.colors.secondaryContainer,
                foreground = GlanceTheme.colors.onSecondaryContainer,
                onClick = actionRunCallback<ToggleBalanceExpandedAction>()
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        AmountOrMask(
            amount = snapshot.totalBalance,
            currency = snapshot.currency,
            hidden = hidden,
            density = density,
            color = GlanceTheme.colors.onPrimaryContainer,
            large = size.width >= BalanceWidget.REGULAR.width,
            pipDp = if (size.width >= BalanceWidget.REGULAR.width) 12f else 10f
        )
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    iconRes: Int,
    amount: Double,
    currency: String,
    hidden: Boolean,
    density: Float,
    background: ColorProvider,
    foreground: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(background)
            .cornerRadius(22.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(foreground),
                modifier = GlanceModifier.size(16.dp)
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = title,
                style = TextStyle(
                    color = foreground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        AmountOrMask(
            amount = amount,
            currency = currency,
            hidden = hidden,
            density = density,
            color = foreground,
            large = false,
            pipDp = 8f
        )
    }
}

@Composable
private fun SoftIconButton(
    iconRes: Int,
    contentDescription: String,
    background: ColorProvider,
    foreground: ColorProvider,
    onClick: androidx.glance.action.Action
) {
    // Rounded square chip, not a cookie — matches the reference controls.
    Box(
        modifier = GlanceModifier
            .size(32.dp)
            .background(background)
            .cornerRadius(12.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(foreground),
            modifier = GlanceModifier.size(16.dp)
        )
    }
}

@Composable
private fun AmountOrMask(
    amount: Double,
    currency: String,
    hidden: Boolean,
    density: Float,
    color: ColorProvider,
    large: Boolean,
    pipDp: Float
) {
    if (hidden) {
        Image(
            provider = ImageProvider(maskedMoneyBitmap(density, count = 6, pipDp = pipDp)),
            contentDescription = "Hidden amount",
            colorFilter = ColorFilter.tint(color),
            modifier = GlanceModifier.height(if (large) 16.dp else 12.dp)
        )
    } else {
        Text(
            text = formatWidgetMoney(currency, amount),
            style = TextStyle(
                color = color,
                fontSize = if (large) 28.sp else 18.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    currency: String,
    hidden: Boolean,
    density: Float
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = account.name,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 13.sp
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        if (hidden) {
            Image(
                provider = ImageProvider(maskedMoneyBitmap(density, count = 5, pipDp = 7f)),
                contentDescription = "Hidden amount",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                modifier = GlanceModifier.height(10.dp)
            )
        } else {
            Text(
                text = formatWidgetMoney(currency, account.currentBalance),
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
