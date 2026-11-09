package codes.tashif.paisa.ui.debts.split

import codes.tashif.paisa.data.repository.DebtShare
import kotlin.math.abs
import kotlin.math.roundToLong

/** Someone in a split: a person already in Debts, or a new name to add on save. */
data class SplitPerson(val personId: Int?, val name: String) {
    /** Stable key for the custom amount fields. */
    val key: String get() = personId?.let { "p:$it" } ?: "n:${name.trim().lowercase()}"

    fun toShare(amount: Double) = DebtShare(personId, name.trim().takeIf { personId == null }, amount)
}

enum class SplitMode { EQUAL, CUSTOM }

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
    val customText: Map<String, String> = emptyMap()
) {
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

private fun SplitPlan.amountOf(key: String): Double =
    customText[key]?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let(::toPaise) ?: 0.0

internal fun toPaise(amount: Double): Double = (amount * 100).roundToLong() / 100.0

/** 1500.0 to "1500", 12.5 to "12.5", for prefilling an amount field. */
internal fun plain(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else toPaise(amount).toString()
