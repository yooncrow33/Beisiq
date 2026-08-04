package com.fw.main.utils.io;

import com.fw.internal.utils.Internal;
import com.fw.internal.utils.InternalUtils;

import java.io.File;
import java.net.URL;

public class IoUtils {
    /**
     * It's include last separator.
     * @return current project folder(It's in under user root.)
     */
    public static String getProjectFolder() {
        return InternalUtils.getProjectFolder() + File.separator;
    }

    /**
     * It's include last separator.
     * @return current project's resource root.
     */
    public static String getCurrentResourceFolder() {
        // 호출하는 스레드의 ContextClassLoader를 통해 엔진을 사용하는 애플리케이션의 리소스 루트 반환
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = IoUtils.class.getClassLoader();
        }

        URL resource = classLoader.getResource("");
        if (resource != null) {
            return "/";
        }
        return "/";
    }

    public static String getAssetFolderInProjectFolder() {
        return getProjectFolder() + "asset" + File.separator;
    }
}
