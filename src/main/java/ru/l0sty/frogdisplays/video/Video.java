package ru.l0sty.frogdisplays.video;

import ru.l0sty.frogdisplays.buffer.DisplaysCustomPayload;
import ru.l0sty.frogdisplays.buffer.PacketByteBufSerializable;
import net.minecraft.network.PacketByteBuf;
import org.apache.commons.lang3.NotImplementedException;

public class Video extends DisplaysCustomPayload<Video> {

    private VideoInfo videoInfo;
    private long startedAt;

    public Video(VideoInfo videoInfo, long startedAt) {
        this();
        this.videoInfo = videoInfo;
        this.startedAt = startedAt;
    }

    public Video() {
        super("video");

    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public long getStartedAt() {
        return startedAt;
    }


}
