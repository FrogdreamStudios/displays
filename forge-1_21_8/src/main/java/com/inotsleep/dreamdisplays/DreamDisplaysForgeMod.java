package com.inotsleep.dreamdisplays;

import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import me.inotsleep.utils.logging.LoggingManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;

@Mod(DreamDisplaysClientCommon.MOD_ID)
public class DreamDisplaysForgeMod {

    public DreamDisplaysForgeMod(FMLJavaModLoadingContext context) {
        LoggingManager.setLogger(LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID));
        System.out.println("DreamDisplaysForgeMod loaded");
        System.out.println(LoggingManager.getLogger());
        System.out.println(LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID));
        System.out.println(LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID).getClass());
        LogManager.getLogger(DreamDisplaysClientCommon.MOD_ID).info("DreamDisplaysForgeMod loaded");
        DreamDisplaysClientCommon.onModInit();
    }
}
