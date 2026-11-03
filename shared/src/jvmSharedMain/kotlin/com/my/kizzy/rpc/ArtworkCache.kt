package com.my.kizzy.rpc

import java.util.LinkedHashMap

internal object ArtworkCache {
    private const val MAX_ENTRIES = 128
    private val cache = object : LinkedHashMap<String, String>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean =
            size > MAX_ENTRIES
    }

    suspend fun getOrFetch(key: String, fetch: suspend () -> String?): String? {
        synchronized(cache) { cache[key] }?.let { return it }
        return fetch()?.also { value ->
            synchronized(cache) { cache[key] = value }
        }
    }
}
