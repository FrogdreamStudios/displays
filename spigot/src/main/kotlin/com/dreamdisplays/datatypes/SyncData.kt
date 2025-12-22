package com.dreamdisplays.datatypes

import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
@JvmRecord
data class SyncData(
    @JvmField val id: UUID?,
    @JvmField val isSync: Boolean,
    @JvmField val currentState: Boolean,
    @JvmField val currentTime: Long,
    @JvmField val limitTime: Long
)
