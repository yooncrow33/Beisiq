package com.fw.main.utils.input.korean;

import com.fw.main.Base;

import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;

public class TextModule {

    public TextModule(Base jComponent) {
        jComponent.setFocusTraversalKeysEnabled(false);

        jComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());
        jComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());

        jComponent.enableInputMethods(true);
        jComponent.requestFocusInWindow();

        jComponent.addInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodTextChanged(InputMethodEvent event) {
                if (TextManager.isActiveKoreanObjectIsEmpty()) {
                    return;
                }

                AttributedCharacterIterator text = event.getText();
                String committedStr = "";
                String composingStr = "";

                if (text != null) {
                    int committedCharacterCount = event.getCommittedCharacterCount();
                    char c = text.first();

                    StringBuilder committed = new StringBuilder();
                    for (int i = 0; i < committedCharacterCount; i++) {
                        committed.append(c);
                        c = text.next();
                    }
                    committedStr = committed.toString();

                    StringBuilder composing = new StringBuilder();
                    while (c != AttributedCharacterIterator.DONE && c != '\uffff') {
                        composing.append(c);
                        c = text.next();
                    }
                    composingStr = composing.toString();
                }

                for(TextObject textObject : TextManager.activeObjectsMap.values()) {
                    if (!committedStr.isEmpty()) {
                        textObject.getTextBuffer().insert(textObject.getCursorIndex(), committedStr);
                        textObject.setCursorIndex(textObject.getCursorIndex() + committedStr.length());
                    }
                    textObject.setComposingText(composingStr);
                }

                event.consume();

            }

            @Override
            public void caretPositionChanged(InputMethodEvent event) {}
        });

        jComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (TextManager.isActiveKoreanObjectIsEmpty()) {
                    return;
                }

                for(TextObject textObject : TextManager.activeObjectsMap.values()) {
                    int keyCode = e.getKeyCode();

                    if (keyCode == KeyEvent.VK_V && (e.isControlDown() || e.isMetaDown())) {
                        textObject.pasteClipboardText();
                        e.consume();
                        continue;
                    }

                    if (keyCode == KeyEvent.VK_LEFT) {
                        textObject.moveCursorLeft();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_RIGHT) {
                        textObject.moveCursorRight();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                        if (textObject.getComposingText().length() > 0) {
                            textObject.setComposingText("");
                        } else if (textObject.getCursorIndex() > 0) {
                            // 커서 좌측 글자 삭제
                            textObject.getTextBuffer().deleteCharAt(textObject.getCursorIndex() - 1);
                            textObject.setCursorIndex(textObject.getCursorIndex() - 1);
                        }
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_ENTER) {
                        textObject.listener.enter();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_TAB) {
                        textObject.listener.tab();
                        e.consume();

                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (TextManager.isActiveKoreanObjectIsEmpty()) {
                    return;
                }

                for(TextObject textObject : TextManager.activeObjectsMap.values()) {

                    char c = e.getKeyChar();

                    if (c != KeyEvent.CHAR_UNDEFINED && c >= 32 && c != 127) {
                        if (textObject.getComposingText().length() == 0) {
                            textObject.getTextBuffer().insert(textObject.getCursorIndex(), c);
                            textObject.setCursorIndex(textObject.getCursorIndex() + 1);
                        }
                    }
                }
            }
        });

        java.awt.im.InputContext ic = jComponent.getInputContext();
        if (ic != null) {
            ic.dispatchEvent(new java.awt.event.FocusEvent(jComponent, java.awt.event.FocusEvent.FOCUS_GAINED));
            ic.selectInputMethod(java.util.Locale.getDefault());
        }
    }
}
