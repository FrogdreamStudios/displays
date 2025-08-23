package com.inotsleep.dreamdisplays.client.media.ytdlp;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class YouTubeCacheEntry {
    private final long expireAt;
    private final Map<String, List<Format>> audioFormats = new LinkedHashMap<>();
    private final NavigableMap<Integer, List<Format>> videoFormats = new TreeMap<>();

    public YouTubeCacheEntry(List<Format> formats) {
        this.expireAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3);

        for (Format format : formats) {
            if (!"none".equals(format.getAcodec())) {
                audioFormats
                        .computeIfAbsent(format.getLanguage(), k -> new ArrayList<>())
                        .add(format);
            } else {
                videoFormats
                        .computeIfAbsent(format.getHeight(), k -> new ArrayList<>())
                        .add(format);
            }
        }
    }

    public List<Format> getAudioFormats(String language) {
        if (audioFormats.isEmpty()) {
            return Collections.emptyList();
        }
        if (audioFormats.size() == 1) {
            return audioFormats.values().iterator().next();
        }

        String langLower = language.toLowerCase();

        List<Format> byKey = audioFormats.get(language);
        if (byKey != null) {
            return byKey;
        }

        for (Map.Entry<String, List<Format>> entry : audioFormats.entrySet()) {
            if (entry.getKey().toLowerCase().contains(langLower)) {
                return entry.getValue();
            }
        }

        for (List<Format> formatsList : audioFormats.values()) {
            if (!formatsList.isEmpty()) {
                String note = formatsList.getFirst().getFormatNote();
                if (note != null && note.toLowerCase().contains(langLower)) {
                    return formatsList;
                }
            }
        }

        for (List<Format> formatsList : audioFormats.values()) {
            if (!formatsList.isEmpty()) {
                String note = formatsList.getFirst().getFormatNote();
                if (note != null) {
                    String nl = note.toLowerCase();
                    if (nl.contains("default") || nl.contains("original")) {
                        return formatsList;
                    }
                }
            }
        }

        return audioFormats.values().iterator().next();
    }

    public List<Format> getVideoFormats(int quality) {
        if (videoFormats.isEmpty()) {
            return Collections.emptyList();
        }
        Map.Entry<Integer, List<Format>> entry = videoFormats.floorEntry(quality);

        if (entry != null) {
            return entry.getValue();
        }
        return videoFormats.firstEntry().getValue();
    }

    public Set<Integer> getVideoQualities() {
        return videoFormats.keySet();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }
}