package com.dreamdisplays.datatypes

import com.dreamdisplays.managers.DisplayManager
import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
class StateData(private val id: UUID?) {
    var displayData: DisplayData =
        DisplayManager.getDisplayData(id) ?: throw IllegalStateException("Display data not found for id: $id")

    private var paused = false
    private var lastReportedTime: Long = 0
    private var lastReportedTimestamp: Long = 0
    private var limitTime: Long = 0

    fun update(packet: SyncData) {
        paused = packet.currentState
        lastReportedTime = packet.currentTime
        lastReportedTimestamp = System.nanoTime()
        limitTime = packet.limitTime
    }

    fun createPacket(): SyncData {
        val nanos = System.nanoTime()
        val currentTime = if (paused) {
            lastReportedTime
        } else {
            lastReportedTime + (nanos - lastReportedTimestamp)
        }

        if (limitTime == 0L) displayData.duration?.let { limitTime = it }

        val time = if (limitTime > 0) currentTime % limitTime else currentTime
        return SyncData(id, true, paused, time, limitTime)
    }
}
