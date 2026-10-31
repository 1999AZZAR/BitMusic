package com.music.bitchord.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.net.UnknownHostException

class ResilientDnsTest {

    @Test
    fun `resolves localhost and ip literals directly`() {
        val loopback = ResilientDns.lookup("127.0.0.1")
        assertEquals(1, loopback.size)
        assertEquals("127.0.0.1", loopback.first().hostAddress)

        val local = ResilientDns.lookup("localhost")
        assertTrue(local.isNotEmpty())
    }

    @Test
    fun `throws on blank hostname`() {
        assertFailsWith<UnknownHostException> {
            ResilientDns.lookup("   ")
        }
    }

    @Test
    fun `resolves and caches public hostname`() {
        val result1 = ResilientDns.lookup("www.google.com")
        assertTrue(result1.isNotEmpty())

        // Second lookup should return cached entries
        val result2 = ResilientDns.lookup("www.google.com")
        assertEquals(result1, result2)
    }
}
