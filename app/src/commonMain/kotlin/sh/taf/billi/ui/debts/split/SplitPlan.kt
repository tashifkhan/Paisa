package sh.taf.billi.ui.debts.split

import sh.taf.billi.data.repository.DebtShare
import kotlin.math.abs
import kotlin.math.roundToLong

/** Someone in a split: a person already in Debts, or a new name to add on save. */
data class SplitPerson(val personId: Int?, val name: String) {
    /** Stable key for the custom amount fields. */
    val key: String get() = personId?.let { "p:$it" } ?: "n:${name.trim().lowercase()}"

    fun toShare(amount: Double) = DebtShare(personId, name.trim().takeIf { personId == null }, amount)
}

enum class SplitMode { EQUAL, CUSTOM, PERCENT, ITEMS }

/**
 * One line of a bill. [who] holds the [SplitPerson.key]s sharing it, and
 * [SplitPlan.ME_KEY] for you. Several people on one item split it equally.
 */
data class BillItem(val name: String = "", val amountText: String = "", val who: Set<String> = emptySet()) {
    val amount: Double get() = amountText.toDoubleOrNull()?.takeIf { it > 0.0 }?.let(::toPaise) ?: 0.0
}

/** Why a split can't be saved yet. */
enum class SplitProblem { NO_TOTAL, NO_PEOPLE, NOTHING_ASSIGNED, OVER_TOTAL, NEEDS_ME }

/**
 * What the split editor holds. Plain data, so a screen can keep it in state
 * and the maths can be tested without Compose.
 */
data class SplitPlan(
    val people: List<SplitPerson> = emptyList(),
    val includeMe: Boolean = true,
    val mode: SplitMode = SplitMode.EQUAL,
    /** Custom amount text per [SplitPerson.key], and [ME_KEY] for your own share. */
    val customText: Map<String, String> = emptyMap(),
    /** Percent text per key, for [SplitMode.PERCENT]. */
    val percentText: Map<String, String> = emptyMap(),
    /** The bill's lines, for [SplitMode.ITEMS]. */
    val items: List<BillItem> = emptyList()
) {
    /** Everyone in the split, you last, as keys. */
    val keys: List<String> get() = people.map { it.key } + if (includeMe) listOf(ME_KEY) else emptyList()

    /** What the bill's items add up to, before tax and other charges. */
    val itemsTotal: Double get() = toPaise(items.sumOf { it.amount })

    /** Percent typed so far, across everyone in the split. */
    val percentTotal: Double get() = keys.sumOf { percentOf(it) }

    fun has(person: SplitPerson) = people.any { it.key == person.key }

    fun toggle(person: SplitPerson): SplitPlan =
        if (has(person)) copy(people = people.filterNot { it.key == person.key }) else copy(people = people + person)

    companion object {
        const val ME_KEY = "me"

        /**
         * Rebuilds a plan from saved shares. Shares that match an equal split come
         * back as Equal; anything else comes back as Custom, with what's left of
         * [total] as your share.
         */
        fun fromShares(total: Double, shares: List<Pair<SplitPerson, Double>>): SplitPlan {
            val people = shares.map { it.first }
            if (people.isEmpty()) return SplitPlan()
            val amounts = shares.map { it.second }
            for (includeMe in listOf(true, false)) {
                val equal = equalShares(total, people.size + if (includeMe) 1 else 0).take(people.size)
                if (equal.zip(amounts).all { (a, b) -> abs(a - b) < 0.005 }) {
                    return SplitPlan(people = people, includeMe = includeMe)
                }
            }
            val mine = toPaise(total - amounts.sum())
            val text = shares.associate { (p, a) -> p.key to plain(a) } +
                if (mine > 0.0) mapOf(ME_KEY to plain(mine)) else emptyMap()
            return SplitPlan(people = people, includeMe = mine > 0.0, mode = SplitMode.CUSTOM, customText = text)
        }
    }
}

/** One person's part of a worked-out split. */
data class SplitShare(val person: SplitPerson, val amount: Double)

/**
 * A worked-out split. [left] is what's not assigned yet (negative when the
 * shares are over the total). [problem] is null when it can be saved.
 */
