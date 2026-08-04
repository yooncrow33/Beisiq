package com.fw.internal.sys.io;

import com.fw.internal.utils.Internal;
import com.fw.internal.utils.InternalUtils;
import com.fw.main.api.io.Io;

public final class Load {
    @Internal
    @Deprecated
    public boolean loadStart = false;
    @Internal
    @Deprecated
    public boolean loadEnd = false;
    private int progress = 0;
    private int maxProgress = 0;
    final Io io;
    public Load(Io io) {this.io = io;}

    public boolean isLoadEnd() {
        return loadEnd;
    }

    public boolean isLoadStart() {
        return loadStart;
    }

    public float getProgress() {
        return (float) progress / maxProgress * 100;
    }

    public void load() {
        loadStart = true;
        maxProgress = io.ioObjects.size();
        System.out.println(InternalUtils.Time.getTimeFormate() + " / start load");
        for (IoObject l : io.ioObjects) {
            l.internalLoad();
            progress++;

        }
        loadEnd = true;
        System.out.println(InternalUtils.Time.getTimeFormate() + " / end load");
    }
}
