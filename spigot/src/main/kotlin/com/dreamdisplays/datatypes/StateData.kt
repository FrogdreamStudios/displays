package com.dreamdisplays.datatypes

import com.dreamdisplays.managers.DisplayManager
import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
class StateData(private val id: UUID?) {
    private var paused = false
    private var lastReportedTime: Long = 0
    private var lastReportedTimeTimestamp: Long = 0
    private var limitTime: Long = 0
    var displayData: com.dreamdisplays.datatypes.DisplayData =
        DisplayManager.getDisplayData(id) ?: throw IllegalStateException("Display data not found for id: $id")

    fun update(packet: SyncData) {
        this.paused = packet.currentState
        this.lastReportedTime = packet.currentTime
        this.lastReportedTimeTimestamp = System.nanoTime()
        limitTime = packet.limitTime
    }

    fun createPacket(): SyncData {
        val nanos = System.nanoTime()
        var currentTime: Long

        if (paused) {
            currentTime = lastReportedTime
        } else {
            val elapsed = nanos - lastReportedTimeTimestamp
            currentTime = lastReportedTime + elapsed
        }

        if (limitTime == 0L) {
            displayData.duration?.let { limitTime = it }
        }

        if (limitTime > 0) {
            currentTime %= limitTime
        }

        return SyncData(id, true, paused, currentTime, limitTime)
    }
}
