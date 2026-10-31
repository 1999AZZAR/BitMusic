package com.music.bitchord.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient DNS resolver with in-memory TTL caching, transient error retries,
 * stale fallback, and zero-dependency public DoH fallback (1.1.1.1 / 8.8.8.8).
 *
 * Prevents playback drops and network freezes during local DNS interruptions
 * (e.g. Pi-hole restarts or transient router drops).
 */
object ResilientDns : Dns {

    private const val TTL_MS = 60_000L
    private const val STALE_MAX_MS = 10 * 60_000L

    private data class CacheEntry(
        val addresses: List<InetAddress>,
        val expiresAt: Long,
        val recordedAt: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    init {
        // Limit Java's negative DNS cache to 2 seconds so momentary lookups don't stick.
        runCatching {
            java.security.Security.setProperty("networkaddress.cache.negative.ttl", "2")
        }
    }

    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .dns(Dns.SYSTEM)
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .callTimeout(Duration.ofSeconds(4))
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.isBlank()) throw UnknownHostException("Empty hostname")

        // Literal IP addresses and localhost need no lookup.
        if (isLiteralOrLocal(hostname)) {
            return Dns.SYSTEM.lookup(hostname)
        }

        val now = System.currentTimeMillis()
        val entry = cache[hostname]
        if (entry != null && now < entry.expiresAt) {
            return entry.addresses
        }

        // 1. Try standard system DNS.
        try {
            val resolved = Dns.SYSTEM.lookup(hostname)
            if (resolved.isNotEmpty()) {
                cache[hostname] = CacheEntry(resolved, now + TTL_MS, now)
                return resolved
            }
        } catch (_: UnknownHostException) {
            // System DNS failed, attempt retry and fallbacks below.
        }

        // 2. Immediate quick retry after brief pause (transient flicker).
        try {
            Thread.sleep(200L)
            val resolved = Dns.SYSTEM.lookup(hostname)
            if (resolved.isNotEmpty()) {
                val updatedNow = System.currentTimeMillis()
                cache[hostname] = CacheEntry(resolved, updatedNow + TTL_MS, updatedNow)
                return resolved
            }
        } catch (_: Exception) {
            // Proceed to DoH fallback.
        }

        // 3. Fallback: Query Cloudflare DoH (1.1.1.1) or Google DoH (8.8.8.8) using IP literals.
        val dohAddresses = queryDoh(hostname)
        if (!dohAddresses.isNullOrEmpty()) {
            val updatedNow = System.currentTimeMillis()
            cache[hostname] = CacheEntry(dohAddresses, updatedNow + TTL_MS, updatedNow)
            return dohAddresses
        }

        // 4. Stale fallback: use expired cache entry if within stale tolerance.
        if (entry != null && now - entry.recordedAt < STALE_MAX_MS) {
            return entry.addresses
        }

        throw UnknownHostException("Unable to resolve host '$hostname' via system DNS or fallback DoH")
    }

    private fun isLiteralOrLocal(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost") || host == "127.0.0.1" || host == "::1") return true
        return host.all { it.isDigit() || it == '.' || it == ':' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun queryDoh(hostname: String): List<InetAddress>? {
        // Try Cloudflare DoH first
        queryCloudflareDoh(hostname)?.let { if (it.isNotEmpty()) return it }
        // Try Google DoH next
        queryGoogleDoh(hostname)?.let { if (it.isNotEmpty()) return it }
        return null
    }

    private fun queryCloudflareDoh(hostname: String): List<InetAddress>? = runCatching {
        val request = Request.Builder()
            .url("https://1.1.1.1/dns-query?name=$hostname&type=A")
            .header("Accept", "application/dns-json")
            .build()
        bootstrapClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body.string()
            parseDohAnswers(body)
        }
    }.getOrNull()

    private fun queryGoogleDoh(hostname: String): List<InetAddress>? = runCatching {
        val request = Request.Builder()
            .url("https://8.8.8.8/resolve?name=$hostname&type=A")
            .header("Accept", "application/json")
            .build()
        bootstrapClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body.string()
            parseDohAnswers(body)
        }
    }.getOrNull()

    private fun parseDohAnswers(jsonText: String): List<InetAddress> {
        val element = Json.parseToJsonElement(jsonText).jsonObject
        val answers = element["Answer"]?.jsonArray ?: return emptyList()
        val list = mutableListOf<InetAddress>()
        for (item in answers) {
            val obj = item.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            if (type == 1 || type == 28) { // 1 = A, 28 = AAAA
                val data = obj["data"]?.jsonPrimitive?.content ?: continue
                runCatching {
                    list += InetAddress.getByName(data)
                }
            }
        }
        return list
    }
}
