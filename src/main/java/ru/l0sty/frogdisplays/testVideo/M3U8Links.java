package ru.l0sty.frogdisplays.testVideo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.impl.networking.client.ClientNetworkingImpl;
import ru.l0sty.frogdisplays.net.M3U8Packet;
import ru.l0sty.frogdisplays.net.M3U8RequestPacket;
import ru.l0sty.frogdisplays.net.VideoInfoPacket;
import ru.l0sty.frogdisplays.net.VideoInfoRequestPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class M3U8Links {
    public static final Map<String, CompletableFuture<M3U8Packet>> requestedM3U8Data = new HashMap<>();
    public static final Map<String, CompletableFuture<VideoInfoPacket>> requestedVideoInfo = new HashMap<>();

    public static CompletableFuture<M3U8Packet> getM3U8(String videoURL, String quality) {
        ClientPlayNetworking.send(new M3U8RequestPacket(videoURL, quality));
        requestedM3U8Data.put(videoURL+quality, new CompletableFuture<>());

        return requestedM3U8Data.get(videoURL+quality);
    }

    public static CompletableFuture<VideoInfoPacket> getVideoInfo(String videoURL) {
        ClientPlayNetworking.send(new VideoInfoRequestPacket(videoURL));
        requestedVideoInfo.put(videoURL, new CompletableFuture<>());
        return requestedVideoInfo.get(videoURL);
    }
}

