package com.dreamdisplays.api.media.search

import com.dreamdisplays.api.DreamDisplaysUnstableApi

/**
 * A single chapter marker within a video. Only YouTube publishes these today.
 *
 * @since 1.9.0
 */
@DreamDisplaysUnstableApi
data class MediaChapter(
    /** Chapter title, as shown under the video. */
    val title: String,

    /** Offset from the start of the video, in seconds. */
    val startSeconds: Long,
)
