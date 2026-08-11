package com.test;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.api.io.DynamicIo;
import com.fw.main.api.io.DynamicIoLoadObject;
import com.fw.main.*;
import com.fw.main.api.io.IoInterface;
import com.fw.main.api.sys.ConsoleCMD;
import com.fw.main.utils.graphics.RenderingOption;
import com.fw.main.utils.input.korean.KoreanObject;
import com.fw.main.utils.input.korean.KoreanObjectEventListener;
import com.fw.main.utils.io.IoUtils;
import com.fw.main.utils.platform.system.asset.Texture;

import java.awt.*;
import java.util.List;
import java.util.Properties;

public class Test extends Base {
    KoreanObject ko = new KoreanObject();
    float updatable;
    int degree = 1;
    int degree2 = 1;
    Texture logo;

    static {
        Core.setConfig(new
                Config.Builder("CivitasTest"). // = folder name.
                setWindowWidth(1280).
                setWindowHeight(720).
                setUseKoreanModule(true).
                setUseIntegerPhysicalScaling(true).
                setEncryptionKey("keyforencryption").
                setUseEncryption(false).build()
        );
    }

    public Test() {
        super(new Builder().
                setIntegerKey(1).
                setStringKey("1").
                setUseConsole(true).
                setRenderingOption(RenderingOption.DEFAULT)
        );
        ko.setFocused(true);
        ko.registerKoreanObjectEventListener(new KoreanObjectEventListener() {
            @Override
            public void enter() {
                System.out.println(ko.getInputText());
                ko.clear();
            }
            @Override
            public void tab() {

            }
        });

        logo = assetManager.getTexture("logo");
    }

    @Override
    public void setConsole(Base.ConsoleInit c) {
        c.registerConsoleCMD(new ConsoleCMD() {
            @Override
            public void CMD(List<String> args) {
                if (args.get(0).equals("test")) {
                    degree = Integer.parseInt(args.get(1));
                }
                if (args.get(0).equals("test2")) {
                    degree2 = Integer.parseInt(args.get(1));
                }
            }
        });
    }

    @Override
    public void init(BaseInit init) {
        new TestBindingLegacy(this);

        assetManager.mallocTexturePool(3000);
        assetManager.mallocLazyLoadPool(500);

        for (int i = 0; i<500; i++) {
            init.getAssetInit().registerBootAsset("index_"+i,
                    InternalUtils.getEngineResourceStream("Beisiq2.png"));
        }

        init.getOperatorManager().exitOperatorPack.addOperator(new Operator() {
            @Override
            public void exe() {
                System.out.println("exit");
            }
        });

        init.getOperatorManager().exitOperatorPack.addOperator(new Operator() {
            @Override
            public void exe() {
                System.exit(0);
            }
        });

        init.getIo().addIoObject("default", new IoInterface() {
            @Override
            public void save(Properties p) {
                p.setProperty("float", Float.toString(updatable));
            }

            @Override
            public void load(Properties p) {
                updatable = Float.parseFloat((String) p.get("float"));
            }

            @Override
            public void initLoad(Properties p) {
                updatable = (float) Math.random();
            }
        });

        new DynamicIoLoadObject("full path....", new DynamicIo() {
            @Override
            public void load(Properties p) {
                //loads...
            }
        }).launch();
    }

    @Override
    public void update(double dt) {
        updatable = (float) Math.random();
    }

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.setFont(new Font("", Font.BOLD, 18));
        g.drawString("Do Test", 50, 80);

        g.setColor(Color.CYAN);
        g.drawString(ko.getInputText() + "_", 50, 130);

        g.drawString(String.format(
                "FPS: %d | frame: %.2f ms | work: %.2f ms | " +
                        "scale: %.6f / requested: %.6f | physical: %.3f (%s%s)",
                getFps(),
                getFrameTimeMs(),
                getRenderWorkTimeMs(),
                getViewScale(),
                getRequestedViewScale(),
                getPhysicalViewScale(),
                isFractionalPhysicalScale() ? "fractional" : "integer",
                isViewScaleSnapped() ? ", snapped" : ""
        ), 550, 800);

        for (int i = 0; i < degree; i++) {
            g.drawRect(200 + (i & 1023), 200, 10, 10);
        }
        for (int i = 0; i < degree2; i++) {
            g.drawImage(logo.getVolatileImage(),200 + (i & 1023), 220, 10, 10,null);
        }

    }

    public static void main(String[] args) {
        new Test();
    }
}