package ru.l0sty.frogdisplays

import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView


object DisplayBlock: Block(Settings.create().solid().strength(-1f, 3600000.0f).dropsNothing().nonOpaque()) {

    lateinit var IDENT: Identifier
    override fun getRenderType(state: BlockState): BlockRenderType {
        return BlockRenderType.ENTITYBLOCK_ANIMATED
    }

    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        return VoxelShapes.empty()
    }
    fun register() {
        IDENT = Identifier.of("frogdisplays", "screen")

        Registry.register(Registries.BLOCK, IDENT, DisplayBlock)

    }
}
