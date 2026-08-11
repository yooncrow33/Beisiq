package com.fw.main.utils.graphics;

import com.fw.main.utils.input.korean.TextObject;

import java.awt.*;

//render utils.
public class RU {

    static int[] cursorXPoints = new int[3];
    static int[] cursorYPoints = new int[3];

    public enum CursorPosition {
        TOP, BOTTOM
    }

    public static void drawStringCenter(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();

        int textWidth = metrics.stringWidth(text);

        int drawX = x - (textWidth / 2);

        int textHeight = metrics.getAscent();
        int drawY = y + (textHeight / 2);

        g.drawString(text, drawX, drawY);
    }

    /**
     * KoreanObject의 텍스트와 지정된 커서 위치에 삼각형 커서를 함께 그립니다.
     *
     * @param g Graphics2D 객체
     * @param ko rendering할 KoreanObject
     * @param x 텍스트 베이스라인 시작 X 좌표
     * @param y 텍스트 베이스라인 Y 좌표
     * @param cursorSize 삼각형 커서의 크기(높이/반폭)
     * @param position 커서 위치 (TOP: 텍스트 위, BOTTOM: 텍스트 아래)
     */
    public static void drawStringWithCursor(Graphics2D g, TextObject ko, int x, int y, int cursorSize, CursorPosition position) {
        String fullText = ko.getInputText();

        g.drawString(fullText, x, y);

        FontMetrics fm = g.getFontMetrics();

        int effectiveCursorIndex = ko.getCursorIndex();
        if (ko.getComposingText() != null && !ko.getComposingText().isEmpty()) {
            effectiveCursorIndex += ko.getComposingText().length();
        }

        effectiveCursorIndex = Math.min(effectiveCursorIndex, fullText.length());

        String subText = fullText.substring(0, effectiveCursorIndex);
        int cursorX = x + fm.stringWidth(subText);

        int cursorY;
        if (position == CursorPosition.TOP) {
            cursorY = y - fm.getAscent() - 2;
        } else {
            cursorY = y + fm.getDescent() + 2;
        }

        drawTriangleCursor(g, cursorX, cursorY, cursorSize, position);
    }

    /**
     * KoreanObject 텍스트의 앞/뒤에 추가 문자열(prefix, suffix)을 붙여 렌더링하고,
     * 그에 맞춰 정확한 위치에 삼각형 커서를 그립니다.
     *
     * @param g Graphics2D 객체
     * @param prefix KoreanObject 텍스트 앞에 붙일 문자열 (예: "> ", "[Input] ")
     * @param ko rendering할 KoreanObject
     * @param suffix KoreanObject 텍스트 뒤에 붙일 문자열 (예: " <", " (optional)")
     * @param x 텍스트 베이스라인 시작 X 좌표
     * @param y 텍스트 베이스라인 Y 좌표
     * @param cursorSize 삼각형 커서의 크기
     * @param position 커서 위치 (TOP: 텍스트 위, BOTTOM: 텍스트 아래)
     */
    public static void drawStringWithCursor(Graphics2D g, String prefix, TextObject ko, String suffix, int x, int y, int cursorSize, CursorPosition position) {
        String p = (prefix != null) ? prefix : "";
        String s = (suffix != null) ? suffix : "";
        String koText = ko.getInputText();

        // 1. prefix + KoreanObject 텍스트 + suffix 조합 전체 출력
        String fullText = p + koText + s;
        g.drawString(fullText, x, y);

        FontMetrics fm = g.getFontMetrics();

        // 2. 조합 중인 텍스트 길이 반영
        int effectiveCursorIndex = ko.getCursorIndex();
        if (ko.getComposingText() != null && !ko.getComposingText().isEmpty()) {
            effectiveCursorIndex += ko.getComposingText().length();
        }
        effectiveCursorIndex = Math.min(effectiveCursorIndex, koText.length());

        // 3. 커서 X 위치 계산: prefix 너비 + (0부터 effectiveCursorIndex까지의 KoreanObject 텍스트 너비)
        String subKoText = koText.substring(0, effectiveCursorIndex);
        int cursorX = x + fm.stringWidth(p) + fm.stringWidth(subKoText);

        // 4. 커서 Y 위치 계산
        int cursorY;
        if (position == CursorPosition.TOP) {
            cursorY = y - fm.getAscent() - 2;
        } else {
            cursorY = y + fm.getDescent() + 2;
        }

        // 5. 삼각형 커서 그리기
        drawTriangleCursor(g, cursorX, cursorY, cursorSize, position);
    }

    private static void drawTriangleCursor(Graphics2D g, int x, int y, int size, CursorPosition position) {

        if (position == CursorPosition.TOP) {
            // 아래를 가리키는 삼각형 (▼)
            cursorXPoints[0] = x - size; cursorYPoints[0] = y - size;
            cursorXPoints[1] = x + size; cursorYPoints[1] = y - size;
            cursorXPoints[2] = x;        cursorYPoints[2] = y;
        } else {
            // 위를 가리키는 삼각형 (▲)
            cursorXPoints[0] = x - size; cursorYPoints[0] = y + size;
            cursorXPoints[1] = x + size; cursorYPoints[1] = y + size;
            cursorXPoints[2] = x;        cursorYPoints[2] = y;
        }

        g.fillPolygon(cursorXPoints, cursorYPoints, 3);
    }
}
