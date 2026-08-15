package com.dreamdisplays.api.media.sink.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.media.sink.model.DecodedVideoFrame

/**
 * Consumer for decoded video frames. Usually implemented by a texture upload queue.
 *
 * @since 1.8.x
 */
@DreamDisplaysUnstableApi
fun interface VideoFrameSink {
    /** Accepts one decoded [frame]. */
    fun onFrame(frame: DecodedVideoFrame)

    companion object {
        /** Sink that intentionally drops every frame. */
        val DISCARD: VideoFrameSink = VideoFrameSink { }
    }
}
