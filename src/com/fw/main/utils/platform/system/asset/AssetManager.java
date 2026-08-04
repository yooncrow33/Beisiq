package com.fw.main.utils.platform.system.asset;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class AssetManager {
    public enum LoadMode {
        /**
         * Loads immediately in the current thread. Do not use on the rendering or main logic thread.
         */
        SYNC,
        /**
         * Loads asynchronously in a background thread when event enable.
         */
        LAZY
    }

    static final Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unsafe init fail", e);
        }
    }

    private final Queue<Texture> freePool = new ConcurrentLinkedQueue<>();
    private Texture[] pool;
    private final Map<String, Texture> activeMap = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicAssetObject>> pendingEvents = new ConcurrentHashMap<>();
    private final Map<String, DynamicAssetObject> pendingObjects = new ConcurrentHashMap<>();

    public void malloc(int capacity) {
        pool = new Texture[capacity];
        freePool.clear();
        for (int i = 0; i < capacity; i++) {
            pool[i] = new Texture();
            freePool.add(pool[i]);
        }
    }

    private synchronized Texture getFreeTexture() {
        Texture tex = freePool.poll();
        if (tex == null) {
            throw new OutOfMemoryError("out of texture pool. use malloc to increase pool size.");
        }
        tex.setInUse(true);
        return tex;
    }

    public Texture load(LoadMode mode, String assetKey, String path, String eventKey) {
        if (activeMap.containsKey(assetKey) || pendingObjects.containsKey(assetKey)) return null;

        Texture texture = getFreeTexture();
        texture.init(assetKey, path);

        if (mode == LoadMode.SYNC) {
            try {
                texture.loadData();
                activeMap.put(assetKey, texture);
            } catch (Exception e) {
                texture.close();
                throw new RuntimeException("SYNC loading fail: " + assetKey, e);
            }
        } else if (mode == LoadMode.LAZY) {
            DynamicAssetObject dao = new DynamicAssetObject(() -> {
                try {
                    texture.loadData();
                } catch (Exception e) {
                    texture.close();
                    throw new RuntimeException("LAZY loading fail: " + assetKey, e);
                }
            });
            pendingObjects.put(assetKey, dao);
            pendingEvents.computeIfAbsent(eventKey, k -> new CopyOnWriteArrayList<>()).add(dao);
        }

        return texture;
    }

    public void event(String eventKey) {
        List<DynamicAssetObject> tasks = pendingEvents.remove(eventKey);
        if (tasks != null) {
            for (DynamicAssetObject task : tasks) {
                task.launch();
            }
        }
    }

    public Texture get(String assetKey) {
        Texture tex = activeMap.get(assetKey);
        if (tex != null) return tex;

        DynamicAssetObject dao = pendingObjects.get(assetKey);
        if (dao != null) {
            if (dao.isError()) {
                free(assetKey);
                return null;
            }
            if (dao.isLoaded()) {
                synchronized (this) {
                    dao = pendingObjects.remove(assetKey);
                    if (dao != null) {
                        for (Texture t : pool) {
                            if (t.isInUse() && assetKey.equals(t.getAssetKey())) {
                                activeMap.put(assetKey, t);
                                return t;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void free(String assetKey) {
        Texture tex = activeMap.remove(assetKey);
        if (tex != null) {
            tex.close();
            freePool.offer(tex);
        } else {
            DynamicAssetObject dao = pendingObjects.remove(assetKey);
            if (dao != null) {
                pendingEvents.values().forEach(list -> list.remove(dao));
                DynamicAssetObject.submitTask(() -> {
                    for (Texture t : pool) {
                        if (t.isInUse() && assetKey.equals(t.getAssetKey())) {
                            t.close();
                            break;
                        }
                    }
                });
            }
        }
    }

    public void disposeAll() {
        for (Texture tex : activeMap.values()) {
            tex.close();
        }
        activeMap.clear();
        pendingObjects.clear();

        for (Texture tex : pool) {
            if (tex != null && tex.isInUse()) {
                tex.close();
            }
        }
    }

    static boolean isResourcePath(String path) {
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