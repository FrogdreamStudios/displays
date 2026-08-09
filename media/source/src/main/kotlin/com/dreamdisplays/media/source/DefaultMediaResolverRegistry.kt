package com.dreamdisplays.media.source

import com.dreamdisplays.api.media.DreamMediaException
import com.dreamdisplays.api.media.source.MediaResolver
import com.dreamdisplays.api.media.source.MediaResolverRegistry
import com.dreamdisplays.api.media.source.MediaSource
import com.dreamdisplays.api.media.source.ResolvedMedia
import com.dreamdisplays.media.runtime.MediaHostGuard
import com.dreamdisplays.util.DreamCoroutines
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Default [MediaResolverRegistry]: tries registered resolvers in priority order, returning the first success. */
class DefaultMediaResolverRegistry : MediaResolverRegistry {

    private val backing = CopyOnWriteArrayList<MediaResolver>()

    /** Limits concurrent prefetch hints to avoid network/process flooding. */
    private val prefetchPermit = Semaphore(PREFETCH_CONCURRENCY)

    /**
     * Sources with a hint already in flight. The client fires [prefetch] on every URL change and on
     * every display load, so without this a wall of screens showing the same video queues one
     * identical warm-up per screen.
     */
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    override val resolvers: List<MediaResolver>
        get() = backing.sortedByDescending { it.priority }

    /** Adds [resolver] to the chain (btw resolver instance is never registered twice). */
    override fun register(resolver: MediaResolver) {
        if (resolver !in backing) backing.add(resolver)
    }

    /** Removes [resolver] from the chain; no-op if it was never registered. */
    override fun unregister(resolver: MediaResolver) {
        backing.remove(resolver)
    }

    /** Prefetches [source] through capable resolvers, stopping at the first success (best-effort). */
    override fun prefetch(source: MediaSource) {
        val key = source.toResolvableUrl() ?: source.toString()
        if (!inFlight.add(key)) return
        DreamCoroutines.clientIo.launch {
            try {
                prefetchPermit.withPermit {
                    if (isBlockedHost(source)) return@withPermit
                    for (resolver in resolvers) {
                        if (!resolver.canResolve(source)) continue
                        if (runCatching { resolver.prefetch(source) }.getOrDefault(false)) break
                    }
                }
            } finally {
                inFlight.remove(key)
            }
        }
    }

    /** Resolves [source] against each capable resolver in priority order, returning the first success. */
    override fun resolve(source: MediaSource): ResolvedMedia {
        if (isBlockedHost(source)) {
            throw DreamMediaException.Unknown("Refusing to resolve a media URL on a non-public host.", isFatal = true)
        }
        var lastError: Throwable? = null
        var attempted = false
        for (resolver in resolvers) {
            if (!resolver.canResolve(source)) continue
            attempted = true
            runCatching {
                return resolver.resolve(source)
            }.onFailure { e ->
                lastError = e
            }
        }
        if (!attempted) throw DreamMediaException.Unknown("No resolver registered for source: $source", isFatal = true)
        throw lastError ?: DreamMediaException.Unknown("All resolvers failed for source: $source")
    }

    /** SSRF guard: blocks non-public addresses like localhost, 192.168.*, etc. */
    private fun isBlockedHost(source: MediaSource): Boolean {
        val url = when (source) {
            is MediaSource.Remote -> source.url
            is MediaSource.DirectStream -> source.streamUrl
            else -> return false
        }
        return !MediaHostGuard.isAllowed(url)
    }

    private companion object {
        /**
         * Hints warmed at once. Enough to cover a room of screens, few enough not to flood the
         * network (or the `yt-dlp` subprocess budget) with speculative work.
         */
        const val PREFETCH_CONCURRENCY = 3
    }
}
