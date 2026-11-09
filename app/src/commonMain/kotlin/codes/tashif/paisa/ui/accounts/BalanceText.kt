package codes.tashif.paisa.ui.accounts

/** [value] as plain decimal text with no trailing zeros and no exponent: "125.5", "0", "-42". */
internal expect fun manualBalanceInputText(value: Double): String
