package com.fw.main.utils.platform.system.asset;

import com.fw.main.Base;
import com.fw.main.Fw;
import com.fw.main.utils.platform.system.console.Console;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class AssetManager {
    public enum LoadMode {
        SYNC,
        LAZY
    }

    private final Base instance;
    private final Queue<Texture> freePool = new ConcurrentLinkedQueue<>();
    private Texture[] pool;
    private final Map<String, Texture> activeMap = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicAssetObject>> pendingEvents = new ConcurrentHashMap<>();
    private final Map<String, DynamicAssetObject> pendingObjects = new ConcurrentHashMap<>();
    private final Queue<DynamicAssetObject> daoFreePool = new ConcurrentLinkedQueue<>();
    private DynamicAssetObject[] daoPool;

    private final Queue<BufferedImage> garbageQueue = new ConcurrentLinkedQueue<>();

    public AssetManager(Base baseInstance) {
        this.instance = baseInstance;
    }

    void addGarbageList(BufferedImage bufferedImage) {
        if (bufferedImage != null) {
            garbageQueue.add(bufferedImage);
        }
    }

    public void clearGarbage() {
        int count = 0;
        BufferedImage b;
        while ((b = garbageQueue.poll()) != null) {
            b.flush();
            count++;
        }
        if (count > 0 && instance != null) {
            Fw.Helper.getConsoleToBaseInstance(instance)
                    .addLog(Console.LogType.SYSTEM, "clear texture garbage count: " + count);
        }
    }

    public synchronized void mallocTexturePool(int capacity) {
        pool = new Texture[capacity];
        freePool.clear();
        for (int i = 0; i < capacity; i++) {
            pool[i] = new Texture(this);
            freePool.add(pool[i]);
        }
    }
    public synchronized void mallocLazyLoadPool(int capacity) {
        daoPool = new DynamicAssetObject[capacity];
        daoFreePool.clear();
        for (int i = 0; i < capacity; i++) {
            daoPool[i] = new DynamicAssetObject();
            daoFreePool.add(daoPool[i]);
        }
    }

    private Texture getFreeTexture() {
        Texture tex = freePool.poll();
        if (tex == null) {
            throw new IllegalStateException("Out of texture pool! Increase pool size with mallocTexturePool().");
        }
        tex.setInUse(true);
        return tex;
    }
    private DynamicAssetObject getFreeDao() {
        DynamicAssetObject dao = daoFreePool.poll();
        if (dao == null) {
            throw new IllegalStateException("Out of DynamicAssetObject pool! Increase pool size with malloc().");
        }
        return dao;
    }

    public Texture load(LoadMode mode, String assetKey, String path, String eventKey) {
        if (activeMap.containsKey(assetKey) || pendingObjects.containsKey(assetKey)) {
            return activeMap.get(assetKey);
        }

        Texture texture = getFreeTexture();
        texture.init(assetKey, path);

        if (mode == LoadMode.SYNC) {
            try {
                texture.loadData();
                activeMap.put(assetKey, texture);
            } catch (Exception e) {
                texture.close();
                freePool.offer(texture);
                throw new RuntimeException("SYNC loading fail: " + assetKey, e);
            }
        } else if (mode == LoadMode.LAZY) {
            DynamicAssetObject dao = getFreeDao();
            dao.init(() -> {
                try {
                    texture.loadData();
                } catch (Exception e) {
                    texture.close();
                    freePool.offer(texture);
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
                        dao.reset();
                        daoFreePool.offer(dao);

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

    public synchronized void free(String assetKey) {
        Texture tex = activeMap.remove(assetKey);
        if (tex != null) {
            tex.close();
            freePool.offer(tex);
        } else {
            DynamicAssetObject dao = pendingObjects.remove(assetKey);
            if (dao != null) {
                pendingEvents.values().forEach(list -> list.remove(dao));

                dao.reset();
                daoFreePool.offer(dao);

                for (Texture t : pool) {
                    if (t.isInUse() && assetKey.equals(t.getAssetKey())) {
                        t.close();
                        freePool.offer(t);
                        break;
                    }
                }
            }
        }
    }

    public synchronized void disposeAll() {
        activeMap.clear();
        pendingObjects.clear();
        pendingEvents.clear();

        if (pool != null) {
            for (Texture tex : pool) {
                if (tex != null) tex.close();
            }
        }
        freePool.clear();
        if (pool != null) Collections.addAll(freePool, pool);

        if (daoPool != null) {
            for (DynamicAssetObject dao : daoPool) {
                if (dao != null) dao.reset();
            }
        }
        daoFreePool.clear();
        if (daoPool != null) Collections.addAll(daoFreePool, daoPool);
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