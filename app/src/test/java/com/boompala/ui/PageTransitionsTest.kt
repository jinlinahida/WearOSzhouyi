package com.boompala.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTransitionsTest {

    @Test
    fun hierarchyDepthFollowsScreenLevels() {
        assertEquals(0, AppScreen.HOME.hierarchyDepth())
        assertEquals(0, AppScreen.WELCOME.hierarchyDepth())

        // Level 1 primary screens
        assertEquals(1, AppScreen.YAO_INPUT.hierarchyDepth())
        assertEquals(1, AppScreen.MEIHUA_TIME.hierarchyDepth())
        assertEquals(1, AppScreen.XIAO_LIU_REN.hierarchyDepth())
        assertEquals(1, AppScreen.DAILY_FORTUNE.hierarchyDepth())
        assertEquals(1, AppScreen.TAROT_ONE_CARD.hierarchyDepth())
        assertEquals(1, AppScreen.TAROT_THREE_CARD.hierarchyDepth())
        assertEquals(1, AppScreen.TAROT_HOLY_TRIANGLE.hierarchyDepth())
        assertEquals(1, AppScreen.TAROT_CELTIC_CROSS.hierarchyDepth())
        assertEquals(1, AppScreen.DESTINY_CHART_MENU.hierarchyDepth())
        assertEquals(1, AppScreen.PULSE_MEASURE.hierarchyDepth())
        assertEquals(1, AppScreen.COMPASS.hierarchyDepth())
        assertEquals(1, AppScreen.ARCHIVES.hierarchyDepth())
        assertEquals(1, AppScreen.BROWSE.hierarchyDepth())
        assertEquals(1, AppScreen.SETTINGS.hierarchyDepth())

        // Level 2 sub-browsers and results
        assertEquals(2, AppScreen.RESULT.hierarchyDepth())
        assertEquals(2, AppScreen.MEIHUA_RESULT.hierarchyDepth())
        assertEquals(2, AppScreen.PULSE_RESULT.hierarchyDepth())
        assertEquals(2, AppScreen.ARCHIVE_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.ARCHIVE_TAG.hierarchyDepth())
        assertEquals(2, AppScreen.HEXAGRAM_BROWSER.hierarchyDepth())
        assertEquals(2, AppScreen.KNOWLEDGE_LIST.hierarchyDepth())
        assertEquals(2, AppScreen.TAROT_BROWSER.hierarchyDepth())
        assertEquals(2, AppScreen.BAZI_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.WESTERN_CHART_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.NUMEROLOGY_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.BONE_WEIGHT_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.NINE_STAR_DETAIL.hierarchyDepth())
        assertEquals(2, AppScreen.ABOUT.hierarchyDepth())

        // Level 3 item details
        assertEquals(3, AppScreen.HEXAGRAM_DETAIL.hierarchyDepth())
        assertEquals(3, AppScreen.KNOWLEDGE_DETAIL.hierarchyDepth())
        assertEquals(3, AppScreen.TAROT_CARD_DETAIL.hierarchyDepth())
    }

    @Test
    fun primaryScreensForwardAndBackwardDirections() {
        // Home -> Feature = FORWARD, Feature -> Home = BACKWARD
        listOf(
            AppScreen.YAO_INPUT,
            AppScreen.MEIHUA_TIME,
            AppScreen.XIAO_LIU_REN,
            AppScreen.DAILY_FORTUNE,
            AppScreen.TAROT_ONE_CARD,
            AppScreen.TAROT_THREE_CARD,
            AppScreen.TAROT_HOLY_TRIANGLE,
            AppScreen.TAROT_CELTIC_CROSS,
            AppScreen.PULSE_MEASURE,
            AppScreen.COMPASS,
            AppScreen.ARCHIVES,
            AppScreen.BROWSE,
            AppScreen.SETTINGS,
        ).forEach { featureScreen ->
            assertEquals(
                "Home -> ${featureScreen.name} should be FORWARD",
                NavigationDirection.FORWARD,
                calculateNavigationDirection(AppScreen.HOME, featureScreen),
            )
            assertEquals(
                "${featureScreen.name} -> Home should be BACKWARD",
                NavigationDirection.BACKWARD,
                calculateNavigationDirection(featureScreen, AppScreen.HOME),
            )
        }
    }

    @Test
    fun divinationResultForwardAndBackwardDirections() {
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.YAO_INPUT, AppScreen.RESULT),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.RESULT, AppScreen.YAO_INPUT),
        )

        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.MEIHUA_TIME, AppScreen.MEIHUA_RESULT),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.MEIHUA_RESULT, AppScreen.MEIHUA_TIME),
        )
    }

    @Test
    fun browseDrillDownHierarchyDirections() {
        // Browse -> Hexagram Browser -> Hexagram Detail
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.BROWSE, AppScreen.HEXAGRAM_BROWSER),
        )
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.HEXAGRAM_BROWSER, AppScreen.HEXAGRAM_DETAIL),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.HEXAGRAM_DETAIL, AppScreen.HEXAGRAM_BROWSER),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.HEXAGRAM_BROWSER, AppScreen.BROWSE),
        )

        // Knowledge
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.BROWSE, AppScreen.KNOWLEDGE_LIST),
        )
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.KNOWLEDGE_LIST, AppScreen.KNOWLEDGE_DETAIL),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.KNOWLEDGE_DETAIL, AppScreen.KNOWLEDGE_LIST),
        )

        // Tarot
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.BROWSE, AppScreen.TAROT_BROWSER),
        )
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.TAROT_BROWSER, AppScreen.TAROT_CARD_DETAIL),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.TAROT_CARD_DETAIL, AppScreen.TAROT_BROWSER),
        )
    }

    @Test
    fun archiveAndSettingsDirections() {
        // Archives -> Detail
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.ARCHIVES, AppScreen.ARCHIVE_DETAIL),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.ARCHIVE_DETAIL, AppScreen.ARCHIVES),
        )

        // Settings -> About
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.SETTINGS, AppScreen.ABOUT),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(AppScreen.ABOUT, AppScreen.SETTINGS),
        )

        // About -> Welcome -> About
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.ABOUT, AppScreen.WELCOME),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(
                from = AppScreen.WELCOME,
                to = AppScreen.ABOUT,
                welcomeReturnScreen = AppScreen.ABOUT,
            ),
        )

        // Result -> Archive Tag -> Result
        assertEquals(
            NavigationDirection.FORWARD,
            calculateNavigationDirection(AppScreen.RESULT, AppScreen.ARCHIVE_TAG),
        )
        assertEquals(
            NavigationDirection.BACKWARD,
            calculateNavigationDirection(
                from = AppScreen.ARCHIVE_TAG,
                to = AppScreen.RESULT,
                archiveReturnScreen = AppScreen.RESULT,
            ),
        )
    }

    @Test
    fun sameScreenReturnsLateral() {
        assertEquals(
            NavigationDirection.LATERAL,
            calculateNavigationDirection(AppScreen.HOME, AppScreen.HOME),
        )
    }

    @Test
    fun archiveTagReturnDirectionsForEverySource() {
        // 任何来源页进入归档命名页都是前进；返回来源页都是后退。
        listOf(
            AppScreen.RESULT,
            AppScreen.MEIHUA_RESULT,
            AppScreen.XIAO_LIU_REN,
            AppScreen.TAROT_ONE_CARD,
            AppScreen.TAROT_THREE_CARD,
            AppScreen.TAROT_HOLY_TRIANGLE,
            AppScreen.TAROT_CELTIC_CROSS,
        ).forEach { source ->
            assertEquals(
                "$source -> ARCHIVE_TAG should be FORWARD",
                NavigationDirection.FORWARD,
                calculateNavigationDirection(source, AppScreen.ARCHIVE_TAG),
            )
            assertEquals(
                "ARCHIVE_TAG -> $source should be BACKWARD",
                NavigationDirection.BACKWARD,
                calculateNavigationDirection(
                    from = AppScreen.ARCHIVE_TAG,
                    to = source,
                    archiveReturnScreen = source,
                ),
            )
        }
    }

    @Test
    fun disabledAnimationsReturnNoneTransition() {
        val forwardTransform = pageTransitionSpec(NavigationDirection.FORWARD, animationsEnabled = false)
        assertNotNull(forwardTransform)
        assertEquals(EnterTransition.None, forwardTransform.targetContentEnter)
        assertEquals(ExitTransition.None, forwardTransform.initialContentExit)

        val backwardTransform = pageTransitionSpec(NavigationDirection.BACKWARD, animationsEnabled = false)
        assertNotNull(backwardTransform)
        assertEquals(EnterTransition.None, backwardTransform.targetContentEnter)
        assertEquals(ExitTransition.None, backwardTransform.initialContentExit)
    }

    @Test
    fun loadingContentTransitionHonorsAnimationsSetting() {
        val disabled = loadingContentTransitionSpec(animationsEnabled = false)
        assertNotNull(disabled)
        assertEquals(EnterTransition.None, disabled.targetContentEnter)
        assertEquals(ExitTransition.None, disabled.initialContentExit)

        val enabled = loadingContentTransitionSpec(animationsEnabled = true)
        assertNotNull(enabled)
        assertTrue(enabled.targetContentEnter != EnterTransition.None)
        assertTrue(enabled.initialContentExit != ExitTransition.None)
    }

    @Test
    fun emphasizedDecelEasingHasStandardEndpoints() {
        // Material emphasized-decelerate 曲线：起点 0、终点 1，中间单调递增。
        assertEquals(0f, EmphasizedDecelEasing.transform(0f), 0.0001f)
        assertEquals(1f, EmphasizedDecelEasing.transform(1f), 0.0001f)
        assertTrue(EmphasizedDecelEasing.transform(0.25f) > 0f)
        assertTrue(EmphasizedDecelEasing.transform(0.5f) > EmphasizedDecelEasing.transform(0.25f))
        assertTrue(EmphasizedDecelEasing.transform(0.75f) > EmphasizedDecelEasing.transform(0.5f))
    }

    @Test
    fun enabledAnimationsReturnNonEmptyTransition() {
        val forwardTransform = pageTransitionSpec(NavigationDirection.FORWARD, animationsEnabled = true)
        assertNotNull(forwardTransform)
        assertTrue(forwardTransform.targetContentZIndex == 1f)

        val backwardTransform = pageTransitionSpec(NavigationDirection.BACKWARD, animationsEnabled = true)
        assertNotNull(backwardTransform)
        assertTrue(backwardTransform.targetContentZIndex == 0f)

        val lateralTransform = pageTransitionSpec(NavigationDirection.LATERAL, animationsEnabled = true)
        assertNotNull(lateralTransform)
    }

    @Test
    fun repeatedCyclesMaintainConsistentDirection() {
        // Simulating 50 consecutive cycles of A -> B -> A -> B
        val pairs = listOf(
            AppScreen.HOME to AppScreen.SETTINGS,
            AppScreen.HOME to AppScreen.BROWSE,
            AppScreen.BROWSE to AppScreen.HEXAGRAM_BROWSER,
            AppScreen.HEXAGRAM_BROWSER to AppScreen.HEXAGRAM_DETAIL,
            AppScreen.YAO_INPUT to AppScreen.RESULT,
        )

        for ((screenA, screenB) in pairs) {
            for (i in 0 until 50) {
                val forwardDir = calculateNavigationDirection(screenA, screenB)
                assertEquals("Cycle $i: $screenA -> $screenB must be FORWARD", NavigationDirection.FORWARD, forwardDir)

                val forwardSpec = pageTransitionSpec(forwardDir, animationsEnabled = true)
                assertEquals(1f, forwardSpec.targetContentZIndex)

                val backwardDir = calculateNavigationDirection(screenB, screenA)
                assertEquals("Cycle $i: $screenB -> $screenA must be BACKWARD", NavigationDirection.BACKWARD, backwardDir)

                val backwardSpec = pageTransitionSpec(backwardDir, animationsEnabled = true)
                assertEquals(0f, backwardSpec.targetContentZIndex)
            }
        }
    }
}
