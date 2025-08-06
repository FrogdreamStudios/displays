package com.inotsleep.dreamdisplays.client_1_21_8.forge;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DreamDisplaysClientCommon.MOD_ID, value = Dist.CLIENT)
public class EventListener {

    @SubscribeEvent
    public void onEndTick(TickEvent.ClientTickEvent event) {
        DreamDisplaysClientCommon.onTick();
    }
}
