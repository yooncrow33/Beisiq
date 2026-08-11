package com.fw.main.utils.input.korean;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.UUID;

public class TextObject {
    final UUID id = UUID.randomUUID();
    TextObjectEventListener listener;
    private int cursorIndex = 0;

    private final StringBuilder textBuffer = new StringBuilder();
    private String composingText = "";
    private boolean focused = false;
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            TextManager.activeObjectPut(this);
        } else {
            TextManager.activeObjectRemove(this);
        }
    }
    public boolean isFocused() { return this.focused; }
    public StringBuilder getTextBuffer() { return textBuffer; }
    public String getComposingText() { return composingText; }
    public void setComposingText(String composingText) { this.composingText = composingText; }

    public void clear() {
        textBuffer.setLength(0);
        composingText = "";
        cursorIndex = 0;
    }

    public String getInputText() {
        return textBuffer.toString() + composingText;
    }

    public void setInputText(String newText) {
        clear();
        if (newText != null) {
            textBuffer.append(newText);
            cursorIndex = textBuffer.length();
        }
    }

    public void moveCursorLeft() {
        confirmComposing();
        if (cursorIndex > 0) cursorIndex--;
    }

    public void moveCursorRight() {
        confirmComposing();
        if (cursorIndex < textBuffer.length()) cursorIndex++;
    }

    public void confirmComposing() {
        if (!composingText.isEmpty()) {
            textBuffer.insert(cursorIndex, composingText);
            cursorIndex += composingText.length();
            composingText = "";
        }
    }

    public int getCursorIndex() { return cursorIndex; }
    public void setCursorIndex(int index) {
        this.cursorIndex = Math.max(0, Math.min(index, textBuffer.length()));
    }

    public void pasteClipboardText() {
        String clipboardText = getClipboardText();
        if (clipboardText == null || clipboardText.isEmpty()) {
            return;
        }

        confirmComposing();

        textBuffer.insert(cursorIndex, clipboardText);
        cursorIndex += clipboardText.length();
    }

    public TextObject() {
        TextManager.koreanObjectPut(this);
    }

    public void registerKoreanObjectEventListener(TextObjectEventListener textObjectEventListener) {this.listener = textObjectEventListener;}

    public void free() {
        TextManager.koreanObjectRemove(this);
    }

    static String getClipboardText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
