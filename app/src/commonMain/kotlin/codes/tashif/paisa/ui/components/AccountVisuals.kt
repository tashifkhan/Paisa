package codes.tashif.paisa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon + color helpers for account types, mirroring [CategoryVisuals]. */
object AccountVisuals {
    fun icon(type: String?, iconName: String? = null): ImageVector {
        return when (type) {
            "Cash" -> Icons.Rounded.Payments
            "Credit Card", "Debit Card" -> Icons.Rounded.CreditCard
            "UPI" -> Icons.Rounded.PhoneAndroid
            "Wallet" -> Icons.Rounded.Wallet
            "Bank Account" -> Icons.Rounded.AccountBalance
            else -> CategoryVisuals.icon(iconName)
        }
    }

    fun color(hex: String?): Color? = CategoryVisuals.color(hex)
}
