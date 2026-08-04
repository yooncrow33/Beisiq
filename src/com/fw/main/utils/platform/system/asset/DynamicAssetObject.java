package com.fw.main.utils.platform.system.asset;

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
    private final DynamicAsset internalInterface;

    public DynamicAssetObject(DynamicAsset dynamicAsset) {
        internalInterface = dynamicAsset;
    }


    public void launch() {
        if (!loadStart.compareAndSet(false, true)) return;

        loadExecutor.submit(() -> {
            try {
                internalInterface.load();
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
}