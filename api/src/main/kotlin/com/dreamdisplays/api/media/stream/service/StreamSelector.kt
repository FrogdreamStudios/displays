package com.dreamdisplays.api.media.stream.service

import com.dreamdisplays.api.DreamDisplaysUnstableApi
import com.dreamdisplays.api.media.stream.model.MediaStream
import com.dreamdisplays.api.media.stream.model.StreamPreferences
import com.dreamdisplays.api.media.stream.model.StreamSet

@DreamDisplaysUnstableApi
interface StreamSelector {
    fun select(streams: List<MediaStream>, preferences: StreamPreferences): StreamSet
}
