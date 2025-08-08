package com.inotsleep.dreamdisplays.client_1_21_8.forge;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.render.DisplayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
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

    @SubscribeEvent
    public static void onRenderAfterEntities(TickEvent.PlayerTickEvent.Post event) {
        DisplayRenderer.onRenderCall(new PoseStack());
    }
}
