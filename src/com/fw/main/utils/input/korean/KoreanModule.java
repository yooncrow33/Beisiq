package com.fw.main.utils.input.korean;

import com.fw.main.Base;

import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;

public class KoreanModule {

    public KoreanModule(Base jComponent) {
        jComponent.setFocusTraversalKeysEnabled(false);

        jComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());
        jComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, java.util.Collections.emptySet());

        jComponent.enableInputMethods(true);
        jComponent.requestFocusInWindow();

        jComponent.addInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodTextChanged(InputMethodEvent event) {
                if (KoreanManager.isActiveKoreanObjectIsEmpty()) {
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

                for(KoreanObject koreanObject : KoreanManager.activeObjectsMap.values()) {
                    if (!committedStr.isEmpty()) {
                        koreanObject.getTextBuffer().insert(koreanObject.getCursorIndex(), committedStr);
                        koreanObject.setCursorIndex(koreanObject.getCursorIndex() + committedStr.length());
                    }
                    koreanObject.setComposingText(composingStr);
                }

                event.consume();

            }

            @Override
            public void caretPositionChanged(InputMethodEvent event) {}
        });

        jComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KoreanManager.isActiveKoreanObjectIsEmpty()) {
                    return;
                }

                for(KoreanObject koreanObject : KoreanManager.activeObjectsMap.values()) {
                    int keyCode = e.getKeyCode();

                    if (keyCode == KeyEvent.VK_V && (e.isControlDown() || e.isMetaDown())) {
                        koreanObject.pasteClipboardText();
                        e.consume();
                        continue;
                    }

                    if (keyCode == KeyEvent.VK_LEFT) {
                        koreanObject.moveCursorLeft();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_RIGHT) {
                        koreanObject.moveCursorRight();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                        if (koreanObject.getComposingText().length() > 0) {
                            koreanObject.setComposingText("");
                        } else if (koreanObject.getCursorIndex() > 0) {
                            // 커서 좌측 글자 삭제
                            koreanObject.getTextBuffer().deleteCharAt(koreanObject.getCursorIndex() - 1);
                            koreanObject.setCursorIndex(koreanObject.getCursorIndex() - 1);
                        }
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_ENTER) {
                        koreanObject.listener.enter();
                        e.consume();
                    }
                    else if (keyCode == KeyEvent.VK_TAB) {
                        koreanObject.listener.tab();
                        e.consume();

                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (KoreanManager.isActiveKoreanObjectIsEmpty()) {
                    return;
                }

                for(KoreanObject koreanObject : KoreanManager.activeObjectsMap.values()) {

                    char c = e.getKeyChar();

                    if (c != KeyEvent.CHAR_UNDEFINED && c >= 32 && c != 127) {
                        if (koreanObject.getComposingText().length() == 0) {
                            koreanObject.getTextBuffer().insert(koreanObject.getCursorIndex(), c);
                            koreanObject.setCursorIndex(koreanObject.getCursorIndex() + 1);
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
