package com.fw.main.utils.platform.system.scene;

import com.fw.main.utils.platform.system.asset.Music;
import com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal.MusicAsset;

public class Bgm {
    MusicAsset music;
    Bgm(MusicAsset musicAsset, Scene scene) { this.music = musicAsset; scene.musicAssetLives.add(this); }

    MusicAsset getMusicAsset() {
        return music;
    }

    public Music getMusic() {
        return music;
    }
}
