package com.fw.internal.utils;

import com.fw.main.Core;

import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InternalUtils {
    private static final BasicStroke basicStroke = new BasicStroke(3f);
    public static String getProjectFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName();
    }
    public static String getAssetFolder() {
        return System.getProperty("user.home") + File.separator + "." + Core.get().getProjectName() + File.separator + "asset";
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
}
