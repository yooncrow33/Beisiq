/*
 * Copyright (c) 2012, Finn Kuusisto
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal;

import com.fw.main.utils.platform.system.asset.AssetManager;

public class MemSound extends SoundAsset {

    private final AssetManager.AssetType type2 = AssetManager.AssetType.SOUND;

    public AssetManager.AssetType getAssetType() {
        return type2;
    }

    private byte[] left;
    private byte[] right;
    private Mixer mixer;
    private final int ID;

    public MemSound(byte[] left, byte[] right, Mixer mixer, int id) {
        this.left = left;
        this.right = right;
        this.mixer = mixer;
        this.ID = id;
    }

    @Override
    public void play() {
        this.play(1.0);
    }

    @Override
    public void play(double volume) {
        this.play(volume, 0.0);
    }

    @Override
    public void play(double volume, double pan) {
        if (this.mixer == null || this.left == null || this.right == null) {
            return;
        }
        SoundReference ref = new MemSoundReference(this.left, this.right, volume, pan, this.ID);
        this.mixer.registerSoundReference(ref);
    }

    @Override
    public void stop() {
        if (this.mixer != null) {
            this.mixer.unRegisterSoundReference(this.ID);
        }
    }

    @Override
    public void free() {
        if (this.mixer != null) {
            this.mixer.unRegisterSoundReference(this.ID);
            this.mixer = null;
        }
        this.left = null;
        this.right = null;
    }

    private static class MemSoundReference implements SoundReference {

        public final int SOUND_ID;

        private byte[] left;
        private byte[] right;
        private int position;
        private double volume;
        private double pan;

        public MemSoundReference(byte[] left, byte[] right, double volume, double pan, int soundID) {
            this.left = left;
            this.right = right;
            this.volume = (volume >= 0.0) ? volume : 1.0;
            this.pan = (pan >= -1.0 && pan <= 1.0) ? pan : 0.0;
            this.position = 0;
            this.SOUND_ID = soundID;
        }

        @Override
        public int getSoundID() {
            return this.SOUND_ID;
        }

        @Override
        public double getVolume() {
            return this.volume;
        }

        @Override
        public double getPan() {
            return this.pan;
        }

        @Override
        public long bytesAvailable() {
            if (this.left == null) return 0;
            return this.left.length - this.position;
        }

        @Override
        public synchronized void skipBytes(long num) {
            this.position += num;
        }

        @Override
        public void nextTwoBytes(int[] data, boolean bigEndian) {
            if (this.left == null || this.right == null || this.position + 1 >= this.left.length) {
                data[0] = 0;
                data[1] = 0;
                return;
            }
            if (bigEndian) {
                data[0] = ((this.left[this.position] << 8) | (this.left[this.position + 1] & 0xFF));
                data[1] = ((this.right[this.position] << 8) | (this.right[this.position + 1] & 0xFF));
            } else {
                data[0] = ((this.left[this.position + 1] << 8) | (this.left[this.position] & 0xFF));
                data[1] = ((this.right[this.position + 1] << 8) | (this.right[this.position] & 0xFF));
            }
            this.position += 2;
        }

        @Override
        public void dispose() {
            this.position = (this.left != null) ? this.left.length + 1 : 0;
            this.left = null;
            this.right = null;
        }
    }
}