package ru.l0sty.frogdisplays.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
//    public static MCEFBrowser createBrowser(String startUrl, Screen screen) {
//        MCEFBrowser browser = MCEF.createBrowser(startUrl, true);
//        browser.setCloseAllowed();
//        browser.createImmediately();
//
//        {
//            float widthBlocks = screen.getWidth();
//            float heightBlocks = screen.getHeight();
//            float scale = widthBlocks / heightBlocks;
//            int height = Integer.parseInt(screen.getQuality());
//            int width = (int) Math.floor(height * scale);
//            browser.resize(width, height);
//        }
//        return browser;
//    }

    public static String extractVideoId(String youtubeUrl) {
        String videoId = null;
        String regex = "(?<=watch\\?v=|/videos/|/shorts/|/live/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%‌2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(youtubeUrl);
        if (matcher.find()) {
            videoId = matcher.group();
        }
        return videoId;
    }
}
