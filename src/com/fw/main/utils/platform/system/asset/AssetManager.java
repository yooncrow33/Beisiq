package com.fw.main.utils.platform.system.asset;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.Base;
import com.fw.main.Fw;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.InternalSoundModule;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.MusicAsset;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.SoundAsset;
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
    public enum MusicType {
        MEM_MUSIC,
        STREAM_MUSIC
    }

    public enum AssetType {
        SOUND,
        MUSIC,
        TEXTURE
    }

    private final Base instance;
    private final Queue<Texture> freePool = new ConcurrentLinkedQueue<>();
    private Texture[] pool;
    private final Map<String, Texture> textureActiveMap = new ConcurrentHashMap<>();
    private final Map<String, SoundAsset> soundActiveMap = new ConcurrentHashMap<>();
    private final Map<String, MusicAsset> musicActiveMap = new ConcurrentHashMap<>();
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

    public Texture loadTexture(LoadMode mode, String assetKey, String path, String eventKey) {
        if (textureActiveMap.containsKey(assetKey) || pendingObjects.containsKey(assetKey)) {
            return textureActiveMap.get(assetKey);
        }

        Texture texture = getFreeTexture();
        texture.init(assetKey, path);

        if (mode == LoadMode.SYNC) {
            try {
                texture.loadData();
                textureActiveMap.put(assetKey, texture);
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

    public SoundAsset loadSound(LoadMode mode, String assetKey, String path, String eventKey) {
        if (soundActiveMap.containsKey(assetKey) || pendingObjects.containsKey(assetKey)) {
            return soundActiveMap.get(assetKey);
        }

        if (!InternalSoundModule.isInitialized()) {
            InternalSoundModule.init();
        }

        java.net.URL url = resolveAudioUrl(path);
        if (url == null) {
            throw new IllegalArgumentException("Invalid sound path: " + path);
        }

        if (mode == LoadMode.SYNC) {
            try {
                SoundAsset sound = InternalSoundModule.loadSound(url);
                if (sound == null) {
                    throw new RuntimeException("Failed to load sound instance: " + assetKey);
                }
                soundActiveMap.put(assetKey, sound);
                return sound;
            } catch (Exception e) {
                throw new RuntimeException("SYNC sound loading fail: " + assetKey, e);
            }
        } else if (mode == LoadMode.LAZY) {
            DynamicAssetObject dao = getFreeDao();
            dao.init(() -> {
                try {
                    SoundAsset sound = InternalSoundModule.loadSound(url);
                    if (sound == null) {
                        throw new RuntimeException("Failed to load sound instance: " + assetKey);
                    }
                    soundActiveMap.put(assetKey, sound);
                } catch (Exception e) {
                    throw new RuntimeException("LAZY sound loading fail: " + assetKey, e);
                }
            });
            pendingObjects.put(assetKey, dao);
            pendingEvents.computeIfAbsent(eventKey, k -> new CopyOnWriteArrayList<>()).add(dao);
        }

        return null;
    }

    public MusicAsset loadMusic(LoadMode mode, MusicType type, String assetKey, String path, String eventKey) {
        if (musicActiveMap.containsKey(assetKey) || pendingObjects.containsKey(assetKey)) {
            return musicActiveMap.get(assetKey);
        }

        if (!InternalSoundModule.isInitialized()) {
            InternalSoundModule.init();
        }

        java.net.URL url = resolveAudioUrl(path);
        if (url == null) {
            throw new IllegalArgumentException("Invalid music path: " + path);
        }

        if (mode == LoadMode.SYNC) {
            try {
                MusicAsset music = createMusicInstance(type, url);
                if (music == null) {
                    throw new RuntimeException("Failed to load music instance: " + assetKey);
                }
                musicActiveMap.put(assetKey, music);
                return music;
            } catch (Exception e) {
                throw new RuntimeException("SYNC music loading fail: " + assetKey, e);
            }
        } else if (mode == LoadMode.LAZY) {
            DynamicAssetObject dao = getFreeDao();
            dao.init(() -> {
                try {
                    MusicAsset music = createMusicInstance(type, url);
                    if (music == null) {
                        throw new RuntimeException("Failed to load music instance: " + assetKey);
                    }
                    musicActiveMap.put(assetKey, music);
                } catch (Exception e) {
                    throw new RuntimeException("LAZY music loading fail: " + assetKey, e);
                }
            });
            pendingObjects.put(assetKey, dao);
            pendingEvents.computeIfAbsent(eventKey, k -> new CopyOnWriteArrayList<>()).add(dao);
        }

        return null;
    }

    private MusicAsset createMusicInstance(MusicType type, java.net.URL url) {
        return switch (type) {
            case MEM_MUSIC -> InternalSoundModule.loadMusic(url, false);
            case STREAM_MUSIC -> InternalSoundModule.loadMusic(url, true);
        };
    }

    private java.net.URL resolveAudioUrl(String path) {
        if (path == null) return null;
        try {
            if (InternalUtils.isResourcePath(path)) {
                String resPath = path.startsWith("/") ? path : "/" + path;
                return InternalSoundModule.class.getResource(resPath);
            } else {
                return new java.io.File(path).toURI().toURL();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void event(String eventKey) {
        List<DynamicAssetObject> tasks = pendingEvents.remove(eventKey);
        if (tasks != null) {
            for (DynamicAssetObject task : tasks) {
                task.launch();
            }
        }
    }

    public Texture getTexture(String assetKey) {
        Texture tex = textureActiveMap.get(assetKey);
        if (tex != null) return tex;

        DynamicAssetObject dao = pendingObjects.get(assetKey);
        if (dao != null) {
            if (dao.isError()) {
                free(AssetType.TEXTURE, assetKey);
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
                                textureActiveMap.put(assetKey, t);
                                return t;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Sound getSound(String assetKey) {
        Sound sound = soundActiveMap.get(assetKey);
        if (sound != null) return sound;

        DynamicAssetObject dao = pendingObjects.get(assetKey);
        if (dao != null) {
            if (dao.isError()) {
                free(AssetType.SOUND, assetKey);
                return null;
            }
            if (dao.isLoaded()) {
                synchronized (this) {
                    dao = pendingObjects.remove(assetKey);
                    if (dao != null) {
                        dao.reset();
                        daoFreePool.offer(dao);
                        return soundActiveMap.get(assetKey);
                    }
                }
            }
        }
        return null;
    }

    public Music getMusic(String assetKey) {
        Music music = musicActiveMap.get(assetKey);
        if (music != null) return music;

        DynamicAssetObject dao = pendingObjects.get(assetKey);
        if (dao != null) {
            if (dao.isError()) {
                free(AssetType.MUSIC, assetKey);
                return null;
            }
            if (dao.isLoaded()) {
                synchronized (this) {
                    dao = pendingObjects.remove(assetKey);
                    if (dao != null) {
                        dao.reset();
                        daoFreePool.offer(dao);
                        return musicActiveMap.get(assetKey);
                    }
                }
            }
        }
        return null;
    }

    public synchronized void free(AssetType assetType, String assetKey) {
        switch (assetType) {
            case TEXTURE -> {
                Texture tex = textureActiveMap.remove(assetKey);
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
            case SOUND -> {
                SoundAsset sound = soundActiveMap.remove(assetKey);
                if (sound != null) {
                    sound.stop();
                    sound.free();
                } else {
                    DynamicAssetObject dao = pendingObjects.remove(assetKey);
                    if (dao != null) {
                        pendingEvents.values().forEach(list -> list.remove(dao));
                        dao.reset();
                        daoFreePool.offer(dao);
                    }
                }
            }
            case MUSIC -> {
                MusicAsset music = musicActiveMap.remove(assetKey);
                if (music != null) {
                    music.stop();
                    music.free();
                } else {
                    DynamicAssetObject dao = pendingObjects.remove(assetKey);
                    if (dao != null) {
                        pendingEvents.values().forEach(list -> list.remove(dao));
                        dao.reset();
                        daoFreePool.offer(dao);

                    }
                }
            }
        }

    }

    public synchronized void disposeAll() {
        textureActiveMap.clear();
        pendingObjects.clear();
        pendingEvents.clear();

        for (SoundAsset sound : soundActiveMap.values()) {
            if (sound != null) {
                sound.stop();
                sound.free();
            }
        }
        soundActiveMap.clear();

        for (MusicAsset sound : musicActiveMap.values()) {
            if (sound != null) {
                sound.stop();
                sound.free();
            }
        }
        musicActiveMap.clear();

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

        InternalSoundModule.shutdown();
    }

    public class SoundAPI {
        public void setGlobalVolume(double volume) {InternalSoundModule.setGlobalVolume(volume);}
        public double getGlobalVolume() {return InternalSoundModule.getGlobalVolume();}
        public boolean isInitialized() {return InternalSoundModule.isInitialized();}
    }
}