package com.boompala.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScalingRotaryScrollColumnTest {
    @Test
    fun `resolveScalingParams uses standard fish-eye scaling when animations are enabled`() {
        val params = resolveScalingParams(animationsEnabled = true)
        assertEquals(0.7f, params.edgeScale, 0.01f)
        assertEquals(0.5f, params.edgeAlpha, 0.01f)
    }

    @Test
    fun `resolveScalingParams disables edge scaling and alpha reduction when animations are disabled`() {
        val params = resolveScalingParams(animationsEnabled = false)
        assertEquals(1.0f, params.edgeScale, 0.001f)
        assertEquals(1.0f, params.edgeAlpha, 0.001f)
    }
}
