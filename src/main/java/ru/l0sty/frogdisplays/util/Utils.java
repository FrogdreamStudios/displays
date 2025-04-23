package ru.l0sty.frogdisplays.util;

import java.net.URI;
import java.net.URISyntaxException;
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
        try {
            URI uri = new URI(youtubeUrl);
            String query = uri.getQuery();                // берёт часть после "?"
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("v")) {
                        return pair[1];
                    }
                }
            }
            // если короткая ссылка youtu.be/ID
            String host = uri.getHost();
            if (host != null && host.contains("youtu.be")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    return path.substring(1);
                }
            }
        } catch (URISyntaxException e) {
        }

        String regex = "(?<=([?&]v=))[^#&?]*";
        Matcher m = Pattern.compile(regex).matcher(youtubeUrl);
        return m.find() ? m.group() : null;
    }
}
