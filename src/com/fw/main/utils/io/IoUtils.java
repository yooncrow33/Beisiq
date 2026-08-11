package com.fw.main.utils.io;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.Core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility class providing I/O operations for engine internal resources,
 * external game project resources, local project directories, and custom file paths.
 */
public class IoUtils {

    /**
     * Retrieves an {@link InputStream} for resources embedded inside the engine's JAR.
     *
     * @param resourcePath the path to the engine resource
     * @return the {@link InputStream} of the resource, or {@code null} if not found
     */
    public static InputStream getEngineResourceStream(String resourcePath) {
        return InternalUtils.getEngineResourceStream(resourcePath);
    }

    /**
     * Retrieves an {@link InputStream} for resources embedded inside the game project's JAR.
     * Uses the current thread's context ClassLoader to isolate game resources from engine resources.
     *
     * @param resourcePath the path to the game resource
     * @return the {@link InputStream} of the game resource, or {@code null} if not found
     */
    public static InputStream getGameResourceStream(String resourcePath) {
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

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream is = contextClassLoader.getResourceAsStream(cleanPath);
            if (is != null) return is;
        }

        return IoUtils.class.getClassLoader().getResourceAsStream(cleanPath);
    }

    /**
     * Retrieves an {@link InputStream} for a file located in the user's local project directory
     * ({@code ~/.{projectName}/relativePath}).
     *
     * @param relativePath the relative file path within the project directory
     * @return the {@link InputStream} of the target file
     * @throws FileNotFoundException if the file does not exist
     */
    public static InputStream getProjectFileStream(String relativePath) throws FileNotFoundException {
        String projectName = Core.get().getProjectName();
        File file = new File(System.getProperty("user.home") + File.separator + "." + projectName, relativePath);
        return new FileInputStream(file);
    }

    /**
     * Retrieves an {@link InputStream} for a file located in the user's local asset directory
     * ({@code ~/.{projectName}/asset/relativePath}).
     *
     * @param relativePath the relative file path within the asset directory
     * @return the {@link InputStream} of the target asset file
     * @throws FileNotFoundException if the file does not exist
     */
    public static InputStream getAssetFileStream(String relativePath) throws FileNotFoundException {
        String projectName = Core.get().getProjectName();
        File file = new File(System.getProperty("user.home") + File.separator + "." + projectName + File.separator + "asset", relativePath);
        return new FileInputStream(file);
    }

    /**
     * Retrieves an {@link InputStream} from a specified custom absolute or relative file path.
     *
     * @param customPath the absolute or relative file system path
     * @return the {@link InputStream} of the file
     * @throws FileNotFoundException if the file does not exist at the specified path
     */
    public static InputStream getCustomPathStream(String customPath) throws FileNotFoundException {
        return new FileInputStream(new File(customPath));
    }
}