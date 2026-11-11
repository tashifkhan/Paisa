package codes.tashif.paisa.ui.accounts

/**
 * Expands the shortest round-trip digits of [value] (which may use an
 * exponent, like "1.0E21") into plain decimal text, then drops trailing zeros.
 */
internal actual fun manualBalanceInputText(value: Double): String {
    val text = value.toString()
    val negative = text.startsWith("-")
    val unsigned = text.removePrefix("-")
    val (mantissa, exponent) = unsigned.uppercase().split("E").let { it[0] to (it.getOrNull(1)?.toInt() ?: 0) }
    val intPart = mantissa.substringBefore(".")
    val fracPart = mantissa.substringAfter(".", "")
    val digits = (intPart + fracPart).trimStart('0')
    if (digits.isEmpty()) return "0"
    // Position of the decimal point within the digits after shifting by the exponent.
    val leadingZeros = (intPart + fracPart).length - (intPart + fracPart).trimStart('0').length
    val point = intPart.length + exponent - leadingZeros
    val plain = when {
        point <= 0 -> "0." + "0".repeat(-point) + digits
        point >= digits.length -> digits + "0".repeat(point - digits.length)
        else -> digits.substring(0, point) + "." + digits.substring(point)
    }
    val trimmed = if (plain.contains('.')) plain.trimEnd('0').trimEnd('.') else plain
    return if (negative) "-$trimmed" else trimmed
}
