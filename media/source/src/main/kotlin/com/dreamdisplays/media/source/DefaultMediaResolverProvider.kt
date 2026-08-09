package com.dreamdisplays.media.source

import com.dreamdisplays.api.media.source.MediaResolver
import com.dreamdisplays.api.media.source.MediaResolverProvider
import com.dreamdisplays.media.source.direct.DirectStreamResolver
import com.dreamdisplays.media.source.kick.KickResolver
import com.dreamdisplays.media.source.twitch.TwitchResolver
import com.dreamdisplays.media.source.vimeo.VimeoResolver
import com.dreamdisplays.media.source.ytdlp.NewPipeResolver
import com.dreamdisplays.media.source.ytdlp.YtDlpResolver

/**
 * Built-in resolver chain, fastest first: direct URL probe, then in-process platform resolvers (NewPipe, Twitch, Vimeo, Kick),
 * then yt-dlp fallback.
 */
object DefaultMediaResolverProvider : MediaResolverProvider {
    override fun resolvers(): List<MediaResolver> = listOf(
        DirectStreamResolver,
        NewPipeResolver,
        TwitchResolver,
        VimeoResolver,
        KickResolver,
        YtDlpResolver,
    )
}
