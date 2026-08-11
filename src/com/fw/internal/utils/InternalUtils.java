package com.fw.internal.utils;

import com.fw.main.Base;
import com.fw.main.Core;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Internal
public class InternalUtils {
    private static final BasicStroke basicStroke = new BasicStroke(3f);
    private static final Random random = new Random();

    public static Random getRandom() {
        return random;
    }

    /**
     * Retrieves an {@link InputStream} for a resource located exclusively within the engine's JAR.
     *
     * @param resourcePath the relative or absolute classpath path to the resource
     * @return the {@link InputStream} of the engine resource, or {@code null} if not found
     */
    public static InputStream getEngineResourceStream(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            return null;
        }

        String cleanPath = resourcePath.trim();
        if (cleanPath.startsWith("classpath:")) {
            cleanPath = cleanPath.substring(10);
        }
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        // 1. Class 기준 절대 경로 탐색 (앞에 '/' 붙음)
        InputStream is = Base.class.getResourceAsStream("/" + cleanPath);

        // 2. ClassLoader 기준 상대 경로 탐색 (앞에 '/' 없어야 함)
        if (is == null && Base.class.getClassLoader() != null) {
            is = Base.class.getClassLoader().getResourceAsStream(cleanPath);
        }

        return is;
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

    public static String getProjectFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName();
    }
    public static String getAssetFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName() + File.separator + "asset";
    }
}