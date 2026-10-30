package com.music.bitchord.ui.tv.drawer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvNowPlayingDrawerStateTest {

    @Test
    fun testInitialStateClosedWithQueueTab() {
        val state = TvNowPlayingDrawerState()
        assertFalse(state.isOpen, "Drawer should be initially closed")
        assertEquals(TvDrawerTab.QUEUE, state.selectedTab)
    }

    @Test
    fun testOpenCloseToggle() {
        val state = TvNowPlayingDrawerState()
        state.open()
        assertTrue(state.isOpen)

        state.close()
        assertFalse(state.isOpen)

        state.toggle()
        assertTrue(state.isOpen)

        state.toggle()
        assertFalse(state.isOpen)
    }

    @Test
    fun testTabSwitching() {
        val state = TvNowPlayingDrawerState()
        assertEquals(TvDrawerTab.QUEUE, state.selectedTab)

        state.nextTab()
        assertEquals(TvDrawerTab.LYRICS, state.selectedTab)

        state.nextTab()
        assertEquals(TvDrawerTab.QUEUE, state.selectedTab)

        state.previousTab()
        assertEquals(TvDrawerTab.LYRICS, state.selectedTab)

        state.selectTab(TvDrawerTab.QUEUE)
        assertEquals(TvDrawerTab.QUEUE, state.selectedTab)
    }

    @Test
    fun testOpenWithSpecificTab() {
        val state = TvNowPlayingDrawerState()
        state.open(tab = TvDrawerTab.LYRICS)
        assertTrue(state.isOpen)
        assertEquals(TvDrawerTab.LYRICS, state.selectedTab)
    }
}
