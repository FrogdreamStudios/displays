package ru.l0sty.frogdisplays

import net.minecraft.client.MinecraftClient
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos

fun getBlockHit(): BlockPos? {
    val hit = MinecraftClient.getInstance().crosshairTarget
    return if (hit != null && hit.type == HitResult.Type.BLOCK) {
        (hit as BlockHitResult).blockPos
    } else {
        null
    }
}