data class SplitOutcome(
    val shares: List<SplitShare>,
    val myShare: Double,
    val left: Double,
    val problem: SplitProblem?
) {
    val isValid: Boolean get() = problem == null

    /** What the people in the split owe you, for a transaction or a bill you paid. */
    fun owedToMe(): List<DebtShare> = shares.filter { it.amount > 0.0 }.map { it.person.toShare(it.amount) }

    /** The total this was worked out against. Null when there was no total. */
    fun total(): Double? = toPaise(shares.sumOf { it.amount } + myShare + left).takeIf { it > 0.0 }
}

/**
 * Divides [total] into [count] shares in whole paise. Leftover paise go to the
 * first shares, so they always add up to the total exactly.
 */
fun equalShares(total: Double, count: Int): List<Double> {
    if (count <= 0) return emptyList()
    val paise = (total * 100).roundToLong()
    val base = paise / count
    val extra = paise % count
    return List(count) { i -> (base + if (i < extra) 1 else 0) / 100.0 }
}

/** Works out each share of [total]. You come after the other people, so you get the smaller share. */
fun SplitPlan.compute(total: Double?): SplitOutcome {
    val sum = total?.takeIf { it > 0.0 } ?: 0.0
    val outcome = when (mode) {
        SplitMode.EQUAL -> {
            val all = equalShares(sum, people.size + if (includeMe) 1 else 0)
            SplitOutcome(
                shares = people.mapIndexed { i, p -> SplitShare(p, all[i]) },
                myShare = if (includeMe && all.isNotEmpty()) all.last() else 0.0,
                left = 0.0,
                problem = null
            )
        }
        SplitMode.CUSTOM -> {
            val shares = people.map { SplitShare(it, amountOf(it.key)) }
            val mine = if (includeMe) amountOf(SplitPlan.ME_KEY) else 0.0
            SplitOutcome(shares, mine, toPaise(sum - shares.sumOf { it.amount } - mine), null)
        }
        SplitMode.PERCENT -> fromAmounts(sum, percentAmounts(sum))
        SplitMode.ITEMS -> fromAmounts(sum, itemAmounts(sum))
    }
    val problem = when {
        total == null || total <= 0.0 -> SplitProblem.NO_TOTAL
        people.isEmpty() -> SplitProblem.NO_PEOPLE
        outcome.left < -0.004 -> SplitProblem.OVER_TOTAL
        outcome.shares.none { it.amount > 0.0 } -> SplitProblem.NOTHING_ASSIGNED
        else -> null
    }
    return outcome.copy(problem = problem)
}

/**
 * Entries for a bill split with no transaction behind it. If you paid
 * ([payer] null), each other person owes you their share. If [payer] paid,
 * you owe them your share, and the others owe the payer, not you, so
 * nothing is recorded for them. Amounts are signed: > 0 they owe you.
 */
fun SplitOutcome.billEntries(payer: SplitPerson?): List<DebtShare> {
    if (payer == null) return owedToMe()
    return if (myShare > 0.0) listOf(payer.toShare(-myShare)) else emptyList()
}

/** Like [compute], with the extra rule for a bill: if someone else paid, you must be in it. */
fun SplitPlan.computeBill(total: Double?, payer: SplitPerson?): SplitOutcome {
    val outcome = compute(total)
    if (payer == null || outcome.problem == SplitProblem.NO_TOTAL) return outcome
    return when {
        !includeMe -> outcome.copy(problem = SplitProblem.NEEDS_ME)
        outcome.problem == SplitProblem.OVER_TOTAL -> outcome
        outcome.myShare <= 0.0 -> outcome.copy(problem = SplitProblem.NOTHING_ASSIGNED)
        else -> outcome.copy(problem = null)
    }
}

/**
 * Switches to Custom amounts, starting every field from the equal split in
 * [current], the numbers the person was just looking at.
 */
fun SplitPlan.toCustom(current: SplitOutcome): SplitPlan {
    val filled = current.shares.associate { it.person.key to plain(it.amount) } +
        if (includeMe && current.myShare > 0.0) mapOf(SplitPlan.ME_KEY to plain(current.myShare)) else emptyMap()
    return copy(mode = SplitMode.CUSTOM, customText = filled)
}

private fun SplitPlan.fromAmounts(total: Double, byKey: Map<String, Double>): SplitOutcome {
    val shares = people.map { SplitShare(it, byKey[it.key] ?: 0.0) }
    val mine = if (includeMe) byKey[SplitPlan.ME_KEY] ?: 0.0 else 0.0
    return SplitOutcome(shares, mine, toPaise(total - shares.sumOf { it.amount } - mine), null)
}

