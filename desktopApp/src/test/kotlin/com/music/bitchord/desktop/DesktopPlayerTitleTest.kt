package com.music.bitchord.desktop

import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPlayerTitleTest {
    @Test
    fun `Latin and left to right scripts start at the beginning`() {
        listOf("A long song title", "Lagu untuk hari ini", "愛の歌", "नमस्ते दुनिया").forEach {
            assertEquals(LayoutDirection.Ltr, playerTitleDirection(it), it)
        }
    }

    @Test
    fun `Arabic Hebrew and Persian read from the right`() {
        listOf("أغنية طويلة جميلة", "שיר ארוך ויפה", "آواز زیبای شب").forEach {
            assertEquals(LayoutDirection.Rtl, playerTitleDirection(it), it)
        }
    }

    @Test
    fun `leading digits punctuation emoji and combining marks do not determine direction`() {
        listOf("2026 — أغنية جميلة", "🎵 שיר יפה", "\u064Eأغنية").forEach {
            assertEquals(LayoutDirection.Rtl, playerTitleDirection(it), it)
        }
        assertEquals(LayoutDirection.Ltr, playerTitleDirection("🎵 2026 — A song"))
    }

    @Test
    fun `mixed scripts follow the first strong character`() {
        assertEquals(LayoutDirection.Rtl, playerTitleDirection("أغنية — Live in London"))
        assertEquals(LayoutDirection.Ltr, playerTitleDirection("Live — أغنية"))
    }

    @Test
    fun `supplementary right to left letters are detected`() {
        assertEquals(LayoutDirection.Rtl, playerTitleDirection("𞤀𞤣𞤤𞤢𞤥"))
    }

    @Test
    fun `empty and neutral titles use the desktop default`() {
        listOf("", "   ", "2026", "🎵 — …").forEach {
            assertEquals(LayoutDirection.Ltr, playerTitleDirection(it), it)
        }
    }
}
