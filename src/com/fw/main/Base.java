package com.fw.main;

import com.fw.internal.sys.input.MouseAtBase;
import com.fw.main.api.io.Io;
import com.fw.internal.sys.operator.OperatorManager;
import com.fw.internal.sys.view.IFrameSize;
import com.fw.internal.sys.view.ViewMetrics;
import com.fw.internal.utils.InternalUtils;
import com.fw.main.api.sys.ConsoleCMD;
import com.fw.main.api.sys.graphics.Call;
import com.fw.main.utils.graphics.RU;
import com.fw.main.utils.graphics.RenderingOption;
import com.fw.main.utils.input.korean.TextModule;
import com.fw.main.utils.input.mouse.MouseInterface;
import com.fw.internal.utils.DynamicAsset;
import com.fw.main.utils.platform.system.asset.AssetManager;
import com.fw.main.utils.platform.system.asset.Texture;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.InternalSoundModule;
import com.fw.main.utils.platform.system.console.Console;
import com.fw.main.utils.platform.system.console.autoComplete.AutoCompleteManager;
import com.fw.main.utils.platform.system.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferStrategy;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Base extends Canvas implements IFrameSize {
    private static final long RESIZE_SETTLE_NANOS = 150_000_000L;

    public static String version = "PRE 0.0.3";
    public JFrame frame = new JFrame("Beisiq Engine");

    private Thread logicThread;
    private Thread renderThread;
    private AtomicBoolean running = new AtomicBoolean(false);
    private volatile long renderPausedUntilNanos = 0L;
    private RenderingOption renderingOption;

    private int fpsCounter = 0;
    private volatile int currentFps = 0;
    private volatile double currentFrameTimeMs = 0;
    private volatile double currentRenderWorkTimeMs = 0;
    private long lastFpsCheckTime = System.nanoTime();
    private long accumulatedRenderWorkNanos = 0L;

    private final Mouse mouse = new Mouse(this);
    public final Mouse getMouse() { return mouse; }
    private final ViewMetrics viewMetrics;
    public final AssetManager assetManager = new AssetManager(this);
    final Io io = new Io();
    final OperatorManager operatorManager = new OperatorManager();
    private final AssetInit assetInit = new AssetInit();
    private final BaseInit baseInit = new BaseInit(this);
    private float sysInitProgress = 0.0f;
    private float loadInitProgress = 0.0f;
    private float assetInitProgress = 0.0f;
    private float sceneInitProgress = 0.0f;
    AtomicBoolean initLoadEnd = new AtomicBoolean(false);
    ArrayList<DynamicAsset> sysLoadStack = new ArrayList<>();
    public AtomicBoolean isChangeScene = new AtomicBoolean(false);
    public Scene getCurrentScene() { return currentScene; }
    Scene currentScene = null;
    Scene pendingScene;
    InitLoadState initLoadState;

    private BufferStrategy bufferStrategy;
    //for legacy rendering.
    private VolatileImage vramBuffer;

    private final ArrayList<Call> drawCalls = new ArrayList<>(1024);
    private final ArrayList<Integer> drawCallXs = new ArrayList<>(1024);
    private final ArrayList<Integer> drawCallYs = new ArrayList<>(1024);

    private final ArrayList<Call> renderTargetCalls = new ArrayList<>(1024);
    private final ArrayList<Integer> renderTargetXs = new ArrayList<>(1024);
    private final ArrayList<Integer> renderTargetYs = new ArrayList<>(1024);

    private TextModule textModule;
    private MouseAtBase mouseAtBase;

    private ConsoleCMD consoleCMD = null;
    public ConsoleCMD getConsoleCMD() {return consoleCMD;}
    Console console = null;
    private Texture logo;

    Font loadingMessageFont = new Font(Font.MONOSPACED,Font.BOLD,48);

    public Base(Builder builder) {
        if (!Core.isIsSetConfig()) {
            System.err.println("config is null! you should init config to Core.java in the static block!");
            System.exit(0);
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(true);

        this.setPreferredSize(new Dimension(Core.get().initWindowWidth, Core.get().getInitWindowHeight()));
        setFocusable(true);
        setIgnoreRepaint(true);

        viewMetrics = new ViewMetrics(
                this,
                Core.get().isUseIntegerPhysicalScaling()
        );

        frame.add(this);
        frame.pack();
        frame.setVisible(true);
        this.requestFocus();

        setBackground(Color.BLACK);
        viewMetrics.calculateViewMetrics();

        this.createBufferStrategy(2);
        this.bufferStrategy = this.getBufferStrategy();

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                viewMetrics.updateVirtualMouse(e.getX(), e.getY());
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                viewMetrics.calculateViewMetrics();
                renderPausedUntilNanos = System.nanoTime() + RESIZE_SETTLE_NANOS;
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                viewMetrics.calculateViewMetrics();
                renderPausedUntilNanos = System.nanoTime() + RESIZE_SETTLE_NANOS;
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                viewMetrics.calculateViewMetrics();
                renderPausedUntilNanos = System.nanoTime() + RESIZE_SETTLE_NANOS;
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                exit();
            }
        });

        if (Core.get().isUseKoreanModule()) {
            textModule = new TextModule(this);
        }

        if (builder.integerKey!=null) { Fw.add(builder.integerKey, this); }
        if (builder.stringKey!=null) { Fw.add(builder.stringKey, this); }
        if (builder.consoleUse) {
            console = new Console(this);
        }
        this.renderingOption = builder.renderingOption;

        mouseAtBase = new MouseAtBase(this);
        this.init(baseInit);
        if (baseInit.initScene != null) {
            currentScene = baseInit.initScene; }
        logo = assetManager.loadTexture(AssetManager.LoadMode.SYNC,"logo",InternalUtils.getEngineResourceStream("Beisiq2.png"),null);
        launch();

        sysLoadStack.add(() -> {
            File assetFolder = new File(System.getProperty("user.home") + File.separator + "." + Core.get().projectName + File.separator + "asset");
            if (!assetFolder.exists()) {
                assetFolder.mkdirs();
            }
        });
        sysLoadStack.add(() -> setConsole(new ConsoleInit()));
        sysLoadStack.add(() -> setMouse(getMouse()));
        sysLoadStack.add(() -> {
            if (console!=null) {io.addIoObject("quickputsystem",console.getQuickPutManager());}
        });
        new Thread(() -> {
            initLoadState = InitLoadState.sys;
            io.load.loadStart = true;
            int maxSysInitProgress = sysLoadStack.size();
            int sysInitProgress = 0;
            for (DynamicAsset dynamicAsset : sysLoadStack) {
                dynamicAsset.load();
                sysInitProgress++;
                this.sysInitProgress = (float) sysInitProgress / maxSysInitProgress * 100;
            }

            initLoadState = InitLoadState.assetInit;
            int maxAssetInitProgress = assetInit.textureAssets.size();
            int assetInitProgress = 0;
            for (Map.Entry<String, InputStream> entry : assetInit.textureAssets.entrySet()) {
                String key = entry.getKey();
                InputStream value = entry.getValue();

                assetManager.loadTexture(AssetManager.LoadMode.SYNC,key,value,null);
                assetInitProgress++;
                this.assetInitProgress = (float) assetInitProgress / maxAssetInitProgress * 100;
            }

            initLoadState = InitLoadState.io;
            io.load.load();
            io.load.loadEnd = true;

            initLoadState = InitLoadState.sceneInit;
            sceneInitProgress = 0.5f;
            if (currentScene!=null) {
                try {
                    currentScene.init();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            sceneInitProgress = 1.0f;
            initLoadEnd.set(true);
        }).start();
    }

    public static class Builder {
        String stringKey;
        Integer integerKey;
        boolean consoleUse;
        Scene initScene = null;

        RenderingOption renderingOption;

        public Builder setStringKey(String stringKey) {
            this.stringKey = stringKey;
            return this;
        }

        public Builder setIntegerKey(Integer integerKey) {
            this.integerKey = integerKey;
            return this;
        }

        public Builder setUseConsole(boolean b) {
            this.consoleUse = b;
            return this;
        }

        public Builder setRenderingOption(RenderingOption renderingOption) {
            this.renderingOption = renderingOption;
            return this;
        }
    }

    public class Mouse {
        final Base base;

        public Mouse(Base base) {
            this.base = base;
        }

        public int x() { return base.getMouseX(); }
        public int y() { return base.getMouseY(); }
        public void registerMouseInterface(MouseInterface mouseInterface) { mouseAtBase.registerInterface(mouseInterface); }
    }

    public class ConsoleInit {
        public void registerConsoleCMD(ConsoleCMD CMD) { if(consoleCMD!=null) {
            System.err.println("ConsoleCMD is already init!"); return;} consoleCMD = CMD;}
        public AutoCompleteManager getAuto() {return console.getAuto();}
    }

    public class BaseInit {
        Base base;
        Scene initScene;

        BaseInit(Base base) {
            this.base = base;
        }

        public Io getIo() {return base.io;}
        public OperatorManager getOperatorManager() {return operatorManager;}
        public AssetInit getAssetInit() {return assetInit;}
        public void initSound() {
            InternalSoundModule.init();
        }
        public void setInitScene(Scene initScene) {
            this.initScene = initScene;
        }
    }

    public class AssetInit {
        public enum RootType {
            IS_ON_RESOURCE,
            IS_ON_PROJECT_FOLDER,
            CUSTOM
        }

        private final Map<String, InputStream> textureAssets = new LinkedHashMap<>();

        /**
         * If RootType is custom, It needs full path.
         * If RootType is isOnResource or isOnProjectFolder, It needs only file name.
         */
        public void registerBootAsset(String key, InputStream is) {
            if (key == null || is == null) {
                throw new IllegalArgumentException("Key and InputStream must not be null.");
            }

            textureAssets.put(key, is);
        }
    }

    private void launch() {
        System.out.println(InternalUtils.Time.getTimeFormate() + " / logic thread start");

        running.set(true);
        logicThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double targetFps = 60.0;
            final long nsPerTick = (long) (1000000000.0 / targetFps);

            while (running.get()) {
                long now = System.nanoTime();
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                try {
                    if (!isChangeScene.get()) {
                        update(deltaTime);
                        return;
                    }
                    if (isChangeScene.get() && pendingScene != null) {
                        if (currentScene != null) {
                            try {
                                Method method = currentScene.getClass().getDeclaredMethod("dispose");
                                method.setAccessible(true);
                                method.invoke(currentScene);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        currentScene = pendingScene;
                        pendingScene = null;

                        new Thread(() -> {
                            try {
                                assetManager.clearGarbage();
                                currentScene.init();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            } finally {
                                System.gc();
                                isChangeScene.set(false);
                            }
                        }).start();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                long timeTaken = System.nanoTime() - now;
                long timeLeftNs = nsPerTick - timeTaken;

                if (timeLeftNs > 2_000_000) {
                    try {
                        Thread.sleep((timeLeftNs - 2_000_000) / 1_000_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running.set(false);
                    }
                }

                while (System.nanoTime() - now < nsPerTick) {
                    Thread.yield();
                }
            }
        });

        logicThread.setName("logicLoop");
        logicThread.start();

        System.out.println(InternalUtils.Time.getTimeFormate() + " / render thread start");

        running.set(true);
        renderThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double targetFps = 60.0;
            final long nsPerTick = (long) (1000000000.0 / targetFps);

            while (running.get()) {
                long now = System.nanoTime();
                lastTime = now;

                try {
                    renderLoop();
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                long timeTaken = System.nanoTime() - now;
                long timeLeftNs = nsPerTick - timeTaken;

                if (timeLeftNs > 2_000_000) {
                    try {
                        Thread.sleep((timeLeftNs - 2_000_000) / 1_000_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running.set(false);
                    }
                }

                while (System.nanoTime() - now < nsPerTick) {
                    Thread.yield();
                }
            }
        });

        renderThread.setName("renderLoop");
        renderThread.start();
    }

    private void renderLoop() {
        if (System.nanoTime() < renderPausedUntilNanos) return;

        if (renderingOption.equals(RenderingOption.DEFAULT) || renderingOption.equals(RenderingOption.EXPERIMENTAL)) {
            BufferStrategy strategy = bufferStrategy;
            if (strategy == null || !isDisplayable()) return;

            int currentWidth = getWidth();
            int currentHeight = getHeight();
            if (currentWidth <= 0 || currentHeight <= 0) return;

            ViewMetrics.Snapshot metrics = viewMetrics.getSnapshot();
            boolean loadingComplete = initLoadEnd.get();
            boolean experimentalFrame = loadingComplete && renderingOption.equals(RenderingOption.EXPERIMENTAL);
            if (experimentalFrame) {
                prepareExperimentalFrame();
            }

            long frameStartNanos = System.nanoTime();
            try {
                do {
                    do {
                        Graphics2D d2 = (Graphics2D) strategy.getDrawGraphics();
                        try {
                            d2.setColor(Color.BLACK);
                            d2.fillRect(0, 0, currentWidth, currentHeight);

                            d2.translate(
                                    metrics.currentXOffset(),
                                    metrics.currentYOffset()
                            );
                            d2.scale(
                                    metrics.currentScale(),
                                    metrics.currentScale()
                            );

                            drawCurrentFrame(d2, loadingComplete);
                        } finally {
                            d2.dispose();
                        }
                    } while (strategy.contentsRestored());

                    strategy.show();
                } while (strategy.contentsLost());
            } finally {
                if (experimentalFrame) {
                    clearExperimentalFrame();
                }
            }

            recordPresentedFrame(System.nanoTime() - frameStartNanos);
        } else if (renderingOption.equals(RenderingOption.LEGACY)) {

            if (bufferStrategy == null) return;
            int currentWidth = getWidth();
            int currentHeight = getHeight();
            if (currentWidth <= 0 || currentHeight <= 0) return;

            if (vramBuffer == null ||
                    vramBuffer.getWidth() != currentWidth ||
                    vramBuffer.getHeight() != currentHeight ||
                    vramBuffer.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
                vramBuffer = getGraphicsConfiguration().createCompatibleVolatileImage(currentWidth, currentHeight);
            }

            long frameStartNanos = System.nanoTime();

            do {
                if (vramBuffer.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_RESTORED) {
                    // 복구 이벤트
                }

                Graphics2D d2 = vramBuffer.createGraphics();
                try {
                    d2.setColor(Color.BLACK);
                    d2.fillRect(0, 0, currentWidth, currentHeight);

                    d2.translate(viewMetrics.getCurrentXOffset(), viewMetrics.getCurrentYOffset());
                    d2.scale(viewMetrics.getCurrentScale(), viewMetrics.getCurrentScale());

                    if (!initLoadEnd.get() || isChangeScene.get()) {
                        renderLoadingScreen(d2);
                    } else {
                        render(d2);
                    }

                    if (Fw.Debugger.showHitbox) {
                        Fw.Debugger.Internal.renderHitbox(d2);
                    }
                    if (console != null) { console.render(d2); }

                } finally {
                    d2.dispose();
                }

                Graphics hwGraphics = bufferStrategy.getDrawGraphics();
                try {
                    hwGraphics.drawImage(vramBuffer, 0, 0, null);
                } finally {
                    hwGraphics.dispose();
                }
                bufferStrategy.show();

            } while (vramBuffer.contentsLost());

            recordPresentedFrame(System.nanoTime() - frameStartNanos);
        }
    }

    private void prepareExperimentalFrame() {
        synchronized (drawCalls) {
            renderTargetCalls.addAll(drawCalls);
            renderTargetXs.addAll(drawCallXs);
            renderTargetYs.addAll(drawCallYs);

            drawCalls.clear();
            drawCallXs.clear();
            drawCallYs.clear();
        }
    }

    private void clearExperimentalFrame() {
        renderTargetCalls.clear();
        renderTargetXs.clear();
        renderTargetYs.clear();
    }

    private void drawCurrentFrame(Graphics2D d2, boolean loadingComplete) {
        if (!loadingComplete) {
            renderLoadingScreen(d2);
        } else if (renderingOption.equals(RenderingOption.EXPERIMENTAL)) {
            for (int i = 0; i < renderTargetCalls.size(); i++) {
                Call call = renderTargetCalls.get(i);
                int x = renderTargetXs.get(i);
                int y = renderTargetYs.get(i);

                if (call != null) {
                    call.updateCache();
                    VolatileImage buffer = call.getBuffer();
                    if (buffer != null) {
                        d2.drawImage(buffer, x, y, null);
                    }
                }
            }
        } else {
            if (!initLoadEnd.get() || isChangeScene.get()) {
                renderLoadingScreen(d2);
            } else {
                render(d2);
            }
        }

        if (Fw.Debugger.showHitbox) {
            Fw.Debugger.Internal.renderHitbox(d2);
        }
        if (console != null) {
            console.render(d2);
        }
    }

    private void recordPresentedFrame(long renderWorkNanos) {
        fpsCounter++;
        accumulatedRenderWorkNanos += renderWorkNanos;

        long currentTime = System.nanoTime();
        long elapsedTime = currentTime - lastFpsCheckTime;
        if (elapsedTime < 1_000_000_000L) return;

        currentFps = (int) Math.round(fpsCounter * 1_000_000_000.0 / elapsedTime);
        currentFrameTimeMs = (elapsedTime / 1_000_000.0) / fpsCounter;
        currentRenderWorkTimeMs = (accumulatedRenderWorkNanos / 1_000_000.0) / fpsCounter;

        fpsCounter = 0;
        accumulatedRenderWorkNanos = 0L;
        lastFpsCheckTime = currentTime;
    }

    /**
     * Returns the currently measured exact FPS.
     */
    public int getFps() {
        return currentFps;
    }

    /**
     * Returns the average time (ms) taken per frame.
     */
    public double getFrameTimeMs() {
        return currentFrameTimeMs;
    }

    /**
     * Average time (in milliseconds) for rendering excluding frame limit wait time and for show().
     */
    public double getRenderWorkTimeMs() {
        return currentRenderWorkTimeMs;
    }

    public double getViewScale() {
        return viewMetrics.getCurrentScale();
    }

    public double getRequestedViewScale() {
        return viewMetrics.getRequestedScale();
    }

    public double getPhysicalViewScale() {
        return viewMetrics.getPhysicalScale();
    }

    public boolean isViewScaleSnapped() {
        return viewMetrics.isScaleSnapped();
    }

    public boolean isFractionalViewScale() {
        double scale = getViewScale();
        return Math.abs(scale - Math.rint(scale)) > 0.000_001;
    }

    public boolean isFractionalPhysicalScale() {
        double scale = getPhysicalViewScale();
        return Math.abs(scale - Math.rint(scale)) > 0.000_001;
    }

    public abstract void init(BaseInit baseInit);
    public abstract void update(double dt);
    public abstract void render(Graphics2D g);
    public void setMouse(Mouse mouse) {}
    public void setConsole(ConsoleInit consoleInit) {}

    /**
     * It's uses buffer cashing. but, It's too experimental. Prone to race conditions.
     * It's slower than default render! so this is experimental function.
     */
    public void experimentalRendering(Renderer r) {}

    public void changeScene(Scene newScene) {
        if (newScene == null) {
            System.err.println("new scene is null!");
            return;
        }
        this.pendingScene = newScene;
        this.isChangeScene.set(true);
    }

    @Override public final int getComponentWidth() { return this.getWidth(); }
    @Override public final int getComponentHeight() { return this.getHeight(); }
    @Override public final double getDeviceScaleX() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        return configuration == null
                ? 1.0
                : configuration.getDefaultTransform().getScaleX();
    }
    @Override public final double getDeviceScaleY() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        return configuration == null
                ? 1.0
                : configuration.getDefaultTransform().getScaleY();
    }

    public final int getMouseX() { return viewMetrics.getVirtualMouseX(); }
    public final int getMouseY() { return viewMetrics.getVirtualMouseY(); }

    private String getLoadingMessage() {
        if (isChangeScene.get()) {
            return "Loading scene...";
        } else {
            switch (initLoadState) {
                case sys -> {
                    return  "Loading sys...";
                }
                case assetInit -> {
                    return "Loading asset...";
                }
                case io -> {
                    return "Loading file...";
                }
                case sceneInit -> {
                    return "Loading Init Scene...";
                }
            }
        }
        return "something wrong";
    }

    public final void save() {
        io.save.save();
    }

    public void exit() {
        save();
        operatorManager.exitOperatorPack.launch();

        running.set(false);
        if (logicThread != null) {
            try {
                logicThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (renderThread != null) {
                try {
                    (renderThread).join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        BufferStrategy strategy = bufferStrategy;
        bufferStrategy = null;
        if (strategy != null) {
            strategy.dispose();
        }

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private void renderLoadingScreen(Graphics g) {
        g.drawImage(logo.getVolatileImage(),0,0,1920,1080,null);
        g.setFont(loadingMessageFont);
        g.setColor(Color.white);
        RU.drawStringCenter(g,getLoadingMessage(),960,850);
    }
    private void addDrawCall(int x, int y, Call call) {
        synchronized (drawCalls) {
            drawCalls.add(call);
            drawCallXs.add(x);
            drawCallYs.add(y);
        }
    }

    public class Renderer {
        public void addDrawCall(int x, int y, Call call) {
            Base.this.addDrawCall(x, y, call);
        }
    }

    public GraphicsConfiguration graphicsConfiguration() {return getGraphicsConfiguration();}

    @Override
    public final java.awt.im.InputMethodRequests getInputMethodRequests() {
        return new java.awt.im.InputMethodRequests() {
            @Override public java.awt.font.TextHitInfo getLocationOffset(int x, int y) { return null; }
            @Override public java.awt.Rectangle getTextLocation(java.awt.font.TextHitInfo offset) {
                return new java.awt.Rectangle(50, 130, 0, 0);
            }
            @Override public java.text.AttributedCharacterIterator getSelectedText(
                    java.text.AttributedCharacterIterator.Attribute[] attributes) { return null; }
            @Override public java.text.AttributedCharacterIterator
            getCommittedText(int beginIndex, int endIndex, java.text.AttributedCharacterIterator.Attribute[] attributes)
            { return null; }
            @Override public int getCommittedTextLength() { return 0; }
            @Override public int getInsertPositionOffset() { return 0; }
            @Override public java.text.AttributedCharacterIterator
            cancelLatestCommittedText(java.text.AttributedCharacterIterator.Attribute[] attributes) { return null; }
        };
    }

    private enum InitLoadState {
        sys,
        assetInit,
        io,
        sceneInit
    }
}
//-Dsun.java2d.opengl=true
