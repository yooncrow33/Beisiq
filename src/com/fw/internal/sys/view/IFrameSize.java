package com.fw.internal.sys.view;

public interface IFrameSize {
    int getComponentWidth();
    int getComponentHeight();

    default double getDeviceScaleX() {
        return 1.0;
    }
    default double getDeviceScaleY() {
        return 1.0;
    }
}
