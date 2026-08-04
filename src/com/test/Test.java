package com.test;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.api.io.DynamicIo;
import com.fw.main.api.io.DynamicIoLoadObject;
import com.fw.main.api.io.Io;
import com.fw.internal.sys.operator.OperatorManager;
import com.fw.main.*;
import com.fw.main.api.io.IoInterface;
import com.fw.main.api.sys.ConsoleCMD;
import com.fw.main.utils.graphics.RenderingOption;
import com.fw.main.utils.input.korean.KoreanObject;
import com.fw.main.utils.input.korean.KoreanObjectEventListener;
import com.fw.main.utils.platform.system.asset.Texture;

import java.awt.*;
import java.util.List;
import java.util.Properties;

public class Test extends Base {
    KoreanObject ko = new KoreanObject();
    float aFloat;
    int degree = 1;
    int degree2 = 1;

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
                setRenderingOption(RenderingOption.LEGACY)
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
    public void init(Io io, AssetInit assetInit, OperatorManager operatorManager) {
        new TestBindingLegacy(this);

        assetManager.malloc(3000);

        operatorManager.exitOperatorPack.addOperator(new Operator() {
            @Override
            public void exe() {
                System.out.println("exit");
            }
        });

        io.addIoObject("default", new IoInterface() {
            @Override
            public void save(Properties p) {
                p.setProperty("float", Float.toString(aFloat));
            }

            @Override
            public void load(Properties p) {
                aFloat = Float.parseFloat((String) p.get("float"));
            }

            @Override
            public void initLoad(Properties p) {
                aFloat = (float) Math.random();
            }
        });

        new DynamicIoLoadObject("full path....", new DynamicIo() {
            @Override
            public void load(Properties p) {
                //loads...
            }
        }).launch();

        for (int i = 0; i<200; i++) {
            assetInit.registerBootAsset(AssetInit.RootType.CUSTOM,"test"+i, InternalUtils.getJarResourceFolder()+"Beisiq2.PNG");
        }
    }



    @Override
    public void update(double dt) {
        aFloat = (float) Math.random();
    }

    @Override
    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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
        g.drawString("degree : "+degree,550,830);
        g.drawString("degree2: "+degree2,550,860);

        for (int i = 0; degree2>i; i++) {
            g.drawRect(330,330,30,30);
        }
        for (int i = 0; degree>i; i++) {
            //g.drawImage(texture.getVolatileImage(), 0,0,360,640,null);
        }
    }

    public static void main(String[] args) {
        new Test();
    }
}