package com.fw.main.utils.platform.system.scene;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.utils.platform.system.asset.AssetManager;
import com.fw.main.utils.platform.system.asset.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class TempTexture {
    private final String path;
    public BufferedImage tempImg;

    public AtomicInteger w,h;
    public final String key;

    public TempTexture(String path, String key) throws Exception {
        this.path = path;
        this.key = key;
    }

    public void loadData() throws Exception {
        tempImg = null;

        if (InternalUtils.isResourcePath(path)) {
            String resourcePath = path.trim();
            if (resourcePath.startsWith("classpath:")) {
                resourcePath = resourcePath.substring("classpath:".length());
            }
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }

            try (java.io.InputStream is = TempTexture.class.getResourceAsStream(resourcePath)) {
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

        w = new AtomicInteger(tempImg.getWidth());
        h = new AtomicInteger(tempImg.getHeight());

        if (tempImg == null) throw new RuntimeException("Texture loadTexture fail: " + this.path);
    }
}
