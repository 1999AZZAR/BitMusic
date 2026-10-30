package com.music.bitchord.ui.tv.drawer

/**
 * Available tabs in the 10-foot TV Now Playing slide-in drawer.
 */
enum class TvDrawerTab {
    QUEUE,
    LYRICS
}

/**
 * State container managing open/closed transitions and active tab switching
 * for the 10-foot TV playback slide-in drawer.
 */
class TvNowPlayingDrawerState(
    initialIsOpen: Boolean = false,
    initialTab: TvDrawerTab = TvDrawerTab.QUEUE,
) {
    var isOpen: Boolean = initialIsOpen
        private set

    var selectedTab: TvDrawerTab = initialTab
        private set

    fun open(tab: TvDrawerTab = selectedTab) {
        selectedTab = tab
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun toggle() {
        isOpen = !isOpen
    }

    fun nextTab() {
        selectedTab = if (selectedTab == TvDrawerTab.QUEUE) TvDrawerTab.LYRICS else TvDrawerTab.QUEUE
    }

    fun previousTab() {
        nextTab()
    }

    fun selectTab(tab: TvDrawerTab) {
        selectedTab = tab
    }
}
