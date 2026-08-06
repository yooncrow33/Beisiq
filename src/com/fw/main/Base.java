package com.fw.main;

import com.fw.internal.sys.input.MouseAtBase;
import com.fw.main.api.io.Io;
import com.fw.internal.sys.operator.OperatorManager;
import com.fw.internal.sys.view.IFrameSize;
import com.fw.internal.sys.view.ViewMetrics;
import com.fw.internal.utils.InternalUtils;
import com.fw.main.api.sys.ConsoleCMD;
import com.fw.main.api.sys.graphics.Call;
import com.fw.main.utils.graphics.RenderingOption;
import com.fw.main.utils.input.korean.KoreanModule;
import com.fw.main.utils.input.mouse.MouseInterface;
import com.fw.main.utils.io.IoUtils;
import com.fw.main.utils.platform.system.asset.AssetManager;
import com.fw.main.utils.platform.system.asset.Texture;
import com.fw.main.utils.platform.system.console.Console;
import com.fw.main.utils.platform.system.console.autoComplete.AutoCompleteManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferStrategy;
import java.awt.image.VolatileImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Base extends Canvas implements IFrameSize {
    private static final long RESIZE_SETTLE_NANOS = 150_000_000L;

    public static String version = "SI 0.9.2";
    public JFrame frame = new JFrame("Beisiq Engine");

    private Thread logicThread;
    private Thread renderThread;
    private volatile boolean running = false;
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
    protected final Io io = new Io();
    private final OperatorManager operatorManager = new OperatorManager();
    private final AssetInit assetInit = new AssetInit();

    private BufferStrategy bufferStrategy;
    //for legacy rendering.
    private VolatileImage vramBuffer;

    private final ArrayList<Call> drawCalls = new ArrayList<>(1024);
    private final ArrayList<Integer> drawCallXs = new ArrayList<>(1024);
    private final ArrayList<Integer> drawCallYs = new ArrayList<>(1024);

    private final ArrayList<Call> renderTargetCalls = new ArrayList<>(1024);
    private final ArrayList<Integer> renderTargetXs = new ArrayList<>(1024);
    private final ArrayList<Integer> renderTargetYs = new ArrayList<>(1024);

    private KoreanModule koreanModule;
    private MouseAtBase mouseAtBase;

    private ConsoleCMD consoleCMD = null;
    public ConsoleCMD getConsoleCMD() {return consoleCMD;}
    Console console = null;
    private Texture logo;

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
            koreanModule = new KoreanModule(this);
        }

        if (builder.integerKey!=null) { Fw.add(builder.integerKey, this); }
        if (builder.stringKey!=null) { Fw.add(builder.stringKey, this); }
        if (builder.consoleUse) {
            console = new Console(this);
        }
        this.renderingOption = builder.renderingOption;

        mouseAtBase = new MouseAtBase(this);
        init(io, assetInit, operatorManager);
        logo = assetManager.load(AssetManager.LoadMode.SYNC,"logo",InternalUtils.getJarResourceFolder()+"Beisiq2.PNG",null);

        launch();

        new Thread(() -> {
            io.load.loadStart = true;
            File assetFolder = new File(InternalUtils.getAssetFolder());
            if (!assetFolder.exists()) {
                assetFolder.mkdirs();
            }
            setConsole(new ConsoleInit());
            setMouse(getMouse());
            if (console!=null) {io.addIoObject("quickputsystem",console.getQuickPutManager());}
            for (Map.Entry<String, String> entry : assetInit.textureAssets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                assetManager.load(AssetManager.LoadMode.SYNC,key,value,null);
            }
            io.load.load();
            io.load.loadEnd = true;
        }).start();
    }

    public static class Builder {
        String stringKey;
        Integer integerKey;
        boolean consoleUse;
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

    public class AssetInit {
        public enum RootType {
            IS_ON_RESOURCE,
            IS_ON_PROJECT_FOLDER,
            CUSTOM
        }

        private final Map<String, String> textureAssets = new LinkedHashMap<>();

        /**
         * If RootType is custom, It needs full path.
         * If RootType is isOnResource or isOnProjectFolder, It needs only file name.
         * @param fileNameOrPath
         */
        public void registerBootAsset(RootType rootType, String key, String fileNameOrPath) {
            if (key == null || fileNameOrPath == null || rootType == null) {
                throw new IllegalArgumentException("Key, fileName, and rootType must not be null.");
            }

            String fullPath = switch (rootType) {
                case CUSTOM -> fileNameOrPath;
                case IS_ON_RESOURCE -> IoUtils.getCurrentResourceFolder() + fileNameOrPath;
                case IS_ON_PROJECT_FOLDER -> IoUtils.getAssetFolderInProjectFolder() + fileNameOrPath;
            };

            textureAssets.put(key, fullPath);
        }
    }

    private void launch() {
        System.out.println(InternalUtils.Time.getTimeFormate() + " / logic thread start");

        running = true;
        logicThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double targetFps = 60.0;
            final long nsPerTick = (long) (1000000000.0 / targetFps);

            while (running) {
                long now = System.nanoTime();
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                try {
                    if (io.load.isLoadEnd()) {
                        update(deltaTime);
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
                        running = false;
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

        running = true;
        renderThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double targetFps = 60.0;
            final long nsPerTick = (long) (1000000000.0 / targetFps);

            while (running) {
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
                        running = false;
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
            boolean loadingComplete = io.load.isLoadEnd();
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

                    if (!io.load.isLoadEnd()) {
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
            if (!io.load.isLoadEnd()) {
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

    public abstract void init(Io io,AssetInit assetInit, OperatorManager operators);
    public abstract void update(double dt);
    public abstract void render(Graphics g);
    public void setMouse(Mouse mouse) {}
    public void setConsole(ConsoleInit consoleInit) {}

    /**
     * It's uses buffer cashing. but, It's too experimental. Prone to race conditions.
     * It's slower than default render! so this is experimental function.
     */
    public void experimentalRendering(Renderer r) {}

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

    public final void save() {
        io.save.save();
    }

    public void exit() {
        save();
        operatorManager.exitOperatorPack.launch();

        running = false;
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
}
//-Dsun.java2d.opengl=true
