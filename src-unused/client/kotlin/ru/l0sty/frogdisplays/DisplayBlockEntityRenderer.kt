package ru.l0sty.frogdisplays


import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import ru.l0sty.frogdisplays.RenderUtil.fixRotation
import ru.l0sty.frogdisplays.RenderUtil.moveForward
import ru.l0sty.frogdisplays.RenderUtil.renderBlack
import ru.l0sty.frogdisplays.RenderUtil.renderTexture

class DisplayBlockEntityRenderer(ctx: BlockEntityRendererFactory.Context?) : BlockEntityRenderer<DisplayBlockEntity> {
    override fun render(
        entity: DisplayBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        //val screenManager: ScreenManager = CinemaModClient.getInstance().getScreenManager() //if (screen == null || !screen.isVisible()) return
        RenderSystem.enableDepthTest()
        val tessellator = Tessellator.getInstance()
        renderScreenTexture(matrices, tessellator)
        RenderSystem.disableDepthTest()
    }

    override fun rendersOutsideBoundingBox(blockEntity: DisplayBlockEntity): Boolean {
        return true
    }

    companion object {
        private fun renderScreenTexture(
            //screen: Screen,
            matrices: MatrixStack,
            tessellator: Tessellator
        ) {
            matrices.push()
            matrices.translate(1f, 1f, 0f)
            moveForward(matrices, "NORTH", 0.008f)
            fixRotation(matrices, "NORTH")
            matrices.scale(300f, 200f, 0f)
            if (false) {
                val glId: Int = FrogDisplaysClient.cef.renderer.getTextureID()
                renderTexture(matrices, tessellator, glId)
            } else {
                renderBlack(matrices, tessellator)
            }
            matrices.pop()
        }

        fun register() {
            BlockEntityRendererFactories.register(DisplayBlockEntity.DISPLAY_BLOCK_ENTITY,
                BlockEntityRendererFactory { ctx: BlockEntityRendererFactory.Context? ->
                    DisplayBlockEntityRenderer(
                        ctx
                    )
                })
        }
    }
}