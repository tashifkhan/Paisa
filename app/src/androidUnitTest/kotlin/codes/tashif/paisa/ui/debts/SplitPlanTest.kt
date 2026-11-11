package codes.tashif.paisa.ui.debts

import codes.tashif.paisa.ui.debts.split.SplitMode
import codes.tashif.paisa.ui.debts.split.SplitPerson
import codes.tashif.paisa.ui.debts.split.SplitPlan
import codes.tashif.paisa.ui.debts.split.SplitProblem
import codes.tashif.paisa.ui.debts.split.billEntries
import codes.tashif.paisa.ui.debts.split.compute
import codes.tashif.paisa.ui.debts.split.computeBill
import codes.tashif.paisa.ui.debts.split.equalShares
import codes.tashif.paisa.ui.debts.split.toCustom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The split maths: equal shares, custom amounts, and bills someone else paid. */
class SplitPlanTest {

    private val radhika = SplitPerson(1, "Radhika")
    private val omar = SplitPerson(2, "Omar")

    @Test
    fun equalSharesAddUpToTheTotalExactly() {
        assertEquals(listOf(33.34, 33.33, 33.33), equalShares(100.0, 3))
        assertEquals(listOf(0.34, 0.33, 0.33), equalShares(1.0, 3))
        assertEquals(10_000L, equalShares(100.0, 7).sumOf { Math.round(it * 100) })
        assertTrue(equalShares(100.0, 0).isEmpty())
    }

    @Test
    fun equalSplitWithMeGivesMeTheLastShare() {
        val outcome = SplitPlan(people = listOf(radhika, omar)).compute(100.0)
        assertEquals(listOf(33.34, 33.33), outcome.shares.map { it.amount })
        assertEquals(33.33, outcome.myShare, 0.0)
        assertTrue(outcome.isValid)

        val withoutMe = SplitPlan(people = listOf(radhika, omar), includeMe = false).compute(100.0)
        assertEquals(listOf(50.0, 50.0), withoutMe.shares.map { it.amount })
        assertEquals(0.0, withoutMe.myShare, 0.0)
    }

    @Test
    fun customSplitShowsWhatIsLeftAndBlocksOverOrNothing() {
        val base = SplitPlan(people = listOf(radhika, omar), mode = SplitMode.CUSTOM)

        val partial = base.copy(customText = mapOf(radhika.key to "300", SplitPlan.ME_KEY to "200")).compute(1000.0)
        assertEquals(500.0, partial.left, 0.001)
        assertNull(partial.problem)

        val over = base.copy(customText = mapOf(radhika.key to "700", omar.key to "400")).compute(1000.0)
        assertEquals(-100.0, over.left, 0.001)
        assertEquals(SplitProblem.OVER_TOTAL, over.problem)

        assertEquals(SplitProblem.NOTHING_ASSIGNED, base.compute(1000.0).problem)
        // Only your own share filled in still means nobody owes you anything.
        assertEquals(SplitProblem.NOTHING_ASSIGNED, base.copy(customText = mapOf(SplitPlan.ME_KEY to "50")).compute(1000.0).problem)
        assertEquals(SplitProblem.NO_TOTAL, base.compute(null).problem)
        assertEquals(SplitProblem.NO_PEOPLE, SplitPlan().compute(1000.0).problem)
    }

    @Test
    fun aBillSomeoneElsePaidRecordsMinusMyShareOnThem() {
        val plan = SplitPlan(people = listOf(radhika, omar))
        val outcome = plan.computeBill(900.0, payer = radhika)
        assertTrue(outcome.isValid)
        val entries = outcome.billEntries(radhika)
        assertEquals(1, entries.size)
        assertEquals(1, entries.single().personId)
        assertEquals(-300.0, entries.single().amount, 0.001)

        // If I paid, everyone else owes me their share.
        val iPaid = plan.computeBill(900.0, payer = null).billEntries(null)
        assertEquals(listOf(300.0, 300.0), iPaid.map { it.amount })

        // Someone else paying needs me in the split.
        assertEquals(SplitProblem.NEEDS_ME, plan.copy(includeMe = false).computeBill(900.0, radhika).problem)
    }

    @Test
    fun savedSharesComeBackAsTheSamePlan() {
        val equal = SplitPlan.fromShares(100.0, listOf(radhika to 33.34, omar to 33.33))
        assertEquals(SplitMode.EQUAL, equal.mode)
        assertTrue(equal.includeMe)

        val custom = SplitPlan.fromShares(1000.0, listOf(radhika to 600.0, omar to 100.0))
        assertEquals(SplitMode.CUSTOM, custom.mode)
        assertEquals("300", custom.customText[SplitPlan.ME_KEY])
        assertEquals(listOf(600.0, 100.0), custom.compute(1000.0).shares.map { it.amount })

        val newName = SplitPerson(null, "Kabir")
        assertEquals("n:kabir", newName.key)
        assertEquals("Kabir", newName.toShare(10.0).newName)
    }

    @Test
    fun switchingToCustomStartsFromTheEqualSplit() {
        val equal = SplitPlan(people = listOf(radhika, omar))
        val custom = equal.toCustom(equal.compute(1200.0))
        assertEquals(SplitMode.CUSTOM, custom.mode)
        val outcome = custom.compute(1200.0)
        assertEquals(listOf(400.0, 400.0), outcome.shares.map { it.amount })
        assertEquals(400.0, outcome.myShare, 0.001)
        assertEquals(0.0, outcome.left, 0.001)
    }
}
