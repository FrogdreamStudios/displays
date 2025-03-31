package ru.l0sty.frogdisplays.video;

import ru.l0sty.frogdisplays.CinemaModClient;
import ru.l0sty.frogdisplays.buffer.DisplaysCustomPayload;
import ru.l0sty.frogdisplays.buffer.PacketByteBufSerializable;
import ru.l0sty.frogdisplays.service.VideoService;
import net.minecraft.network.PacketByteBuf;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class VideoInfo extends DisplaysCustomPayload<VideoInfo> {

    private VideoService videoService;
    private String id;
    private String title;
    private String poster;
    private String thumbnailUrl;
    private long durationSeconds;

    public VideoInfo(VideoService videoService, String id) {
        this();
        this.videoService = videoService;
        this.id = id;
    }

    public VideoInfo() {
        super("video_info");

    }

    public VideoService getVideoService() {
        return videoService;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleShort() {
        if (title.length() > 23) {
            return title.substring(0, 20) + "...";
        } else {
            return title;
        }
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDurationString() {
        long totalDurationMillis = durationSeconds * 1000;
        String totalDurationFormatted = DurationFormatUtils.formatDuration(totalDurationMillis, "H:mm:ss");
        totalDurationFormatted = reduceFormattedDuration(totalDurationFormatted);
        return totalDurationFormatted;
    }

    private static String reduceFormattedDuration(String formatted) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] split = formatted.split(":");

        // If does not have hours
        if (!split[0].equals("0")) {
            return formatted;
        } else {
            stringBuilder.append(split[1]).append(":").append(split[2]);
            return stringBuilder.toString();
        }
    }

    public boolean isLivestream() {
        return durationSeconds == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VideoInfo)) {
            return false;
        }
        VideoInfo videoInfo = (VideoInfo) o;
        return videoService == videoInfo.videoService && Objects.equals(id, videoInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoService, id);
    }


}
