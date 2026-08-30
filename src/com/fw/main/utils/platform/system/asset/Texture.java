package com.fw.main.utils.platform.system.asset;

import com.fw.internal.utils.InternalUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Supplier;

public class Texture implements AutoCloseable {
    private GraphicsConfiguration config;
    private volatile String assetKey;
    private volatile InputStream assetStream;
    private volatile boolean closed = false;

    private BufferedImage backupImage;

    private VolatileImage volatileImage;
    private int width;
    private int height;
    private volatile boolean inUse = false;
    final AssetManager assetManager;
    AssetManager.AssetType type2 = AssetManager.AssetType.TEXTURE;
    public AssetManager.AssetType getAssetType() {return type2;}


    Texture(AssetManager assetManager) {
        this.config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        this.assetManager = assetManager;
    }

    public void init(String assetKey, InputStream assetStream) {
        this.assetKey = assetKey;
        this.assetStream = assetStream;
        this.inUse = true;

        this.closed = false;
    }

    public void setInUse(boolean use) { this.inUse = use; }

    public synchronized void loadData() throws Exception {
        if (closed || assetStream == null) return;
        BufferedImage tempImg;

        try (InputStream is = this.assetStream) {
            tempImg = ImageIO.read(is);
        } finally {
            this.assetStream = null;
        }

        if (tempImg == null) {
            throw new RuntimeException("Texture load fail: " + this.assetKey);
        }

        this.width = tempImg.getWidth();
        this.height = tempImg.getHeight();
        this.backupImage = tempImg;
    }

    private void createAndCopyVolatileImage() {
        this.volatileImage = config.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
        copyToVolatile();
    }

    private synchronized void copyToVolatile() {
        if (backupImage == null || closed) return;

        Graphics2D g = volatileImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(backupImage, 0, 0, null);
        g.dispose();
    }

    public VolatileImage getVolatileImage() {
        if (volatileImage == null) {
            createAndCopyVolatileImage();
            return volatileImage;
        }

        int validateCode = volatileImage.validate(config);
        if (validateCode == VolatileImage.IMAGE_INCOMPATIBLE) {
            createAndCopyVolatileImage();
        } else if (validateCode == VolatileImage.IMAGE_RESTORED) {
            copyToVolatile();
        }
        return volatileImage;
    }

    public void flush() {
        if (volatileImage != null) {
            volatileImage.flush();
            volatileImage = null;
        }
    }


    @Override
    public synchronized void close() {
        closed = true;
        flush();

        if (backupImage != null) {
            //쓰래기 통에 담아두고 Texture객체의 backupImage만 비우기.
            //쓰레기 통은 알아서 버림.
            assetManager.addGarbageList(backupImage);
            backupImage = null;
        }

        inUse = false;
        assetKey = null;
        assetStream = null;
    }

    public boolean isInUse() { return inUse; }
    public String getAssetKey() { return assetKey; }
}