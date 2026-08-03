package com.dreamdisplays.api.media.source

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * Resolves a [MediaSource] into playable streams and metadata.
 *
 * @since 1.8.0
 */
@DreamDisplaysUnstableApi
interface MediaResolver {
    /** Higher-priority resolvers are preferred when several can resolve the same source. */
    val priority: Int get() = 0

    /** True when this resolver knows how to resolve [source]. */
    fun canResolve(source: MediaSource): Boolean

    /** Resolves [source] into stream URLs and metadata, or throws a media exception on failure. */
    fun resolve(source: MediaSource): ResolvedMedia

    /**
     * Pre-warms this resolver's caches for [source] before playback starts, returning true when it
     * actually warmed a usable result. [MediaResolverRegistry.prefetch] stops at the first resolver
     * that reports true, so a universal fallback is not made to speculatively warm sources a
     * dedicated fast path already owns. No-op returning false by default.
     */
    fun prefetch(source: MediaSource): Boolean = false
}
