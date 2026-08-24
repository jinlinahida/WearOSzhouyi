package com.boompala.ui

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.SixSpirit
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultScreenTest {
    @Test
    fun `result display reverses positions without changing line associations`() {
        val linesFromBottom = YaoPosition.entries.mapIndexed { index, position ->
            Yao(
                position = position,
                yinYang = if (index % 2 == 0) YaoPolarity.YANG else YaoPolarity.YIN,
                moving = position == YaoPosition.SECOND,
                heavenlyStem = HeavenlyStem.JIA,
                earthlyBranch = EarthlyBranch.ZI,
                element = EarthlyBranch.ZI.element,
                sixRelation = SixRelation.entries[index % SixRelation.entries.size],
                sixSpirit = SixSpirit.entries[index],
                isShi = position == YaoPosition.THIRD,
                isYing = position == YaoPosition.TOP,
                isVoid = false,
                lineText = "${position.displayName}爻辞",
            )
        }

        val displayed = linesFromBottom.forResultDisplay()

        assertEquals(YaoPosition.entries.reversed(), displayed.map(Yao::position))
        displayed.forEach { displayedLine ->
            assertSame(
                linesFromBottom.single { it.position == displayedLine.position },
                displayedLine,
            )
        }
        assertEquals(YaoPosition.THIRD, displayed.single(Yao::isShi).position)
        assertEquals(YaoPosition.TOP, displayed.single(Yao::isYing).position)
        assertEquals(YaoPosition.SECOND, displayed.single(Yao::moving).position)
        assertEquals(
            "二爻爻辞",
            displayed.single { it.position == YaoPosition.SECOND }.lineText,
        )
    }

    @Test
    fun `moving line at first position renders at visual bottom without semantic corruption`() {
        val linesFromBottom = YaoPosition.entries.mapIndexed { index, position ->
            Yao(
                position = position,
                yinYang = YaoPolarity.YANG,
                moving = position == YaoPosition.FIRST,
                heavenlyStem = HeavenlyStem.JIA,
                earthlyBranch = EarthlyBranch.ZI,
                element = EarthlyBranch.ZI.element,
                sixRelation = SixRelation.SIBLINGS,
                sixSpirit = SixSpirit.AZURE_DRAGON,
                isShi = false,
                isYing = false,
                isVoid = false,
            )
        }

        val displayed = linesFromBottom.forResultDisplay()

        // Visual index 0 is TOP (上爻), not moving
        assertEquals(YaoPosition.TOP, displayed[0].position)
        assertFalse(displayed[0].moving)

        // Visual index 5 is FIRST (初爻), moving
        assertEquals(YaoPosition.FIRST, displayed[5].position)
        assertTrue(displayed[5].moving)
    }

    @Test
    fun `moving line at top position renders at visual top without semantic corruption`() {
        val linesFromBottom = YaoPosition.entries.mapIndexed { index, position ->
            Yao(
                position = position,
                yinYang = YaoPolarity.YANG,
                moving = position == YaoPosition.TOP,
                heavenlyStem = HeavenlyStem.JIA,
                earthlyBranch = EarthlyBranch.ZI,
                element = EarthlyBranch.ZI.element,
                sixRelation = SixRelation.SIBLINGS,
                sixSpirit = SixSpirit.AZURE_DRAGON,
                isShi = false,
                isYing = false,
                isVoid = false,
            )
        }

        val displayed = linesFromBottom.forResultDisplay()

        // Visual index 0 is TOP (上爻), moving
        assertEquals(YaoPosition.TOP, displayed[0].position)
        assertTrue(displayed[0].moving)

        // Visual index 5 is FIRST (初爻), not moving
        assertEquals(YaoPosition.FIRST, displayed[5].position)
        assertFalse(displayed[5].moving)
    }

    @Test
    fun `line display keeps yin yang shape independent from moving state for all four casts`() {
        val expected = mapOf(
            YaoState.OLD_YIN to YaoLineShape.BROKEN,
            YaoState.YOUNG_YANG to YaoLineShape.SOLID,
            YaoState.YOUNG_YIN to YaoLineShape.BROKEN,
            YaoState.OLD_YANG to YaoLineShape.SOLID,
        )

        expected.forEach { (state, expectedShape) ->
            val display = yaoFor(state).toLineDisplay()

            assertEquals("${state.numericValue} should keep its yin-yang shape", expectedShape, display.shape)
            assertEquals(state.isChanging, display.isMoving)
        }
    }

    @Test
    fun `hexagram line display dimensions maintain symmetry between solid and broken lines`() {
        val totalLineWidth = 64
        val gapWidth = 10
        val segmentWidth = 27

        assertEquals(
            "Broken line two segments + gap must equal total solid line width",
            totalLineWidth,
            segmentWidth * 2 + gapWidth,
        )
    }

    private fun yaoFor(state: YaoState): Yao = Yao(
        position = YaoPosition.FIRST,
        yinYang = if (state.isYang) YaoPolarity.YANG else YaoPolarity.YIN,
        moving = state.isChanging,
        heavenlyStem = HeavenlyStem.JIA,
        earthlyBranch = EarthlyBranch.ZI,
        element = EarthlyBranch.ZI.element,
        sixRelation = SixRelation.SIBLINGS,
        sixSpirit = SixSpirit.AZURE_DRAGON,
        isShi = false,
        isYing = false,
        isVoid = false,
    )
}
