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

/**
 * Default [MediaResolverRegistry]: tries registered [MediaResolver]s highest-[MediaResolver.priority]
 * first, skipping any whose [MediaResolver.canResolve] returns false. A resolver that throws is
 * treated as a soft failure and the chain falls through to the next candidate; the last error is
 * rethrown only if every candidate fails.
 *
 * Registration is backed by a [CopyOnWriteArrayList], so [register] / [unregister] are safe to call
 * concurrently with [resolve].
 */
class DefaultMediaResolverRegistry : MediaResolverRegistry {

    private val backing = CopyOnWriteArrayList<MediaResolver>()

    /**
     * Prefetch is a best-effort hint that opens with a blocking DNS lookup (the SSRF guard) and then
     * does real network work, so it must never run on the caller's thread - [prefetch] is invoked
     * from the client / render thread on every URL change. Permits bound how much of it runs at once
     * without serializing unrelated displays behind each other: a single permit meant the fourth
     * screen to come into view waited out three full probes before its own even started.
     */
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

    /**
     * Warms [source] through the capable resolvers in priority order, stopping at the first one that
     * reports it warmed a usable result. The SSRF host check and the dispatch run on
     * [DreamCoroutines.clientIo] so the blocking DNS lookup never stalls the caller.
     *
     * Stopping early matters: [MediaResolver.canResolve] is true for every source on the universal
     * `yt-dlp` fallback, so warming the whole chain spawned a doomed subprocess for every Twitch,
     * Vimeo, Kick and direct link — competing for CPU and bandwidth with the resolver that was
     * actually about to serve the video.
     */
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

    /**
     * Resolves [source] against each capable resolver in priority order, returning the first success.
     * @throws DreamMediaException.Unknown if no resolver is registered for [source].
     * @throws DreamMediaException.Unknown if [source] targets a non-public host (SSRF guard).
     * @throws Throwable the last resolver's failure if every capable resolver threw.
     */
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

    /**
     * SSRF guard: true when [source] carries a client-supplied URL whose host resolves to a
     * non-public address. Only [MediaSource.Remote] / [MediaSource.DirectStream] are checked here;
     * the platform sources ([MediaSource.YouTube], [MediaSource.Twitch], [MediaSource.Vimeo],
     * [MediaSource.Kick]) are constrained to their own fixed, trusted hosts by [MediaSource.from]
     * itself, and their resolvers validate the CDN URLs they mint through the same guard at playback.
     */
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
