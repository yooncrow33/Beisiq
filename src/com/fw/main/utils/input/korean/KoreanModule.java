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
                        koreanObject.getTextBuffer().append(committedStr);
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
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (koreanObject.getComposingText().length() > 0) {
                            koreanObject.setComposingText("");
                        } else if (koreanObject.getTextBuffer().length() > 0) {
                            koreanObject.getTextBuffer().deleteCharAt(koreanObject.getTextBuffer().length() - 1);
                        }
                    }

                    else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        koreanObject.listener.enter();
                        e.consume();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_TAB) {
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
                            koreanObject.getTextBuffer().append(c);
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
