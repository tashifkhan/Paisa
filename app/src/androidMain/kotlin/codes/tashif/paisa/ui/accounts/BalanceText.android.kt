package codes.tashif.paisa.ui.accounts

internal actual fun manualBalanceInputText(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
