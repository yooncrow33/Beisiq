package com.fw.main.utils.platform.system.console;

import com.fw.internal.utils.Internal;
import com.fw.main.Base;
import com.fw.main.Core;
import com.fw.main.api.io.IoInterface;
import com.fw.main.utils.graphics.RU;
import com.fw.main.utils.input.korean.TextObject;
import com.fw.main.utils.input.korean.TextObjectEventListener;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.InternalSoundModule;
import com.fw.main.utils.platform.system.console.autoComplete.AutoCompleteManager;
import com.fw.main.PerformanceReader;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Console {
    public enum LogType {
        ROOT("root: "),
        CONSOLE("[Console] "),
        SYSTEM("[System] "),
        ERROR("[Console] Error: "),
        @Internal
        /**
         *It's use only internal option.
         */
        SAFE_RUNTIME("[Safe_Runtime] ");

        private final String prefix;
        LogType(String prefix) { this.prefix = prefix; }
        public String get() { return prefix; }
    }

    private static final Font FONT_LOG = new Font("Consolas", Font.PLAIN, 16);
    private static final Font FONT_LOG_INDEX = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_PROMPT = new Font("Consolas", Font.BOLD, 18);
    private static final Font FONT_SUGGEST_TOP = new Font("Consolas", Font.BOLD, 14);
    private static final Font FONT_SUGGEST_SUB = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_ALL_CANDIDATES = new Font("Consolas", Font.PLAIN, 13);

    private static final Color COLOR_BG = new Color(10, 10, 10, 240);
    private static final Color COLOR_BORDER = new Color(240, 240, 240);
    private static final Color COLOR_LOG_ERROR = new Color(250, 80, 80);
    private static final Color COLOR_LOG_ROOT = Color.WHITE;
    private static final Color COLOR_LOG_SYS = Color.GREEN;
    private static final Color COLOR_LOG_SAFE_RUNTIME = Color.YELLOW;
    private static final Color COLOR_LOG_DEFAULT = Color.LIGHT_GRAY;
    private static final Color COLOR_GRAY_TEXT = Color.GRAY;

    private static final Color COLOR_PANEL_BG = new Color(20, 20, 20, 220);
    private static final Color COLOR_PANEL_BORDER = new Color(80, 80, 80);
    private static final Color COLOR_CANDIDATES_TEXT = new Color(130, 130, 130);

    private static final BasicStroke STROKE_BORDER = new BasicStroke(3f);

    private static final Font FONT_WARNING = new Font("Consolas", Font.BOLD, 12);
    private static final Color COLOR_WARN_BG = new Color(50, 20, 20, 230);
    private static final Color COLOR_WARN_BORDER = new Color(220, 80, 80);
    private static final Color COLOR_WARN_TEXT = new Color(255, 180, 180);

    private boolean isOpen = false;

    private final List<String> logs = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxLines = 10;
    @Internal
    private final QuickPutManager quickPutManager = new QuickPutManager();
    public QuickPutManager getQuickPutManager() {
        return quickPutManager;
    }
    private AutoCompleteManager autoCompleteManager = new AutoCompleteManager();

    private final TextObject text;

    Base base;

    public Console(Base comp) {
        this.text = new TextObject();
        text.registerKoreanObjectEventListener(new TextObjectEventListener() {
            @Override
            public void enter() {
                enterAtConsole();
            }
            @Override
            public void tab() {
                handleTabCompletion();
            }
        });
        this.base = comp;
        this.targetCanvas = comp;

        comp.setFocusable(true);
        initBinding();

        getAuto().suggestAt(0,"sys");
        getAuto().suggestAt(1,"copy").whenToken(0).is("sys");
        getAuto().suggestAt(1,"drct").whenToken(0).is("sys");
        getAuto().suggestAt(1,"quickput").whenToken(0).is("sys");
        getAuto().suggestAt(2,"10").whenToken(0).is("sys").
                whenToken(1).is("copy");
        getAuto().suggestAt(2,"10").whenToken(0).is("sys").
                whenToken(1).is("drct");
        getAuto().suggestAt(2,"put").whenToken(0).is("sys")
                .whenToken(1).is("quickput");
        getAuto().suggestAt(2,"delete").whenToken(0).is("sys")
                .whenToken(1).is("quickput");
        getAuto().suggestAt(3,"key").whenToken(0).is("sys")
                .whenToken(1).is("quickput").whenToken(2).is("delete");
        getAuto().suggestAt(3,"key").whenToken(0).is("sys")
                .whenToken(1).is("quickput").whenToken(2).is("put");
        getAuto().suggestAt(4,"CMD").whenToken(0).is("sys")
                .whenToken(1).is("quickput").whenToken(2).is("put").
                whenToken(3).is("key");
        getAuto().suggestAt(2,"clear").whenToken(0).is("sys")
                .whenToken(1).is("quickput");
        getAuto().suggestAt(1,"up").whenToken(0).is("sys");
        getAuto().suggestAt(1,"down").whenToken(0).is("sys");
        getAuto().suggestAt(1,"gc").whenToken(0).is("sys");
        getAuto().suggestAt(1,"getInfo").whenToken(0).is("sys");
        getAuto().suggestAt(2,"ver").whenToken(0).is("sys").
                whenToken(1).is("getInfo");
        getAuto().suggestAt(3,"engine").whenToken(0).is("sys").
                whenToken(1).is("getInfo").whenToken(2).is("ver");
        getAuto().suggestAt(3,"sound").whenToken(0).is("sys").
                whenToken(1).is("getInfo").whenToken(2).is("ver");
        getAuto().suggestAt(3,"built").whenToken(0).is("sys").
                whenToken(1).is("getInfo").whenToken(2).is("ver");
        getAuto().suggestAt(4,"jdk").whenToken(0).is("sys").
                whenToken(1).is("getInfo").whenToken(2).is("ver")
                .whenToken(3).is("built");
        getAuto().suggestAt(4,"vm").whenToken(0).is("sys").
                whenToken(1).is("getInfo").whenToken(2).is("ver")
                .whenToken(3).is("built");
        getAuto().suggestAt(1,"exe").whenToken(0).is("sys");
        getAuto().suggestAt(2,"performanceReader").whenToken(0).is("sys").
                whenToken(1).is("exe");
        getAuto().suggestAt(2,"testInConsole").whenToken(0).is("sys").
                whenToken(1).is("exe");
        getAuto().suggestAt(3,"errorMessage").whenToken(0).is("sys").
                whenToken(1).is("exe").whenToken(2).is("testInConsole");
        getAuto().suggestAt(3,"consoleMessage").whenToken(0).is("sys").
                whenToken(1).is("exe").whenToken(2).is("testInConsole");
        getAuto().suggestAt(3,"systemMessage").whenToken(0).is("sys").
                whenToken(1).is("exe").whenToken(2).is("testInConsole");
        getAuto().suggestAt(3,"safe_runtimeMessage").whenToken(0).is("sys").
                whenToken(1).is("exe").whenToken(2).is("testInConsole");
    }

    public boolean isOpen() { return isOpen; }
    public void toggle() { isOpen = !isOpen; text.setFocused(!text.isFocused()); text.clear(); }

    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
    }
    public void scrollUp(int lines) {
        int maxOffset = Math.max(0, logs.size() - maxLines);
        scrollOffset = Math.min(scrollOffset + lines, maxOffset);
    }

    public void scrollDown(int lines) {
        scrollOffset = Math.max(0, scrollOffset - lines);
    }

    public void CMD(List<String> cmd) {
        if (base.getConsoleCMD()!=null) {
            base.getConsoleCMD().CMD(cmd);
        }
    }

    public AutoCompleteManager getAuto() { return autoCompleteManager; }

    @Internal
    public void enterAtConsole() {
        String input = text.getInputText().trim();
        if (input.isEmpty()) {text.clear();return;}
        if (hasConsecutiveSpaces(input)) {
            addLog(LogType.ERROR, "multiple consecutive spaces detected.");
            return;
        }
        if (hasSyntaxError(input)) {
            addLog(LogType.ERROR, "Unclosed quotes detected in command.");
            return;
        }

        List<String> args = parseBuffer(input, false);
        if (args.isEmpty()) {return;}

        if (args.get(0) != null && args.get(0).equals("sys")) {
            internalCMD(args);
            return;
        }

        logs.add(String.format("[%tT] %s%s", System.currentTimeMillis(), LogType.ROOT.get(), input));
        scrollOffset = 0;
        CMD(args);
        text.clear();
    }

    @Internal
    public boolean hasConsecutiveSpaces(String sb) {
        if (sb == null) {
            return false;
        }
        boolean inQuotes = false;
        for (int i = 0; i < sb.length() - 1; i++) {
            char c = sb.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            if (!inQuotes && c == ' ' && sb.charAt(i + 1) == ' ') {
                return true;
            }
        }
        return false;
    }

    @Internal
    public boolean hasSyntaxError(String sb) {
        if (sb == null) {
            return false;
        }

        boolean inQuotes = false;
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '"') {
                inQuotes = !inQuotes; // 따옴표 토글
            }
        }

        // 루프가 끝났는데 inQuotes가 true면 따옴표가 닫히지 않은 것
        if (inQuotes) {
            return true;
        }

        return false;
    }

    @Internal
    public List<String> parseBuffer(String sb, boolean preserveTrailingEmpty) {
        if (sb == null || sb.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
        }

        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        } else if (preserveTrailingEmpty && (sb.endsWith(" ") || sb.endsWith("\""))) {
            result.add("");
        }

        return result;
    }

    public void addLog(LogType type, String message) {
        logs.add(String.format("[%tT] %s%s", System.currentTimeMillis(), type.get(), message));
        scrollOffset = 0;
    }

    @Internal
    public void render(Graphics g) {
        if (!isOpen || g == null) return;
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(COLOR_BG);
        g2.fillRect(0, 0, 1920, 340);

        g2.setColor(COLOR_BORDER);
        g2.setStroke(STROKE_BORDER);
        g2.drawLine(0, 340, 1920, 340);

        int lineHeight = 25;
        int startY = 40;
        int totalLogs = logs.size();
        int endIndex = totalLogs - scrollOffset;
        int startIndex = Math.max(0, endIndex - maxLines);
        int lineCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
            String line = logs.get(i);

            g2.setFont(FONT_LOG);
            if (line.contains("Error")) g2.setColor(COLOR_LOG_ERROR);
            if (line.contains("[Safe_Runtime]")) g2.setColor(COLOR_LOG_SAFE_RUNTIME);
            else if (line.contains("root:")) g2.setColor(COLOR_LOG_ROOT);
            else if (line.contains("[System]")) g2.setColor(COLOR_LOG_SYS);
            else g2.setColor(COLOR_LOG_DEFAULT);

            g2.drawString(line, 30, startY + (lineCount * lineHeight));

            int currentY = startY + (lineCount * lineHeight);

            g2.setFont(FONT_LOG_INDEX);
            g2.setColor(COLOR_GRAY_TEXT);
            g2.drawString(String.format("#%d", i), 1750, currentY);

            lineCount++;
        }

        g2.setFont(FONT_LOG_INDEX);
        g2.setColor(COLOR_GRAY_TEXT);
        g2.drawString(String.format("Lines: %d/%d", endIndex, totalLogs), 1820, 30);

        int promptX = 30;
        int promptY = 310;

        String promptPrefix = "root@" + Core.get().getProjectName().toLowerCase() + ":~$ ";
        String currentInput = text.getInputText();
        String fullPrompt = promptPrefix + currentInput;

        g2.setFont(FONT_PROMPT);
        g2.setColor(COLOR_BORDER);
        RU.drawStringWithCursor(g2,promptPrefix,text,"",promptX,promptY,3,RU.CursorPosition.TOP);

        FontMetrics fmPrompt = g2.getFontMetrics(FONT_PROMPT);
        int cursorX = promptX + fmPrompt.stringWidth(fullPrompt);
        int cursorY = promptY;

        String cursor = (System.currentTimeMillis() % 1000 > 300) ? "_" : "";
        g2.drawString(cursor, cursorX, cursorY);

        List<String> allCandidates = getAllCandidates();
        List<String> suggestions = getCurrentSuggestions();

        int boxPadding = 8;
        int itemHeight = 20;

        if (!allCandidates.isEmpty()) {
            int maxWordWidth = 0;
            for (String cand : allCandidates) {
                maxWordWidth = Math.max(maxWordWidth, fmPrompt.stringWidth(cand));
            }
            int panelWidth = Math.max(maxWordWidth + (boxPadding * 2), 80);
            int panelHeight = (allCandidates.size() * itemHeight) + (boxPadding * 2);

            int leftPanelX = cursorX - panelWidth - 10;
            int leftPanelY = cursorY + 15;

            g2.setColor(COLOR_PANEL_BG);
            g2.fillRect(leftPanelX, leftPanelY, panelWidth, panelHeight);
            g2.setColor(COLOR_PANEL_BORDER);
            g2.drawRect(leftPanelX, leftPanelY, panelWidth, panelHeight);

            g2.setFont(FONT_SUGGEST_SUB);
            g2.setColor(COLOR_CANDIDATES_TEXT);
            for (int idx = 0; idx < allCandidates.size(); idx++) {
                String candidateWord = allCandidates.get(idx);
                int textY = leftPanelY + boxPadding + (idx + 1) * itemHeight - 4;
                g2.drawString(candidateWord, leftPanelX + boxPadding, textY);
            }
        }

        if (!suggestions.isEmpty()) {
            int maxWordWidth = 0;
            for (String sug : suggestions) {
                maxWordWidth = Math.max(maxWordWidth, fmPrompt.stringWidth(sug));
            }
            int panelWidth = maxWordWidth + (boxPadding * 2);
            int panelHeight = (suggestions.size() * itemHeight) + (boxPadding * 2);

            int rightPanelX = cursorX + 10;
            int rightPanelY = cursorY + 15;

            g2.setColor(COLOR_PANEL_BG);
            g2.fillRect(rightPanelX, rightPanelY, panelWidth, panelHeight);
            g2.setColor(COLOR_PANEL_BORDER);
            g2.drawRect(rightPanelX, rightPanelY, panelWidth, panelHeight);

            for (int idx = 0; idx < suggestions.size(); idx++) {
                String suggestionWord = suggestions.get(idx);
                int textY = rightPanelY + boxPadding + (idx + 1) * itemHeight - 4;

                if (idx == 0) {
                    g2.setFont(FONT_SUGGEST_TOP);
                    g2.setColor(COLOR_LOG_ROOT);
                } else {
                    g2.setFont(FONT_SUGGEST_SUB);
                    g2.setColor(COLOR_GRAY_TEXT);
                }
                g2.drawString(suggestionWord, rightPanelX + boxPadding, textY);
            }
        }
        g2.setFont(FONT_WARNING);
        FontMetrics fmWarn = g2.getFontMetrics(FONT_WARNING);

        int warnBoxHeight = 22;
        int warnBoxY = cursorY - 30;

        if (hasConsecutiveSpaces(currentInput)) {
            String msg = "Consecutive spaces detected";
            int warnWidth = fmWarn.stringWidth(msg) + 12;
            int warnX = cursorX + 10;

            if (warnX + warnWidth > 1900) {
                warnX = 1900 - warnWidth;
            }

            g2.setColor(COLOR_WARN_BG);
            g2.fillRect(warnX, warnBoxY, warnWidth, warnBoxHeight);
            g2.setColor(COLOR_WARN_BORDER);
            g2.drawRect(warnX, warnBoxY, warnWidth, warnBoxHeight);

            g2.setColor(COLOR_WARN_TEXT);
            g2.drawString(msg, warnX + 6, warnBoxY + 15);

            warnBoxY -= (warnBoxHeight + 4);
        }

        if (hasSyntaxError(currentInput)) {
            String msg = "Unclosed quotes (\")";
            int warnWidth = fmWarn.stringWidth(msg) + 12;
            int warnX = cursorX + 10;

            if (warnX + warnWidth > 1900) {
                warnX = 1900 - warnWidth;
            }

            g2.setColor(COLOR_WARN_BG);
            g2.fillRect(warnX, warnBoxY, warnWidth, warnBoxHeight);
            g2.setColor(COLOR_WARN_BORDER);
            g2.drawRect(warnX, warnBoxY, warnWidth, warnBoxHeight);

            g2.setColor(COLOR_WARN_TEXT);
            g2.drawString(msg, warnX + 6, warnBoxY + 15);
        }

        g.setColor(Color.CYAN);
        g.setFont(FONT_WARNING);
        g.drawString(String.format(
                "FPS: %d | frame: %.2f ms | work: %.2f ms | " +
                        "scale: %.6f / requested: %.6f | physical: %.3f (%s%s)",
                base.getFps(),
                base.getFrameTimeMs(),
                base.getRenderWorkTimeMs(),
                base.getViewScale(),
                base.getRequestedViewScale(),
                base.getPhysicalViewScale(),
                base.isFractionalPhysicalScale() ? "fractional" : "integer",
                base.isViewScaleSnapped() ? ", snapped" : ""
        ), 5, 15);
    }

    private static final int CONSOLE_KEY_CODE = KeyEvent.VK_BACK_QUOTE;
    private final Canvas targetCanvas;

    private void initBinding() {
        targetCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == CONSOLE_KEY_CODE) {
                    toggle();
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == CONSOLE_KEY_CODE) {
                    e.consume();
                }
            }
        });
    }


    public void internalCMD(List<String> args) {
        if (args == null || args.size() < 2 || !"sys".equals(args.get(0))) {
            return;
        }

        switch (args.get(1)) {
            case "up" -> {
                if (args.size() < 3) {
                    scrollUp(1);
                } else {
                    int value;
                    try {
                        value = Integer.parseInt(args.get(2));
                    } catch (NumberFormatException e) {
                        addLog(LogType.ERROR, "value is not a number!");
                        return;
                    }
                    scrollUp(value);
                }
            }
            case "down" -> {
                if (args.size() < 3) {
                    scrollDown(1);
                } else {
                    int value;
                    try {
                        value = Integer.parseInt(args.get(2));
                    } catch (NumberFormatException e) {
                        addLog(LogType.ERROR, "value is not a number!");
                        return;
                    }
                    scrollDown(value);
                }
            }
            case "drct" -> {
                if (args.size() < 3) {
                    addLog(LogType.ERROR,"line is null!");
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(args.get(2));
                } catch (NumberFormatException e) {
                    addLog(LogType.ERROR, "value is not a number!");
                    return;
                }

                if (value < 0 || value >= logs.size()) {
                    addLog(LogType.ERROR, "out of line range!");
                    return;
                }

                int targetOffset = logs.size() - value;
                int maxOffset = Math.max(0, logs.size() - maxLines);

                scrollOffset = Math.min(targetOffset, maxOffset);
                text.clear();
            }
            case "copy" -> {
                if (args.size() < 3) {
                    addLog(LogType.ERROR,"line is null!");
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(args.get(2));
                } catch (NumberFormatException e) {
                    addLog(LogType.ERROR, "value is not a number!");
                    return;
                }
                if (value < 0 || value >= logs.size()) {
                    addLog(LogType.ERROR, "out of line range!");
                    return;
                }
                text.setInputText(logs.get(value).substring(17));
            }
            case "gc" -> {
                base.assetManager.clearGarbage();
                text.clear();
            }
            case "getInfo" -> {
                if (args.size() < 3) {
                    addLog(LogType.ERROR,"fuc is null!");
                    return;
                }
                switch (args.get(2)) {
                    case "ver" -> {
                        if (args.size() < 4) {
                            addLog(LogType.ERROR,"object is null!");
                            return;
                        }
                        switch (args.get(3)) {
                            case "engine" -> {
                                addLog(LogType.SYSTEM, Base.version);
                                text.clear();
                            }
                            case "sound" -> {
                                addLog(LogType.SYSTEM, "BeisiqTinySound: " + InternalSoundModule.VERSION);
                                text.clear();
                            }
                            case "built" -> {
                                if (args.size() < 5) {
                                    addLog(LogType.ERROR,"object is null!");
                                    return;
                                }
                                switch (args.get(4)) {
                                    case "jdk" -> {
                                        addLog(LogType.SYSTEM, "open jdk 21");
                                        text.clear();
                                    }
                                    case "vm" -> {
                                        addLog(LogType.SYSTEM, "graalvm 21.0.7");
                                        text.clear();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            case "quickput" -> {
                if (args.size() < 3) {
                    addLog(LogType.ERROR,"3rd token is null!");
                    return;
                }
                switch (args.get(2)) {
                    case "put" -> {
                        if (args.size() < 4) {
                            addLog(LogType.ERROR,"key is null!");
                            return;
                        }
                        if (args.size() < 5) {
                            addLog(LogType.ERROR,"cmd is null!");
                            return;
                        }
                        if (args.size() > 5) {
                            addLog(LogType.ERROR,"should be using \"\" !");
                            return;
                        }
                        getQuickPutManager().map.put(args.get(3),args.get(4));
                        text.clear();
                    }
                    case "delete" -> {
                        if (args.size() < 4) {
                            addLog(LogType.ERROR,"key is null!");
                            return;
                        }
                        getQuickPutManager().map.remove(args.get(3));
                        text.clear();
                    }
                    case "clear" -> {
                        getQuickPutManager().map.clear();
                        text.clear();
                    }
                }
            }
            case "exe" -> {
                if (args.size() < 3) {
                    addLog(LogType.ERROR,"object is null!");
                    return;
                }
                switch (args.get(2)) {
                    case "performanceReader" -> {
                        new PerformanceReader().launch();
                    }
                    case "consoleHelper" -> {
                        //new Help();
                    }
                    case "testInConsole" -> {
                        if (args.size() < 4) {
                            addLog(LogType.ERROR,"object is null!");
                            return;
                        }
                        String message = "test";

                        switch (args.get(3)) {
                            case "errorMessage" -> {
                                addLog(LogType.ERROR,message);
                            }
                            case "consoleMessage" -> {
                                addLog(LogType.CONSOLE,message);
                            }
                            case "systemMessage" -> {
                                addLog(LogType.SYSTEM,message);
                            }
                            case "safe_runtimeMessage" -> {
                                addLog(LogType.SAFE_RUNTIME,message);
                            }
                            default -> {

                            }
                        }
                    }
                    default -> {

                    }
                }
                text.clear();
            }
            default -> {
                List<Map.Entry<String, String>> entryList = new ArrayList<>(getQuickPutManager().map.entrySet());
                for (int i = 0; i < entryList.size(); i++) {
                    Map.Entry<String, String> entry = entryList.get(i);
                    String token = args.get(1);
                    if (token.equals(entry.getKey())) {
                        text.setInputText(entry.getValue());
                    }
                }
            }
        }
    }

    @Internal
    public class QuickPutManager implements IoInterface {
        private Map<String, String> map = new ConcurrentHashMap<>();

        @Override
        public void save(Properties p) {
            List<Map.Entry<String, String>> entryList = new ArrayList<>(map.entrySet());

            p.setProperty("count", String.valueOf(entryList.size()));

            for (int i = 0; i < entryList.size(); i++) {
                Map.Entry<String, String> entry = entryList.get(i);
                String prefix = "q" + i + "_";

                p.setProperty(prefix + "key", entry.getKey());
                p.setProperty(prefix + "cmd", entry.getValue());
            }
        }

        @Override
        public void load(Properties p) {
            int count = Integer.parseInt(p.getProperty("count", "0"));
            Map<String, String> loadedMap = new ConcurrentHashMap<>();

            for (int i = 0; i < count; i++) {
                String prefix = "q" + i + "_";
                String key = p.getProperty(prefix + "key");
                String cmd = p.getProperty(prefix + "cmd");

                if (key != null && cmd != null) {
                    loadedMap.put(key, cmd);
                }
            }

            this.map = loadedMap;
        }

        @Override public void initLoad(Properties p) { }
    }

    public void handleTabCompletion() {
        String inputText = text.getInputText();
        List<String> currentTokens = parseBuffer(inputText, true);

        if (currentTokens.isEmpty()) return;

        String currentToken = currentTokens.get(currentTokens.size() - 1);

        if (currentToken.isEmpty()) return;

        List<String> candidates = autoCompleteManager.getCandidates(currentTokens, currentToken);

        if (!candidates.isEmpty()) {
            String topCandidate = candidates.get(0);

            currentTokens.set(currentTokens.size() - 1, topCandidate);
            String completedInput = String.join(" ", currentTokens) + " ";

            text.setInputText(completedInput);
        }
    }

    public List<String> getCurrentSuggestions() {
        String inputText = text.getInputText();
        List<String> currentTokens = parseBuffer(inputText, true);
        if (currentTokens.isEmpty()) {
            return Collections.emptyList();
        }
        String currentToken = currentTokens.get(currentTokens.size() - 1);
        return autoCompleteManager.getCandidates(currentTokens, currentToken);
    }

    public List<String> getAllCandidates() {
        String inputText = text.getInputText();
        List<String> currentTokens = parseBuffer(inputText, true);
        if (currentTokens.isEmpty()) {
            return Collections.emptyList();
        }
        return autoCompleteManager.getAllCandidates(currentTokens);
    }
}