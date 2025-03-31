package ru.l0sty.frogdisplays

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import org.joml.Quaternionf


object RenderUtil {
    fun fixRotation(matrixStack: MatrixStack, facing: String?) {
        val rotation: Quaternionf


        when (facing) {
            "NORTH" -> {
                rotation = Quaternionf().rotationY(Math.toRadians(180.0).toFloat())
                matrixStack.translate(0f, 0f, 1f)
            }

            "WEST" -> {
                rotation = Quaternionf().rotationY(Math.toRadians(-90.0).toFloat())
                matrixStack.translate(0f, 0f, 0f)
            }

            "EAST" -> {
                rotation = Quaternionf().rotationY(Math.toRadians(90.0).toFloat())
                matrixStack.translate(-1f, 0f, 1f)
            }

            else -> {
                rotation = Quaternionf()
                matrixStack.translate(-1f, 0f, 0f)
            }
        }
        matrixStack.multiply(rotation)
    }

    fun moveForward(matrixStack: MatrixStack, facing: String?, amount: Float) {
        when (facing) {
            "NORTH" -> matrixStack.translate(0f, 0f, -amount)
            "WEST" -> matrixStack.translate(-amount, 0f, 0f)
            "EAST" -> matrixStack.translate(amount, 0f, 0f)
            else -> matrixStack.translate(0f, 0f, amount)
        }
    }

    fun moveHorizontal(matrixStack: MatrixStack, facing: String?, amount: Float) {
        when (facing) {
            "NORTH" -> matrixStack.translate(-amount, 0f, 0f)
            "WEST" -> matrixStack.translate(0f, 0f, amount)
            "EAST" -> matrixStack.translate(0f, 0f, -amount)
            else -> matrixStack.translate(amount, 0f, 0f)
        }
    }

    fun moveVertical(matrixStack: MatrixStack, amount: Float) {
        matrixStack.translate(0f, amount, 0f)
    }

    fun renderTexture(matrixStack: MatrixStack, tessellator: Tessellator, glId: Int) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram)
        RenderSystem.setShaderTexture(0, glId)
        val matrix4f = matrixStack.peek().positionMatrix
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(matrix4f, 0.0f, -1.0f, 1.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f)
        buffer.vertex(matrix4f, 1.0f, -1.0f, 1.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f)
        buffer.vertex(matrix4f, 1.0f, 0.0f, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f)
        buffer.vertex(matrix4f, 0f, 0f, 0f).color(255, 255, 255, 255).texture(0.0f, 0.0f)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        RenderSystem.setShaderTexture(0, 0)
    }

    fun renderColor(matrixStack: MatrixStack, tessellator: Tessellator, r: Int, g: Int, b: Int) {
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val matrix4f = matrixStack.peek().positionMatrix
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix4f, 0.0f, -1.0f, 1.0f).color(r, g, b, 255)
        buffer.vertex(matrix4f, 1.0f, -1.0f, 1.0f).color(r, g, b, 255)
        buffer.vertex(matrix4f, 1.0f, 0.0f, 0.0f).color(r, g, b, 255)
        buffer.vertex(matrix4f, 0f, 0f, 0f).color(r, g, b, 255)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    fun renderBlack(matrixStack: MatrixStack, tessellator: Tessellator) {
        renderColor(matrixStack, tessellator, 0, 0, 0)
    }
}