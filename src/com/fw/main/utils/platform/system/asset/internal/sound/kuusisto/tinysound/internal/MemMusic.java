/*
 * Copyright (c) 2012, Finn Kuusisto
 * Copyright (c) 2026, yooncrow33
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
import com.fw.main.utils.platform.system.asset.Music;
import sun.misc.Unsafe;

import java.lang.reflect.Field;


/**
 * The MemMusic class is an implementation of the Music interface that stores
 * all audio data in memory for low latency.
 */

class MemMusic extends MusicAsset {

    static final Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unsafe init fail", e);
        }
    }

    AssetManager.AssetType type2 = AssetManager.AssetType.SOUND;
    public AssetManager.AssetType getAssetType() {return type2;}

    long leftAddress;
    long rightAddress;
    int capacity;
    private Mixer mixer;
    private MusicReference reference;


    public MemMusic(long leftAddress, long rightAddress, int capacity, Mixer mixer) {
        this.leftAddress = leftAddress;
        this.rightAddress = rightAddress;
        this.capacity = capacity;
        this.mixer = mixer;
        this.reference = new MemMusicReference(this, false, false, 0, 0, 1.0, 0.0);
        this.mixer.registerMusicReference(this.reference);
    }

    /**
     * Play this MemMusic and loop if specified.
     * @param loop if this MemMusic should loop
     */
    @Override
    public void play(boolean loop) {
        this.reference.setLoop(loop);
        this.reference.setPlaying(true);
    }

    /**
     * Play this MemMusic at the specified volume and loop if specified.
     * @param loop if this MemMusic should loop
     * @param volume the volume to play the this Music
     */
    @Override
    public void play(boolean loop, double volume) {
        this.setLoop(loop);
        this.setVolume(volume);
        this.reference.setPlaying(true);
    }

    /**
     * Play this MemMusic at the specified volume and pan, and loop if specified
     * .
     * @param loop if this MemMusic should loop
     * @param volume the volume to play the this MemMusic
     * @param pan the pan at which to play this MemMusic [-1.0,1.0], values
     * outside the valid range will be ignored
     */
    @Override
    public void play(boolean loop, double volume, double pan) {
        this.setLoop(loop);
        this.setVolume(volume);
        this.setPan(pan);
        this.reference.setPlaying(true);
    }

    /**
     * Stop playing this MemMusic and set its position to the beginning.
     */
    @Override
    public void stop() {
        this.reference.setPlaying(false);
        this.rewind();
    }

    /**
     * Stop playing this MemMusic and keep its current position.
     */
    @Override
    public void pause() {
        this.reference.setPlaying(false);
    }

    /**
     * Play this MemMusic from its current position.
     */
    @Override
    public void resume() {
        this.reference.setPlaying(true);
    }

    /**
     * Set this MemMusic's position to the beginning.
     */
    @Override
    public void rewind() {
        this.reference.setPosition(0);
    }

    /**
     * Set this MemMusic's position to the loop position.
     */
    @Override
    public void rewindToLoopPosition() {
        long byteIndex = this.reference.getLoopPosition();
        this.reference.setPosition(byteIndex);
    }

    /**
     * Determine if this MemMusic is playing.
     * @return true if this MemMusic is playing
     */
    @Override
    public boolean playing() {
        return this.reference.getPlaying();
    }

    /**
     * Determine if this MemMusic has reached its end and is done playing.
     * @return true if this MemMusic has reached the end and is done playing
     */
    @Override
    public boolean done() {
        return this.reference.done();
    }

    /**
     * Determine if this MemMusic will loop.
     * @return true if this MemMusic will loop
     */
    @Override
    public boolean loop() {
        return this.reference.getLoop();
    }

    /**
     * Set whether this MemMusic will loop.
     * @param loop whether this MemMusic will loop
     */
    @Override
    public void setLoop(boolean loop) {
        this.reference.setLoop(loop);
    }

    /**
     * Get the loop position of this MemMusic by sample frame.
     * @return loop position by sample frame
     */
    @Override
    public int getLoopPositionByFrame() {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() /
                InternalSoundModule.FORMAT.getChannels();
        long byteIndex = this.reference.getLoopPosition();
        return (int)(byteIndex / bytesPerChannelForFrame);
    }

    /**
     * Get the loop position of this MemMusic by seconds.
     * @return loop position by seconds
     */
    @Override
    public double getLoopPositionBySeconds() {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() /
                InternalSoundModule.FORMAT.getChannels();
        long byteIndex = this.reference.getLoopPosition();
        return (byteIndex / (InternalSoundModule.FORMAT.getFrameRate() *
                bytesPerChannelForFrame));
    }

    /**
     * Set the loop position of this MemMusic by sample frame.
     * @param frameIndex sample frame loop position to set
     */
    @Override
    public void setLoopPositionByFrame(int frameIndex) {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() /
                InternalSoundModule.FORMAT.getChannels();
        long byteIndex = (long)(frameIndex * bytesPerChannelForFrame);
        this.reference.setLoopPosition(byteIndex);
    }

    /**
     * Set the loop position of this MemMusic by seconds.
     * @param seconds loop position to set by seconds
     */
    @Override
    public void setLoopPositionBySeconds(double seconds) {
        int bytesPerChannelForFrame = InternalSoundModule.FORMAT.getFrameSize() /
                InternalSoundModule.FORMAT.getChannels();
        long byteIndex = (long)(seconds * InternalSoundModule.FORMAT.getFrameRate()) *
                bytesPerChannelForFrame;
        this.reference.setLoopPosition(byteIndex);
    }

    /**
     * Get the volume of this MemMusic.
     * @return volume of this MemMusic
     */
    @Override
    public double getVolume() {
        return this.reference.getVolume();
    }

    /**
     * Set the volume of this MemMusic.
     * @param volume the desired volume of this MemMusic
     */
    @Override
    public void setVolume(double volume) {
        if (volume >= 0.0) {
            this.reference.setVolume(volume);
        }
    }

    /**
     * Get the pan of this MemMusic.
     * @return pan of this MemMusic
     */
    @Override
    public double getPan() {
        return this.reference.getPan();
    }

    /**
     * Set the pan of this MemMusic.  Must be between -1.0 (full pan left) and
     * 1.0 (full pan right).  Values outside the valid range will be ignored.
     * @param pan the desired pan of this MemMusic
     */
    @Override
    public void setPan(double pan) {
        if (pan >= -1.0 && pan <= 1.0) {
            this.reference.setPan(pan);
        }
    }

    /**
     * Unload this MemMusic from the system.  Attempts to use this MemMusic
     * after unloading will result in error.
     */
    @Override
    public void free() {
        if (this.mixer != null) {
            this.mixer.unRegisterMusicReference(this.reference);
        }

        synchronized (this) {
            this.reference.dispose();

            if (this.leftAddress != 0) {
                unsafe.freeMemory(this.leftAddress);
                this.leftAddress = 0;
            }
            if (this.rightAddress != 0) {
                unsafe.freeMemory(this.rightAddress);
                this.rightAddress = 0;
            }
            this.capacity = 0;
        }

        this.mixer = null;
    }

    /////////////
    //Reference//
    /////////////

    private static class MemMusicReference implements MusicReference {

        private final MemMusic parent; // 부모 참조 유지
        private boolean playing;
        private boolean loop;
        private int loopPosition;
        private int position;
        private double volume;
        private double pan;

        public MemMusicReference(MemMusic parent, boolean playing,
                                 boolean loop, int loopPosition, int position, double volume, double pan) {
            this.parent = parent;
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
            if (position >= 0 && position < parent.capacity) {
                this.position = (int)position;
            }
        }

        @Override
        public synchronized void setLoopPosition(long loopPosition) {
            if (loopPosition >= 0 && loopPosition < parent.capacity) {
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
        public synchronized void skipBytes(long num) {
            for (int i = 0; i < num; i++) {
                this.position++;
                if (this.position >= parent.capacity) {
                    if (this.loop) {
                        this.position = this.loopPosition;
                    }
                    else {
                        this.playing = false;
                    }
                }
            }
        }

        @Override
        public synchronized long bytesAvailable() {
            synchronized (this.parent) {
                return this.parent.capacity - this.position;
            }
        }

        @Override
        public synchronized boolean done() {
            synchronized (this.parent) {
                return (this.parent.capacity - this.position) <= 0 && !this.playing;
            }
        }

        @Override
        public void nextTwoBytes(int[] data, boolean bigEndian) {
            synchronized (this.parent) {
                if (this.parent.leftAddress == 0 || this.position >= this.parent.capacity) {
                    data[0] = 0; data[1] = 0;
                    return;
                }

                byte l1 = unsafe.getByte(this.parent.leftAddress + this.position);
                byte l2 = unsafe.getByte(this.parent.leftAddress + this.position + 1);
                byte r1 = unsafe.getByte(this.parent.rightAddress + this.position);
                byte r2 = unsafe.getByte(this.parent.rightAddress + this.position + 1);

                if (bigEndian) {
                    data[0] = ((l1 << 8) | (l2 & 0xFF));
                    data[1] = ((r1 << 8) | (r2 & 0xFF));
                } else {
                    data[0] = ((l2 << 8) | (l1 & 0xFF));
                    data[1] = ((r2 << 8) | (r1 & 0xFF));
                }
                this.position += 2;

                if (this.position >= this.parent.capacity) {
                    if (this.loop) {
                        this.position = this.loopPosition;
                    } else {
                        this.playing = false;
                    }
                }
            }
        }

        @Override
        public synchronized void dispose() {
            synchronized (this.parent) {
                this.playing = false;
                this.position = this.parent.capacity + 1;
            }
        }

    }
}