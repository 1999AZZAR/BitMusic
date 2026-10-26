package com.music.bitchord.ui.tv.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class TvRemoteKeyDispatcherTest {

    @Test
    fun testMediaPlaybackControls() {
        assertEquals(TvRemoteAction.PLAY_PAUSE, TvRemoteKeyDispatcher.resolveAction(85, false))
        assertEquals(TvRemoteAction.PLAY, TvRemoteKeyDispatcher.resolveAction(126, false))
        assertEquals(TvRemoteAction.PAUSE, TvRemoteKeyDispatcher.resolveAction(127, false))
    }

    @Test
    fun testMediaSkipAndSeekControls() {
        assertEquals(TvRemoteAction.NEXT_TRACK, TvRemoteKeyDispatcher.resolveAction(87, false))
        assertEquals(TvRemoteAction.PREVIOUS_TRACK, TvRemoteKeyDispatcher.resolveAction(88, false))
        assertEquals(TvRemoteAction.FAST_FORWARD, TvRemoteKeyDispatcher.resolveAction(90, false))
        assertEquals(TvRemoteAction.REWIND, TvRemoteKeyDispatcher.resolveAction(89, false))
    }

    @Test
    fun testDpadUpDrawerTriggerOnlyInPlaybackView() {
        assertEquals(TvRemoteAction.TOGGLE_DRAWER, TvRemoteKeyDispatcher.resolveAction(19, isPlayingViewActive = true))
        assertEquals(TvRemoteAction.NONE, TvRemoteKeyDispatcher.resolveAction(19, isPlayingViewActive = false))
    }

    @Test
    fun testMenuKeyAction() {
        assertEquals(TvRemoteAction.TOGGLE_MENU, TvRemoteKeyDispatcher.resolveAction(82, false))
    }

    @Test
    fun testUnhandledKeyReturnsNone() {
        assertEquals(TvRemoteAction.NONE, TvRemoteKeyDispatcher.resolveAction(999, true))
    }
}
