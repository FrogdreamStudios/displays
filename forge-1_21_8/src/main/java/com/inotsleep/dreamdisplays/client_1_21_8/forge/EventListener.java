package com.inotsleep.dreamdisplays.client_1_21_8.forge;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.render.DisplayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DreamDisplaysClientCommon.MOD_ID, value = Dist.CLIENT)
public class EventListener {

    @SubscribeEvent
    public static void onEndTick(TickEvent.ClientTickEvent.Post event) {
        DreamDisplaysClientCommon.onTick();
    }

    private static boolean calledRendererOnThisFrame = false;

    @SubscribeEvent
    public static void onRenderBeforeEntities(TickEvent.RenderTickEvent.Pre event) {
        calledRendererOnThisFrame = false;
    }

    @SubscribeEvent
    public static void onRenderAfterEntities(RenderLivingEvent.Post<?, ?, ?> event) {
        if (calledRendererOnThisFrame) return;

        DisplayRenderer.onRenderCall();
        calledRendererOnThisFrame = true;
    }
}
