package sh.taf.billi.ui.debts.split

import sh.taf.billi.ai.BillScanParser
import sh.taf.billi.ai.ScannedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitModesTest {

    private val asha = SplitPerson(1, "Asha")
    private val ravi = SplitPerson(2, "Ravi")
    private val me = SplitPlan.ME_KEY

    private fun SplitOutcome.of(person: SplitPerson) = shares.first { it.person.key == person.key }.amount

    @Test
    fun percentSplitsTheTotal() {
        val plan = SplitPlan(
            people = listOf(asha, ravi), mode = SplitMode.PERCENT,
            percentText = mapOf(asha.key to "50", ravi.key to "30", me to "20")
        )
        val out = plan.compute(1000.0)
        assertEquals(500.0, out.of(asha))
        assertEquals(300.0, out.of(ravi))
        assertEquals(200.0, out.myShare)
        assertEquals(0.0, out.left)
        assertNull(out.problem)
    }

    @Test
    fun percentsThatMakeAHundredAddUpExactly() {
        val third = "33.33"
        val plan = SplitPlan(
            people = listOf(asha, ravi), mode = SplitMode.PERCENT,
            percentText = mapOf(asha.key to third, ravi.key to third, me to "33.34")
        )
        val out = plan.compute(100.01)
        assertEquals(100.01, toPaise(out.of(asha) + out.of(ravi) + out.myShare))
        assertEquals(0.0, out.left)
    }

    @Test
    fun percentUnderAndOver() {
        val under = SplitPlan(
            people = listOf(asha), mode = SplitMode.PERCENT, percentText = mapOf(asha.key to "25", me to "25")
        ).compute(200.0)
        assertEquals(50.0, under.of(asha))
        assertEquals(100.0, under.left)
        assertNull(under.problem)

        val over = SplitPlan(
            people = listOf(asha), mode = SplitMode.PERCENT, percentText = mapOf(asha.key to "80", me to "40")
        ).compute(200.0)
        assertEquals(SplitProblem.OVER_TOTAL, over.problem)
    }

    @Test
    fun switchingToPercentKeepsTheShares() {
        val equal = SplitPlan(people = listOf(asha, ravi, SplitPerson(3, "Meera")))
        val percent = equal.toPercent(equal.compute(1000.0), 1000.0)
        assertEquals(SplitMode.PERCENT, percent.mode)
        assertEquals("25", percent.percentText[asha.key])
        assertEquals("25", percent.percentText[me])
        assertEquals(100.0, percent.percentTotal)
        // No total yet: an even split.
        val blank = SplitPlan(people = listOf(asha)).let { it.toPercent(it.compute(null), null) }
        assertEquals("50", blank.percentText[asha.key])
    }

    @Test
    fun itemsGoToWhoHadThemAndTaxFollows() {
        // Items come to 1000. The bill is 1180 with tax and service charge.
        val plan = SplitPlan(
            people = listOf(asha, ravi), mode = SplitMode.ITEMS,
            items = listOf(
                BillItem("Biryani", "400", setOf(asha.key)),
                BillItem("Paneer", "300", setOf(ravi.key, me)),
                BillItem("Naan", "300", setOf(asha.key, ravi.key, me))
            )
        )
        // Asha 400 + 100, Ravi 150 + 100, me 150 + 100, each scaled by 1.18.
        val out = plan.compute(1180.0)
        assertEquals(590.0, out.of(asha))
        assertEquals(295.0, out.of(ravi))
        assertEquals(295.0, out.myShare)
        assertEquals(0.0, out.left)
        assertNull(out.problem)
        assertEquals(mapOf(asha.key to 50.0, ravi.key to 25.0, me to 25.0), plan.itemPercents())
    }

    @Test
    fun anItemNobodyTookStaysUnassigned() {
        val plan = SplitPlan(
            people = listOf(asha), mode = SplitMode.ITEMS,
            items = listOf(BillItem("Soup", "100", setOf(asha.key)), BillItem("Dessert", "100"))
        )
        val out = plan.compute(220.0)
        assertEquals(110.0, out.of(asha))
        assertEquals(0.0, out.myShare)
        assertEquals(110.0, out.left)
    }

    @Test
    fun oddPaiseStillAddUp() {
        val plan = SplitPlan(
            people = listOf(asha, ravi), mode = SplitMode.ITEMS,
            items = listOf(BillItem("Thali", "100", setOf(asha.key, ravi.key, me)))
        )
        val out = plan.compute(107.0)
        assertEquals(107.0, toPaise(out.of(asha) + out.of(ravi) + out.myShare))
        assertEquals(0.0, out.left)
        assertTrue(out.of(asha) - out.myShare < 0.011)
    }

    @Test
    fun aDiscountComesOffInProportionToo() {
        val plan = SplitPlan(
            people = listOf(asha), mode = SplitMode.ITEMS,
            items = listOf(BillItem("A", "300", setOf(asha.key)), BillItem("B", "100", setOf(me)))
        )
        val out = plan.compute(360.0)
        assertEquals(270.0, out.of(asha))
        assertEquals(90.0, out.myShare)
    }

    @Test
    fun someoneTakenOutOfTheSplitLosesTheirItems() {
        val plan = SplitPlan(
            people = listOf(asha), includeMe = false, mode = SplitMode.ITEMS,
            items = listOf(BillItem("A", "100", setOf(asha.key, ravi.key, me)))
        )
        val out = plan.compute(100.0)
        assertEquals(100.0, out.of(asha))
        assertEquals(0.0, out.myShare)
    }

    @Test
    fun noItemsMeansNothingAssigned() {
        val out = SplitPlan(people = listOf(asha), mode = SplitMode.ITEMS).compute(500.0)
        assertEquals(SplitProblem.NOTHING_ASSIGNED, out.problem)
    }

    @Test
    fun readsAScannedBill() {
        val bill = BillScanParser.parse(
            """```json
            {"items": [{"name": "Paneer tikka", "amount": 320}, {"name": "Lassi x2", "amount": "₹1,180.50"},
                       {"name": "", "price": 40}, {"name": "Free water", "amount": 0}],
             "total": "1,650.00"}
            ```"""
        )
        assertEquals(
            listOf(ScannedItem("Paneer tikka", 320.0), ScannedItem("Lassi x2", 1180.5), ScannedItem("Item 3", 40.0)),
            bill.items
        )
        assertEquals(1650.0, bill.total)
        assertTrue(BillScanParser.parse("sorry, I can't read that").items.isEmpty())
        assertNull(BillScanParser.parse("""{"items": [{"name": "Tea", "amount": 20}]}""").total)
    }
}
