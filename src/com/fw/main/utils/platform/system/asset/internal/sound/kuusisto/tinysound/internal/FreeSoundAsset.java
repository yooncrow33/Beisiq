package com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal;

interface FreeSoundAsset {
    /**
     * Unloads this Sound from the system.  Attempts to use this Sound after
     * unloading will result in error.
     */
    void free();
}
