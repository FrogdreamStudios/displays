package com.dreamdisplays.datatypes

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.util.BoundingBox
import org.jspecify.annotations.NullMarked
import java.util.*

@NullMarked
class DisplayData(
    val id: UUID,
    val ownerId: UUID,
    val pos1: Location,
    val pos2: Location,
    val width: Int,
    val height: Int,
    val facing: BlockFace = BlockFace.NORTH
) {
    var url: String = ""
    var lang: String = ""
    var isSync: Boolean = false
    var duration: Long? = null

    val box: BoundingBox by lazy {
        BoundingBox(
            minOf(pos1.blockX, pos2.blockX).toDouble(),
            minOf(pos1.blockY, pos2.blockY).toDouble(),
            minOf(pos1.blockZ, pos2.blockZ).toDouble(),
            (maxOf(pos1.blockX, pos2.blockX) + 1).toDouble(),
            (maxOf(pos1.blockY, pos2.blockY) + 1).toDouble(),
            (maxOf(pos1.blockZ, pos2.blockZ) + 1).toDouble()
        )
    }
}
