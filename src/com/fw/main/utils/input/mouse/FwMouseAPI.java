package com.fw.main.utils.input.mouse;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class FwMouseAPI {

    private MouseEvent rawEvent;

    public FwMouseAPI() {
    }

    public int getX() { return rawEvent.getX(); }

    public int getY() { return rawEvent.getY(); }

    public void setRawEvent(MouseEvent e) {
        this.rawEvent = e;
    }

    public void clear() {
        this.rawEvent = null;
    }

    /**
     * @return MouseEvent.BUTTON1 (좌), BUTTON2 (휠), BUTTON3 (우), NOBUTTON
     */
    public int getButton() {
        return rawEvent.getButton();
    }

    /**
     * @return 더블 클릭이면 true, 그렇지 않으면 false
     */
    public boolean isDoubleClick() {
        return rawEvent.getClickCount() == 2;
    }

    /**
     * OS별 플랫폼 기준의 팝업 메뉴 트리거인지 여부를 반환합니다.
     */
    public boolean isPopupTrigger() {
        return rawEvent.isPopupTrigger();
    }

    public boolean isControlDown() {
        return rawEvent.isControlDown();
    }

    public boolean isShiftDown() {
        return rawEvent.isShiftDown();
    }

    public boolean isAltDown() {
        return rawEvent.isAltDown();
    }

    public boolean isMetaDown() {
        return rawEvent.isMetaDown();
    }

    public int getModifiersEx() {
        return rawEvent.getModifiersEx();
    }

    /**
     * @return 음수면 위로, 양수면 아래로 스크롤. 휠 이벤트가 아니면 0 반환.
     */
    public int getWheelRotation() {
        if (rawEvent instanceof MouseWheelEvent) {
            return ((MouseWheelEvent) rawEvent).getWheelRotation();
        }
        return 0;
    }

    /**
     * 정밀 스크롤 방향과 양을 정수(int) 형태로 반환합니다.
     * 기존 double 값을 int로 캐스팅하여 반환합니다.
     * @return 음수면 위로, 양수면 아래로 스크롤. 휠 이벤트가 아니면 0 반환.
     */
    public int getPreciseWheelRotation() {
        if (rawEvent instanceof MouseWheelEvent) {
            return (int) ((MouseWheelEvent) rawEvent).getPreciseWheelRotation();
        }
        return 0;
    }

    public void consume() {
        rawEvent.consume();
    }

    public boolean isConsumed() {
        return rawEvent.isConsumed();
    }
}