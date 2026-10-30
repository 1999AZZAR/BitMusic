package com.music.bitchord.ui.tv.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TvFocusSpecTest {

    @Test
    fun testRestingStateProperties() {
        assertEquals(1.0f, TvFocusSpec.calculateScale(isFocused = false, isPressed = false))
        assertEquals(0.0f, TvFocusSpec.calculateBorderWidth(isFocused = false))
        assertEquals(0.0f, TvFocusSpec.calculateElevation(isFocused = false, isPressed = false))
    }

    @Test
    fun testFocusedStateProperties() {
        assertEquals(1.05f, TvFocusSpec.calculateScale(isFocused = true, isPressed = false))
        assertEquals(2.5f, TvFocusSpec.calculateBorderWidth(isFocused = true))
        assertEquals(16.0f, TvFocusSpec.calculateElevation(isFocused = true, isPressed = false))
    }

    @Test
    fun testPressedStateDampsScale() {
        val pressedScale = TvFocusSpec.calculateScale(isFocused = true, isPressed = true)
        assertTrue(pressedScale < 1.05f, "Pressed scale ($pressedScale) should be damped below 1.05x")
        assertTrue(pressedScale > 1.0f, "Pressed scale ($pressedScale) should remain above 1.0x")
        assertEquals(0.0f, TvFocusSpec.calculateElevation(isFocused = true, isPressed = true))
    }
}
