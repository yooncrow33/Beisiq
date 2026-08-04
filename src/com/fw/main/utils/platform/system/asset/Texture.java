package com.fw.main.utils.platform.system.asset;

import com.fw.internal.utils.InternalUtils;
import sun.misc.Unsafe;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.io.File;
import java.lang.reflect.Field;

public class Texture implements AutoCloseable {
    private GraphicsConfiguration config;
    private volatile String assetKey;
    private volatile String path;
    private volatile boolean closed = false;

    private long offHeapAddress = 0;
    private int bufferSize = 0;

    private VolatileImage volatileImage;
    private int width;
    private int height;
    private volatile boolean inUse = false;

    public Texture() {
        this.config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }

    public void init(String assetKey, String path) {
        this.assetKey = assetKey;
        this.path = path;
        this.inUse = true;
        this.closed = false;
    }

    public void setInUse(boolean use) { this.inUse = use; }

    public synchronized void loadData() throws Exception {
        if (closed) return;
        BufferedImage tempImg = null;

        if (AssetManager.isResourcePath(path)) {
            String resourcePath = path.trim();
            if (resourcePath.startsWith("classpath:")) {
                resourcePath = resourcePath.substring("classpath:".length());
            }
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }

            try (java.io.InputStream is = Texture.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    tempImg = ImageIO.read(is);
                }
            }
        } else {
            File file = new File(this.path);
            if (file.exists()) {
                tempImg = ImageIO.read(file);
            }
        }

        if (tempImg == null) throw new RuntimeException("Texture load fail: " + this.path);

        this.width = tempImg.getWidth();
        this.height = tempImg.getHeight();
        int[] pixels = new int[width * height];
        tempImg.getRGB(0, 0, width, height, pixels, 0, width);

        this.bufferSize = pixels.length * 4;

        try {
            offHeapAddress = AssetManager.unsafe.allocateMemory(bufferSize);
            AssetManager.unsafe.copyMemory(pixels, Unsafe.ARRAY_INT_BASE_OFFSET, null, offHeapAddress, bufferSize);
        }catch (OutOfMemoryError e) {
            if (offHeapAddress != 0) AssetManager.unsafe.freeMemory(offHeapAddress);
            offHeapAddress = 0;
            throw new Exception("Off-Heap Memory allocate fail: " + this.path, e);
        } finally {
            tempImg.flush();
        }
    }

    private void createAndCopyVolatileImage() {
        this.volatileImage = config.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
        copyToVolatile();
    }

    private synchronized void copyToVolatile() {
        if (offHeapAddress == 0 || closed) return;

        BufferedImage viewImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] viewPixels = ((DataBufferInt) viewImg.getRaster().getDataBuffer()).getData();

        AssetManager.unsafe.copyMemory(
                null, offHeapAddress,
                viewPixels, Unsafe.ARRAY_INT_BASE_OFFSET,
                bufferSize
        );

        Graphics2D g = volatileImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(viewImg, 0, 0, null);
        g.dispose();

        viewImg.flush();
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
        if (offHeapAddress != 0) {
            AssetManager.unsafe.freeMemory(offHeapAddress);
            offHeapAddress = 0;
            bufferSize = 0;
        }
        inUse = false;
        assetKey = null;
        path = null;
    }

    public boolean isInUse() { return inUse; }
    public String getAssetKey() { return assetKey; }
}