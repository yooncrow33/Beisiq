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

public class MemMusic extends MusicAsset {

    private final AssetManager.AssetType type2 = AssetManager.AssetType.MUSIC;

    public AssetManager.AssetType getAssetType() {
        return type2;
    }

    private byte[] left;
    private byte[] right;
    private Mixer mixer;
    private MusicReference reference;

    public MemMusic(byte[] left, byte[] right, Mixer mixer) {
        this.left = left;
        this.right = right;
        this.mixer = mixer;
        this.reference = new MemMusicReference(this.left, this.right, false, false, 0, 0, 1.0, 0.0);
        this.mixer.registerMusicReference(this.reference);
    }

    @Override
    public void play(boolean loop) {
        this.reference.setLoop(loop);
        this.reference.setPlaying(true);
    }

    @Override
    public void play(boolean loop, double volume) {
        this.setLoop(loop);
        this.setVolume(volume);
        this.reference.setPlaying(true);
    }

    @Override
    public void play(boolean loop, double volume, double pan) {
        this.setLoop(loop);
        this.setVolume(volume);
        this.setPan(pan);
        this.reference.setPlaying(true);
    }

    @Override
    public void stop() {
        this.reference.setPlaying(false);
        this.rewind();
    }

    @Override
    public void pause() {
        this.reference.setPlaying(false);
    }

    @Override
    public void resume() {
        this.reference.setPlaying(true);
    }

    @Override
    public void rewind() {
        this.reference.setPosition(0);
    }

    @Override
    public void rewindToLoopPosition() {
        long byteIndex = this.reference.getLoopPosition();
        this.reference.setPosition(byteIndex);
    }

    @Override
    public boolean playing() {
        return this.reference.getPlaying();
    }

    @Override
    public boolean done() {
        return this.reference.done();
    }

    @Override
    public boolean loop() {
        return this.reference.getLoop();
    }

    @Override
    public void setLoop(boolean loop) {
        this.reference.setLoop(loop);
    }

    @Override
    public int getLoopPositionByFrame() {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() / InternalSoundModule.FORMAT.getChannels();
        long byteIndex = this.reference.getLoopPosition();
        return (int)(byteIndex / bytesPerChannelForFrame);
    }

    @Override
    public double getLoopPositionBySeconds() {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() / InternalSoundModule.FORMAT.getChannels();
        long byteIndex = this.reference.getLoopPosition();
        return (byteIndex / (InternalSoundModule.FORMAT.getFrameRate() * bytesPerChannelForFrame));
    }

    @Override
    public void setLoopPositionByFrame(int frameIndex) {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() / InternalSoundModule.FORMAT.getChannels();
        long byteIndex = (long)(frameIndex * bytesPerChannelForFrame);
        this.reference.setLoopPosition(byteIndex);
    }

    @Override
    public void setLoopPositionBySeconds(double seconds) {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() / InternalSoundModule.FORMAT.getChannels();
        long byteIndex = (long)(seconds * InternalSoundModule.FORMAT.getFrameRate()) * bytesPerChannelForFrame;
        this.reference.setLoopPosition(byteIndex);
    }

    @Override
    public double getVolume() {
        return this.reference.getVolume();
    }

    @Override
    public void setVolume(double volume) {
        if (volume >= 0.0) {
            this.reference.setVolume(volume);
        }
    }

    @Override
    public double getPan() {
        return this.reference.getPan();
    }

    @Override
    public void setPan(double pan) {
        if (pan >= -1.0 && pan <= 1.0) {
            this.reference.setPan(pan);
        }
    }

    @Override
    public void free() {
        if (this.mixer != null) {
            this.mixer.unRegisterMusicReference(this.reference);
            this.mixer = null;
        }
        if (this.reference != null) {
            this.reference.dispose();
            this.reference = null;
        }
        this.left = null;
        this.right = null;
    }

    private static class MemMusicReference implements MusicReference {

        private byte[] left;
        private byte[] right;
        private boolean playing;
        private boolean loop;
        private int loopPosition;
        private int position;
        private double volume;
        private double pan;

        public MemMusicReference(byte[] left, byte[] right, boolean playing,
                                 boolean loop, int loopPosition, int position, double volume,
                                 double pan) {
            this.left = left;
            this.right = right;
            this.playing = playing;
            this.loop = loop;
            this.loopPosition = loopPosition;
            this.position = position;
            this.volume = volume;
            this.pan = pan;
        }

        @Override
        public synchronized boolean getPlaying() {
            return this.playing;
        }

        @Override
        public synchronized boolean getLoop() {
            return this.loop;
        }

        @Override
        public synchronized long getPosition() {
            return this.position;
        }

        @Override
        public synchronized long getLoopPosition() {
            return this.loopPosition;
        }

        @Override
        public synchronized double getVolume() {
            return this.volume;
        }

        @Override
        public synchronized double getPan() {
            return this.pan;
        }

        @Override
        public synchronized void setPlaying(boolean playing) {
            this.playing = playing;
        }

        @Override
        public synchronized void setLoop(boolean loop) {
            this.loop = loop;
        }

        @Override
        public synchronized void setPosition(long position) {
            if (this.left != null && position >= 0 && position < this.left.length) {
                this.position = (int)position;
            }
        }

        @Override
        public synchronized void setLoopPosition(long loopPosition) {
            if (this.left != null && loopPosition >= 0 && loopPosition < this.left.length) {
                this.loopPosition = (int)loopPosition;
            }
        }

        @Override
        public synchronized void setVolume(double volume) {
            this.volume = volume;
        }

        @Override
        public synchronized void setPan(double pan) {
            this.pan = pan;
        }

        @Override
        public synchronized long bytesAvailable() {
            if (this.left == null) return 0;
            return this.left.length - this.position;
        }

        @Override
        public synchronized boolean done() {
            if (this.left == null) return true;
            long available = this.left.length - this.position;
            return available <= 0 && !this.playing;
        }

        @Override
        public synchronized void skipBytes(long num) {
            if (this.left == null) return;
            for (int i = 0; i < num; i++) {
                this.position++;
                if (this.position >= this.left.length) {
                    if (this.loop) {
                        this.position = this.loopPosition;
                    } else {
                        this.playing = false;
                    }
                }
            }
        }

        @Override
        public synchronized void nextTwoBytes(int[] data, boolean bigEndian) {
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
            if (this.position >= this.left.length) {
                if (this.loop) {
                    this.position = this.loopPosition;
                } else {
                    this.playing = false;
                }
            }
        }

        @Override
        public synchronized void dispose() {
            this.playing = false;
            this.position = (this.left != null) ? this.left.length + 1 : 0;
            this.left = null;
            this.right = null;
        }
    }
}