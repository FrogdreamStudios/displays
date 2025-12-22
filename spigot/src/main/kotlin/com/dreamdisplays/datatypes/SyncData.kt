package com.dreamdisplays.datatypes

import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
@JvmRecord
data class SyncData(
    val id: UUID?,
    val isSync: Boolean,
    val currentState: Boolean,
    val currentTime: Long,
    val limitTime: Long
)
