package com.fw.main.utils.platform.system.scene;

import com.fw.main.utils.platform.system.asset.Sound;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.SoundAsset;

public class Sfx {
    SoundAsset sound;
    Sfx(SoundAsset sound, Scene scene) { this.sound = sound; scene.soundAssetLives.add(this); }

    SoundAsset getSoundAsset() {
        return sound;
    }
    public Sound get() {
        return sound;
    }
}
