package com.fw.main.utils.platform.system.scene;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TempTexture {
    private final InputStream assetStream;
    public BufferedImage tempImg;

    public AtomicInteger w, h;
    public final String key;

    public TempTexture(InputStream assetStream, String key) {
        this.assetStream = assetStream;
        this.key = key;
    }

    public void loadData() throws Exception {
        tempImg = null;

        if (assetStream != null) {
            try (InputStream is = this.assetStream) {
                tempImg = ImageIO.read(is);
            }
        }

        if (tempImg == null) {
            throw new RuntimeException("TempTexture load fail: " + this.key);
        }

        w = new AtomicInteger(tempImg.getWidth());
        h = new AtomicInteger(tempImg.getHeight());
    }
}