package com.inotsleep.dreamdisplays.media.ytdlp;

import java.util.HashMap;

public class YouTubeQueryCache extends HashMap<String, YouTubeCacheEntry> {
    @Override
    public YouTubeCacheEntry get(Object key) {
        YouTubeCacheEntry entry = super.get(key);

        if (entry == null) return null;
        if (entry.isExpired()) {
            remove(key);
            return null;
        }

        return entry;
    }
}