internal fun SplitPlan.percentOf(key: String): Double =
    percentText[key]?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0

/**
 * Each person's percent of [total], in whole paise. When the percents come to
 * 100 the amounts add up to the total exactly, whatever the rounding.
 */
private fun SplitPlan.percentAmounts(total: Double): Map<String, Double> {
    val weights = keys.associateWith { percentOf(it) }
    val percent = weights.values.sum()
    val whole = abs(percent - 100.0) < 0.005
    val target = if (whole) total else total * percent / 100.0
    return proportional(target, weights)
}

/**
 * Each person's part of an itemised bill of [total]. An item is divided
 * equally between the people on it. Tax, tip and discounts are whatever
 * [total] has beyond the items, and each person takes them in proportion to
 * their items: someone who ate 40% of the food pays 40% of the tax. Items
 * nobody is on stay unassigned, along with their part of the tax.
 */
private fun SplitPlan.itemAmounts(total: Double): Map<String, Double> {
    val active = keys
    val subtotal = active.associateWith { 0.0 }.toMutableMap()
    for (item in items) {
        val sharers = active.filter { it in item.who }
        if (item.amount <= 0.0 || sharers.isEmpty()) continue
        // Exact shares here. Rounding twice (once per item, again for tax)
        // can leave people a couple of paise apart on an even split.
        val each = item.amount / sharers.size
        for (key in sharers) subtotal[key] = subtotal.getValue(key) + each
    }
    val all = itemsTotal
    if (all <= 0.0) return emptyMap()
    val assigned = subtotal.values.sum()
    if (assigned <= 0.0) return emptyMap()
    val target = if (abs(assigned - all) < 0.001) total else total * assigned / all
    return proportional(target, subtotal)
}

/** Each person's share of the bill's items, as a percent. This is how the tax is divided. */
fun SplitPlan.itemPercents(): Map<String, Double> {
    val amounts = itemAmounts(100.0)
    return keys.associateWith { amounts[it] ?: 0.0 }
}

/**
 * Divides [target] between the keys by [weights], in whole paise. Paise lost
 * to rounding go to the largest remainders, so the parts add up to [target].
 */
private fun proportional(target: Double, weights: Map<String, Double>): Map<String, Double> {
    val sum = weights.values.sum()
    if (sum <= 0.0 || target <= 0.0) return weights.mapValues { 0.0 }
    val paise = (target * 100).roundToLong()
    val exact = weights.mapValues { paise * it.value / sum }
    val floor = exact.mapValues { kotlin.math.floor(it.value + 1e-9).toLong() }.toMutableMap()
    var spare = paise - floor.values.sum()
    for (key in exact.keys.sortedByDescending { exact.getValue(it) - floor.getValue(it) }) {
        if (spare <= 0) break
        floor[key] = floor.getValue(key) + 1
        spare--
    }
    return floor.mapValues { it.value / 100.0 }
}

/** Switches to Percent, starting from the shares in [current]. */
fun SplitPlan.toPercent(current: SplitOutcome, total: Double?): SplitPlan {
    val sum = total?.takeIf { it > 0.0 }
    val amounts = current.shares.associate { it.person.key to it.amount } +
        if (includeMe) mapOf(SplitPlan.ME_KEY to current.myShare) else emptyMap()
    val assigned = amounts.values.sum()
    val filled = if (sum == null || assigned <= 0.0) {
        // Nothing to go on yet: an even split.
        val each = if (keys.isEmpty()) 0.0 else 100.0 / keys.size
        keys.associateWith { percentText(each) }
    } else {
        amounts.mapValues { percentText(it.value / sum * 100.0) }
    }
    return copy(mode = SplitMode.PERCENT, percentText = filled)
}

/** 33.333 to "33.33", 50.0 to "50". */
internal fun percentText(percent: Double): String {
    val hundredths = (percent * 100).roundToLong()
    return if (hundredths % 100 == 0L) (hundredths / 100).toString() else (hundredths / 100.0).toString()
}

private fun SplitPlan.amountOf(key: String): Double =
    customText[key]?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let(::toPaise) ?: 0.0

internal fun toPaise(amount: Double): Double = (amount * 100).roundToLong() / 100.0

/** 1500.0 to "1500", 12.5 to "12.5", for prefilling an amount field. */
internal fun plain(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else toPaise(amount).toString()
