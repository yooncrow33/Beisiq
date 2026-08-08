package com.fw.main.utils.platform.system.asset;

import com.fw.internal.utils.DynamicAsset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicAssetObject {
    private static final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AssetLoadThread");
        t.setDaemon(true);
        return t;
    });

    public static void submitTask(Runnable task) {
        loadExecutor.submit(task);
    }

    private final AtomicBoolean loadStart = new AtomicBoolean(false);
    private final AtomicBoolean loadEnd = new AtomicBoolean(false);
    private final AtomicBoolean loadError = new AtomicBoolean(false);
    private DynamicAsset internalInterface;

    public DynamicAssetObject() {}

    public void init(DynamicAsset dynamicAsset) {
        this.internalInterface = dynamicAsset;
        this.loadStart.set(false);
        this.loadEnd.set(false);
        this.loadError.set(false);
    }

    public void launch() {
        if (!loadStart.compareAndSet(false, true)) return;

        loadExecutor.submit(() -> {
            try {
                if (internalInterface != null) {
                    internalInterface.load();
                }
            } catch (Exception e) {
                loadError.set(true);
                System.err.println("Asset Load Error: " + e.getMessage());
            } finally {
                loadEnd.set(true);
            }
        });
    }

    public boolean isLoaded() {
        return loadEnd.get() && !loadError.get();
    }

    public boolean isError() {
        return loadError.get();
    }

    public void reset() {
        this.internalInterface = null;
        this.loadStart.set(false);
        this.loadEnd.set(false);
        this.loadError.set(false);
    }
}