package com.dreamdisplays.datatypes

import com.dreamdisplays.utils.Outliner
import com.dreamdisplays.utils.Region
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.jspecify.annotations.NullMarked
import java.util.*

/**
 * Player's current selection for a feature display.
 * @param player The player making the selection.
 *
 * @property pos1 One corner of the selected area.
 * @property pos2 Opposite corner of the selected area.
 * @property isReady Boolean indicating if the selection is complete.
 * @property face The direction the selection is facing.
 * @property playerId Unique identifier for the player.
 *
 */
@NullMarked
class SelectionData(player: Player) {
    var pos1: Location? = null
    var pos2: Location? = null
    var isReady: Boolean = false

    private var face: BlockFace? = null
    private val playerId: UUID = player.uniqueId

    fun setFace(face: BlockFace) {
        this.face = face
    }

    fun getFace(): BlockFace = face ?: BlockFace.NORTH

    fun drawBox() {
        val p1 = pos1 ?: return
        val p2 = pos2 ?: return
        val player = Bukkit.getPlayer(playerId) ?: return
        Outliner.showOutline(player, p1, p2)
    }

    fun generateDisplayData(): DisplayData {
        val p1 = pos1 ?: throw IllegalStateException("pos1 is not set")
        val p2 = pos2 ?: throw IllegalStateException("pos2 is not set")
        val f = face ?: throw IllegalStateException("face is not set")

        val region = Region.calculateRegion(p1, p2)
        val dPos1 = region.getMinLocation(p1.world)
        val dPos2 = region.getMaxLocation(p1.world)

        return DisplayData(UUID.randomUUID(), playerId, dPos1, dPos2, region.width, region.height, f)
    }
}
