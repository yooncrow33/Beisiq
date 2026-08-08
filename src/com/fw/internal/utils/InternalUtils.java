package com.fw.internal.utils;

import com.fw.main.Base;
import com.fw.main.Core;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Internal
public class InternalUtils {
    private static final BasicStroke basicStroke = new BasicStroke(3f);
    private static final Random random = new Random();
    public static Random getRandom() {return random;}
    public static String getProjectFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName();
    }
    public static String getAssetFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName() + File.separator + "asset";
    }

    /**
     * It's include last separator.
     */
    public static String getJarResourceFolder() {
        URL resource = InternalUtils.class.getResource("/");
        if (resource != null) {
            return "/";
        }
        return "/";
    }

    public static BasicStroke getBasicStroke() {
        return basicStroke;
    }

    public static class Time {
        public static LocalDateTime currentTime() {
            return LocalDateTime.now();
        }

        public static ZoneId getCurrentTimeZone() {
            return ZoneId.systemDefault();
        }

        public static String getTimeFormate() {
            LocalDateTime localTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return localTime.format(formatter);
        }
    }

     public static boolean isResourcePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ||
                trimmed.startsWith("classpath:") ||
                trimmed.startsWith("jar:") ||
                trimmed.contains("!/");
    }
}
