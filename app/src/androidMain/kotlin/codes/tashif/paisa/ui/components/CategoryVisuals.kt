package codes.tashif.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Categories store a material icon name string (e.g. "restaurant") and a hex
 * color. These helpers turn that into a consistent icon + tinted circle
 * everywhere categories appear.
 */
object CategoryVisuals {

    private val iconsByName: Map<String, ImageVector> = mapOf(
        "payments" to Icons.Rounded.Payments,
        "storefront" to Icons.Rounded.Storefront,
        "trending_up" to Icons.AutoMirrored.Rounded.TrendingUp,
        "card_giftcard" to Icons.Rounded.CardGiftcard,
        "computer" to Icons.Rounded.Computer,
        "receipt_long" to Icons.Rounded.ReceiptLong,
        "savings" to Icons.Rounded.Savings,
        "account_balance" to Icons.Rounded.AccountBalance,
        "redeem" to Icons.Rounded.Redeem,
        "more_horiz" to Icons.Rounded.MoreHoriz,
        "restaurant" to Icons.Rounded.Restaurant,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "medical_services" to Icons.Rounded.MedicalServices,
        "sports_esports" to Icons.Rounded.SportsEsports,
        "shopping_cart" to Icons.Rounded.ShoppingCart,
        "home" to Icons.Rounded.Home,
        "school" to Icons.Rounded.School,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "flight" to Icons.Rounded.Flight,
        "shield" to Icons.Rounded.Shield,
        "local_gas_station" to Icons.Rounded.LocalGasStation,
        "swap_horiz" to Icons.Rounded.SwapHoriz,
        "credit_card" to Icons.Rounded.CreditCard,
        "account_balance_wallet" to Icons.Rounded.AccountBalanceWallet,
        "spa" to Icons.Rounded.Spa,
        "wallet" to Icons.Rounded.Wallet,
        "receipt" to Icons.Rounded.Receipt,
        "category" to Icons.Rounded.Category
    )

    /** Keyword → icon name, used to pick icons for user-created categories. */
    private val keywordIcons = listOf(
        listOf("food", "dining", "restaurant", "lunch", "dinner", "cafe", "coffee") to "restaurant",
        listOf("grocer", "supermarket", "kirana") to "shopping_cart",
        listOf("shop", "cloth", "fashion", "amazon", "flipkart") to "shopping_bag",
        listOf("transport", "cab", "taxi", "uber", "ola", "metro", "bus", "car") to "directions_car",
        listOf("fuel", "petrol", "gas", "diesel") to "local_gas_station",
        listOf("bill", "utilit", "electric", "water", "recharge") to "receipt_long",
        listOf("health", "fitness", "medic", "doctor", "pharma", "gym") to "medical_services",
        listOf("entertain", "game", "movie", "music") to "sports_esports",
        listOf("rent", "hous", "home", "maintenance") to "home",
        listOf("educat", "school", "course", "tuition", "book") to "school",
        listOf("subscri", "netflix", "spotify", "prime") to "subscriptions",
        listOf("travel", "flight", "trip", "hotel", "vacation") to "flight",
        listOf("insur", "shield", "protect") to "shield",
        listOf("upi", "transfer", "swap") to "swap_horiz",
        listOf("credit card") to "credit_card",
        listOf("bank") to "account_balance",
        listOf("care", "spa", "salon", "beauty") to "spa",
        listOf("tax") to "receipt",
        listOf("invest", "stock", "mutual", "sip", "trading") to "trending_up",
        listOf("salary", "income", "pay") to "payments",
        listOf("business", "store") to "storefront",
        listOf("gift") to "card_giftcard",
        listOf("freelance", "computer", "tech") to "computer",
        listOf("refund", "reimburse") to "receipt_long",
        listOf("interest", "saving") to "savings",
        listOf("dividend") to "account_balance",
        listOf("cashback", "reward", "redeem") to "redeem",
        listOf("cash", "wallet") to "wallet"
    )

    /** Best icon-name guess for a category name; used when creating categories. */
    fun suggestIconName(categoryName: String): String {
        val lower = categoryName.lowercase()
        return keywordIcons.firstOrNull { (keywords, _) ->
            keywords.any { lower.contains(it) }
        }?.second ?: "category"
    }

    /** Resolve a stored icon name (falling back to the category name) to a vector. */
    fun icon(iconName: String?, categoryName: String? = null): ImageVector {
        iconName?.let { iconsByName[it] }?.let { return it }
        categoryName?.let { iconsByName[suggestIconName(it)] }?.let { return it }
        return Icons.Rounded.Category
    }

    fun color(hex: String?): Color? =
        hex?.let {
            runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
        }
}

/** Circular tinted badge with the category's icon. */
@Composable
fun CategoryBadge(
    iconName: String?,
    categoryName: String,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val tint = CategoryVisuals.color(colorHex)
        ?: MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .size(size)
            .background(tint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.icon(iconName, categoryName),
            contentDescription = null,
            // Pastel category swatches read best with a soft dark glyph
            tint = Color.Black.copy(alpha = 0.62f),
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
