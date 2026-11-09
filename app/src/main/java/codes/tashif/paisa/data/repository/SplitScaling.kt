package codes.tashif.paisa.data.repository

/**
 * Rescales a transaction's split as a group, in whole paise, so no money is
 * lost to rounding each entry on its own. Pure.
 */
object SplitScaling {

    /**
     * Scales [entries] (entry id to paise) by [newPaise] / [oldPaise]. The new
     * total is the old total scaled and rounded half up. Each entry gets the
     * floor of its exact share, and the paise left over go one each to the
     * entries with the largest remainders, ties going to the lower entry id.
     */
    fun scale(entries: List<Pair<Int, Long>>, newPaise: Long, oldPaise: Long): Map<Int, Long> {
        require(oldPaise > 0L) { "oldPaise must be positive" }
        if (entries.isEmpty()) return emptyMap()
        val total = entries.sumOf { it.second }
        val target = roundHalfUp(total * newPaise, oldPaise)

        class Part(val id: Int, val floor: Long, val remainder: Long)
        val parts = entries.map { (id, paise) ->
            val product = paise * newPaise
            Part(id, Math.floorDiv(product, oldPaise), Math.floorMod(product, oldPaise))
        }
        var left = target - parts.sumOf { it.floor }
        val result = parts.associateTo(LinkedHashMap()) { it.id to it.floor }
        if (left > 0L) {
            val byRemainder = parts.sortedWith(compareByDescending<Part> { it.remainder }.thenBy { it.id })
            var i = 0
            while (left > 0L) {
                val part = byRemainder[i % byRemainder.size]
                result[part.id] = result.getValue(part.id) + 1
                left--
                i++
            }
        } else if (left < 0L) {
            val byRemainder = parts.sortedWith(compareBy<Part> { it.remainder }.thenBy { it.id })
            var i = 0
            while (left < 0L) {
                val part = byRemainder[i % byRemainder.size]
                result[part.id] = result.getValue(part.id) - 1
                left++
                i++
            }
        }
        return result
    }

    /** [num] / [den] rounded to the nearest whole number, halves away from zero. [den] > 0. */
    private fun roundHalfUp(num: Long, den: Long): Long {
        val q = Math.floorDiv(num, den)
        val r = Math.floorMod(num, den)
        return when {
            2 * r > den -> q + 1
            2 * r < den -> q
            num >= 0 -> q + 1
            else -> q
        }
    }
}
