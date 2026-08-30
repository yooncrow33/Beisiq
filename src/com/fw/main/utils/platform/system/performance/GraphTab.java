package com.fw.main.utils.platform.system.performance;

import com.fw.main.utils.input.mouse.FwMouseAPI;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GraphTab {
    private final String title;
    private final Long[] data;
    private final Map<Integer, String> phaseMap;

    private float offsetX = 0;
    private float zoomX = 2.0f;
    private int lastMouseX;
    private boolean isDragging = false;

    private double overallAvg;
    private long overallMax = Long.MIN_VALUE;
    private long overallMin = Long.MAX_VALUE;

    private final List<PhaseRange> phases = new ArrayList<>();
    private int selectedPhaseIndex = -1;

    private static final int AXIS_LEFT = 75;
    private static final int AXIS_BOTTOM = 25;
    private static final int PADDING_TOP = 25;
    private static final int BOTTOM_UI_HEIGHT = 135;

    public static class PhaseRange {
        public String name;
        public int startTick;
        public int endTick;
        public double avg;
        public long max = Long.MIN_VALUE;
        public long min = Long.MAX_VALUE;
        public int maxTick = -1;
        public int minTick = -1;
    }

    public GraphTab(String title, Long[] data, Map<Integer, String> phaseMap) {
        this.title = title;
        this.data = (data != null) ? data : new Long[0];
        this.phaseMap = (phaseMap != null) ? phaseMap : new TreeMap<>();
        initStatsAndPhases();
    }

    private void initStatsAndPhases() {
        if (data.length == 0) {
            overallMax = 0;
            overallMin = 0;
            overallAvg = 0;
            return;
        }

        double totalSum = 0;
        for (long val : data) {
            totalSum += val;
            if (val > overallMax) overallMax = val;
            if (val < overallMin) overallMin = val;
        }
        overallAvg = totalSum / data.length;

        List<Integer> ticks = new ArrayList<>(phaseMap.keySet());
        Collections.sort(ticks);

        for (int i = 0; i < ticks.size(); i++) {
            int start = ticks.get(i);
            int end = (i + 1 < ticks.size()) ? ticks.get(i + 1) - 1 : data.length - 1;
            if (start >= data.length) continue;
            end = Math.min(end, data.length - 1);

            PhaseRange p = new PhaseRange();
            p.name = phaseMap.get(start) + " Phase";
            p.startTick = start;
            p.endTick = end;

            double pSum = 0;
            int count = 0;
            for (int t = start; t <= end; t++) {
                long val = data[t];
                pSum += val;
                if (val > p.max) {
                    p.max = val;
                    p.maxTick = t;
                }
                if (val < p.min) {
                    p.min = val;
                    p.minTick = t;
                }
                count++;
            }
            p.avg = (count > 0) ? (pSum / count) : 0;
            phases.add(p);
        }
    }

    public void render(Graphics2D g, int x, int y, int width, int height) {
        int innerGraphX = x + AXIS_LEFT;
        int innerGraphY = y + PADDING_TOP;
        int innerGraphW = width - AXIS_LEFT;
        int innerGraphH = height - BOTTOM_UI_HEIGHT - AXIS_BOTTOM - PADDING_TOP;

        g.setColor(new Color(14, 14, 18));
        g.fillRect(x, y, width, height - BOTTOM_UI_HEIGHT);

        Shape oldClip = g.getClip();
        g.setClip(innerGraphX, y, innerGraphW, innerGraphH + PADDING_TOP);

        g.setColor(new Color(20, 20, 25));
        g.fillRect(innerGraphX, innerGraphY, innerGraphW, innerGraphH);

        for (int i = 0; i < phases.size(); i++) {
            PhaseRange p = phases.get(i);
            float startScreenX = innerGraphX + offsetX + (p.startTick * zoomX);
            float endScreenX = innerGraphX + offsetX + ((p.endTick + 1) * zoomX);

            g.setColor((i % 2 == 0) ? new Color(28, 30, 38) : new Color(23, 24, 30));
            g.fillRect((int) startScreenX, innerGraphY, (int) (endScreenX - startScreenX), innerGraphH);

            g.setColor(new Color(160, 175, 195));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(p.name, startScreenX + 8, y + 16);
        }

        int tickStep = calculateOptimalTickStep(zoomX);
        int startVisibleTick = Math.max(0, (int) (-offsetX / zoomX) - 1);
        int endVisibleTick = Math.min(data.length - 1, (int) ((-offsetX + innerGraphW) / zoomX) + 1);
        int firstAlignedTick = (startVisibleTick / tickStep) * tickStep;

        for (int t = firstAlignedTick; t <= endVisibleTick; t += tickStep) {
            if (t < 0 || t >= data.length) continue;
            int tickScreenX = (int) (innerGraphX + offsetX + (t * zoomX));
            g.setColor(new Color(50, 55, 65, 80));
            g.drawLine(tickScreenX, innerGraphY, tickScreenX, innerGraphY + innerGraphH);
        }

        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            float ratio = (float) i / steps;
            int lineY = (int) (innerGraphY + innerGraphH - (ratio * innerGraphH));
            g.setColor(new Color(55, 60, 72, 110));
            g.drawLine(innerGraphX, lineY, innerGraphX + innerGraphW, lineY);
        }

        long range = overallMax - overallMin;
        if (data.length > 1 && range > 0) {
            g.setColor(new Color(75, 220, 130));
            for (int i = 0; i < data.length - 1; i++) {
                int x1 = (int) (innerGraphX + offsetX + (i * zoomX));
                int y1 = (int) (innerGraphY + innerGraphH - ((double) (data[i] - overallMin) / range * innerGraphH));
                int x2 = (int) (innerGraphX + offsetX + ((i + 1) * zoomX));
                int y2 = (int) (innerGraphY + innerGraphH - ((double) (data[i + 1] - overallMin) / range * innerGraphH));

                if (x2 >= innerGraphX && x1 <= innerGraphX + innerGraphW) {
                    g.drawLine(x1, y1, x2, y2);
                }
            }

            for (PhaseRange p : phases) {
                if (p.maxTick >= 0) {
                    int mx = (int) (innerGraphX + offsetX + (p.maxTick * zoomX));
                    int my = (int) (innerGraphY + innerGraphH - ((double) (p.max - overallMin) / range * innerGraphH));
                    if (mx >= innerGraphX - 50 && mx <= innerGraphX + innerGraphW + 50) {
                        drawValueBadge(g, mx, my, "MAX " + formatValue(p.max), true);
                    }
                }
                if (p.minTick >= 0) {
                    int nx = (int) (innerGraphX + offsetX + (p.minTick * zoomX));
                    int ny = (int) (innerGraphY + innerGraphH - ((double) (p.min - overallMin) / range * innerGraphH));
                    if (nx >= innerGraphX - 50 && nx <= innerGraphX + innerGraphW + 50) {
                        drawValueBadge(g, nx, ny, "MIN " + formatValue(p.min), false);
                    }
                }
            }
        }
        g.setClip(oldClip);

        g.setColor(new Color(70, 75, 88));
        g.drawRect(innerGraphX, innerGraphY, innerGraphW, innerGraphH);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= steps; i++) {
            float ratio = (float) i / steps;
            int lineY = (int) (innerGraphY + innerGraphH - (ratio * innerGraphH));
            long val = (long) (overallMin + ratio * range);
            String valStr = formatValue(val);

            g.setColor(new Color(140, 150, 168));
            g.drawString(valStr, innerGraphX - fm.stringWidth(valStr) - 8, lineY + 4);
        }

        int bottomAxisY = innerGraphY + innerGraphH + 16;
        Shape bottomClip = g.getClip();
        g.setClip(innerGraphX, innerGraphY + innerGraphH, innerGraphW, AXIS_BOTTOM);

        for (int t = firstAlignedTick; t <= endVisibleTick; t += tickStep) {
            if (t < 0 || t >= data.length) continue;
            int tickScreenX = (int) (innerGraphX + offsetX + (t * zoomX));
            g.setColor(new Color(130, 140, 160));
            g.drawString(String.valueOf(t), tickScreenX - (fm.stringWidth(String.valueOf(t)) / 2), bottomAxisY);
        }
        g.setClip(bottomClip);

        int subTabY = y + height - BOTTOM_UI_HEIGHT + 20;
        int subTabX = x + AXIS_LEFT;
        for (int i = 0; i < phases.size(); i++) {
            PhaseRange p = phases.get(i);
            int tabW = 120;
            g.setColor(selectedPhaseIndex == i ? new Color(65, 85, 135) : new Color(38, 40, 48));
            g.fillRect(subTabX, subTabY, tabW, 28);
            g.setColor(selectedPhaseIndex == i ? Color.CYAN : new Color(90, 95, 110));
            g.drawRect(subTabX, subTabY, tabW, 28);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(p.name, subTabX + 10, subTabY + 19);
            subTabX += tabW + 10;
        }

        int statY = subTabY + 54;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(String.format("[Total Stats]  Avg: %.2f  |  Max: %s  |  Min: %s",
                overallAvg, formatValue(overallMax), formatValue(overallMin)), x + AXIS_LEFT, statY);

        if (selectedPhaseIndex >= 0 && selectedPhaseIndex < phases.size()) {
            PhaseRange p = phases.get(selectedPhaseIndex);
            g.setColor(new Color(255, 215, 0));
            g.drawString(String.format("[%s]  Avg: %.2f  |  Max: %s (Tick %d)  |  Min: %s (Tick %d)  |  Ticks: %d ~ %d",
                    p.name, p.avg, formatValue(p.max), p.maxTick, formatValue(p.min), p.minTick, p.startTick, p.endTick), x + AXIS_LEFT, statY + 22);
        } else {
            g.setColor(Color.GRAY);
            g.drawString("[Phase Stats]  Click a phase tab above to view segmented metrics.", x + AXIS_LEFT, statY + 22);
        }
    }

    private void drawValueBadge(Graphics2D g, int x, int y, String text, boolean isMax) {
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int badgeW = textW + 10;
        int badgeH = 16;

        int badgeX = x - (badgeW / 2);
        int badgeY = isMax ? (y + 6) : (y - badgeH - 6);

        g.setColor(new Color(255, 215, 0));
        g.fillOval(x - 3, y - 3, 6, 6);

        g.setColor(new Color(20, 20, 25, 230));
        g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
        g.setColor(new Color(255, 215, 0));
        g.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

        g.drawString(text, badgeX + 5, badgeY + 12);
    }

    private String formatValue(long value) {
        if (title.contains("BYTES") || title.contains("HEAP")) {
            if (value >= 1024L * 1024 * 1024) return String.format("%.1fGB", value / (1024.0 * 1024 * 1024));
            if (value >= 1024L * 1024) return String.format("%.1fMB", value / (1024.0 * 1024));
            if (value >= 1024L) return String.format("%.1fKB", value / 1024.0);
            return value + "B";
        }
        if (title.contains("NS") || title.contains("WORK")) {
            if (value >= 1_000_000_000L) return String.format("%.2fs", value / 1e9);
            if (value >= 1_000_000L) return String.format("%.2fms", value / 1e6);
            if (value >= 1_000L) return String.format("%.1fus", value / 1e3);
            return value + "ns";
        }
        if (title.contains("PERCENT") || title.contains("CPU")) {
            return value + "%";
        }
        return String.valueOf(value);
    }

    private int calculateOptimalTickStep(float currentZoom) {
        float minPixelDistance = 70.0f;
        int[] stepCandidates = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000};
        for (int step : stepCandidates) {
            if (step * currentZoom >= minPixelDistance) {
                return step;
            }
        }
        return 10000;
    }

    public void onMousePressed(int mouseX) {
        lastMouseX = mouseX;
        isDragging = true;
    }

    public void onMouseReleased() {
        isDragging = false;
    }

    public void onMouseDragged(int currentMouseX, int graphWidth) {
        if (isDragging) {
            int deltaX = currentMouseX - lastMouseX;
            offsetX += deltaX;
            lastMouseX = currentMouseX;
            clampOffset(graphWidth - AXIS_LEFT);
        }
    }

    public void onMouseWheel(FwMouseAPI e, int mouseX, int graphWidth) {
        int rot = e.getWheelRotation();
        if (rot == 0) return;

        float oldZoom = zoomX;
        if (rot < 0) {
            zoomX = Math.min(zoomX * 1.15f, 60.0f);
        } else {
            zoomX = Math.max(zoomX / 1.15f, 0.05f);
        }
        offsetX = mouseX - (mouseX - offsetX) * (zoomX / oldZoom);
        clampOffset(graphWidth - AXIS_LEFT);
    }

    private void clampOffset(int graphInnerWidth) {
        if (data.length == 0) return;
        float totalGraphWidth = (data.length - 1) * zoomX;

        if (totalGraphWidth <= graphInnerWidth) {
            if (offsetX > 0) offsetX = 0;
            if (offsetX < graphInnerWidth - totalGraphWidth) offsetX = Math.max(0, graphInnerWidth - totalGraphWidth);
        } else {
            float minOffset = graphInnerWidth - totalGraphWidth;
            float maxOffset = 0;
            if (offsetX < minOffset) offsetX = minOffset;
            if (offsetX > maxOffset) offsetX = maxOffset;
        }
    }

    public void onSubTabClick(int mx, int my, int x, int y, int height) {
        int subTabY = y + height - BOTTOM_UI_HEIGHT + 20;
        int subTabX = x + AXIS_LEFT;
        for (int i = 0; i < phases.size(); i++) {
            int tabW = 120;
            if (mx >= subTabX && mx <= subTabX + tabW && my >= subTabY && my <= subTabY + 28) {
                selectedPhaseIndex = (selectedPhaseIndex == i) ? -1 : i;
                break;
            }
            subTabX += tabW + 10;
        }
    }

    public String getTitle() { return title; }
}