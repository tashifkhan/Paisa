package codes.tashif.paisa.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitScalingTest {

    @Test
    fun hundredSplitThreeWaysScalesToOneAndBack() {
        val down = SplitScaling.scale(listOf(1 to 3334L, 2 to 3333L, 3 to 3333L), 100, 10_000)
        assertEquals(mapOf(1 to 34L, 2 to 33L, 3 to 33L), down)
        val up = SplitScaling.scale(down.toList(), 10_000, 100)
        assertEquals(10_000L, up.values.sum())
    }

    @Test
    fun tiesGoToTheLowerEntryId() {
        // 3 x 1 paisa scaled by 2/3: each is 0.666, total 2. Ids 1 and 2 get the paise.
        val out = SplitScaling.scale(listOf(3 to 1L, 1 to 1L, 2 to 1L), 2, 3)
        assertEquals(mapOf(3 to 0L, 1 to 1L, 2 to 1L), out)
    }

    @Test
    fun negativeEntriesMirrorPositiveOnes() {
        val out = SplitScaling.scale(listOf(1 to -3334L, 2 to -3333L, 3 to -3333L), 100, 10_000)
        assertEquals(mapOf(1 to -34L, 2 to -33L, 3 to -33L), out)
    }

    @Test
    fun aPartialSplitKeepsItsShareOfTheTotal() {
        // 400 + 400 of a 1200 bill, bill edited to 1000: 800 * 1000 / 1200 = 666.67 -> 66667 paise.
        val out = SplitScaling.scale(listOf(1 to 40_000L, 2 to 40_000L), 100_000, 120_000)
        assertEquals(66_667L, out.values.sum())
        assertEquals(mapOf(1 to 33_334L, 2 to 33_333L), out)
    }
}
