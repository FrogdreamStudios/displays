package ru.l0sty.frogdisplays

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos


class DisplayBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : BlockEntity(type, pos, state) {
    companion object {
        lateinit var DISPLAY_BLOCK_ENTITY: BlockEntityType<DisplayBlockEntity>
        lateinit var IDENT: Identifier
        fun register() {
            IDENT = Identifier.of("cinemamod", "screen_block_entity")
            DISPLAY_BLOCK_ENTITY = BlockEntityType.Builder
                .create(::DisplayBlockEntity, DisplayBlock)
                .build()
            Registry.register(Registries.BLOCK_ENTITY_TYPE, IDENT, DISPLAY_BLOCK_ENTITY)
        }
    }
    constructor(pos: BlockPos?, state: BlockState?) : this(null, pos, state) {}

}