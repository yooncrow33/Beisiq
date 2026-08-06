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
import com.fw.main.utils.platform.system.asset.Sound;
import sun.misc.Unsafe;

import java.lang.reflect.Field;


/**
 * The MemSound class is an implementation of the Sound interface that stores
 * all audio data in memory for low latency.
 *
 * @author Finn Kuusisto
 */
class MemSound implements Sound {

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
    private final int ID;

    public MemSound(long leftAddress, long rightAddress, int capacity, Mixer mixer, int id) {
        this.leftAddress = leftAddress;
        this.rightAddress = rightAddress;
        this.capacity = capacity;
        this.mixer = mixer;
        this.ID = id;
    }

    /**
     * Plays this MemSound.
     */
    @Override
    public void play() { this.play(1.0); }

    /**
     * Plays this MemSound with a specified volume.
     * @param volume the volume at which to play this MemSound
     */
    @Override
    public void play(double volume) { this.play(volume, 0.0); }

    /**
     * Plays this MemSound with a specified volume and pan.
     * @param volume the volume at which to play this MemSound
     * @param pan the pan value to play this MemSound [-1.0,1.0], values outside
     * the valid range will assume no panning (0.0)
     */
    @Override
    public void play(double volume, double pan) {
        SoundReference ref = new MemSoundReference(this, volume, pan, this.ID);
        if (this.mixer != null) {
            this.mixer.registerSoundReference(ref);
        }
    }

    /**
     * Stops this MemSound from playing.  Note that if this MemSound was played
     * repeatedly in an overlapping fashion, all instances of this MemSound
     * still playing will be stopped.
     */
    @Override
    public void stop() {
        if (this.mixer != null) {
            this.mixer.unRegisterSoundReference(this.ID);
        }
    }

    /**
     * Unloads this MemSound from the system.  Attempts to use this MemSound
     * after unloading will result in error.
     */
    @Override
    public void free() {
        if (this.mixer != null) {
            this.mixer.unRegisterSoundReference(this.ID);
        }

        synchronized (this) {
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

    private static class MemSoundReference implements SoundReference {
        public final int SOUND_ID;
        private final MemSound parent;

        private int position;
        private double volume;
        private double pan;

        public MemSoundReference(MemSound parent, double volume, double pan, int soundID) {
            this.parent = parent;
            this.volume = (volume >= 0.0) ? volume : 1.0;
            this.pan = (pan >= -1.0 && pan <= 1.0) ? pan : 0.0;
            this.position = 0;
            this.SOUND_ID = soundID;
        }

        @Override
        public int getSoundID() { return this.SOUND_ID; }

        @Override
        public double getVolume() { return this.volume; }

        @Override
        public double getPan() { return this.pan; }

        @Override
        public long bytesAvailable() {
            synchronized (this.parent) {
                return this.parent.capacity - this.position;
            }
        }

        @Override
        public void skipBytes(long num) {
            synchronized (this.parent) {
                this.position += num;
            }
        }

        @Override
        public void nextTwoBytes(int[] data, boolean bigEndian) {
            synchronized (this.parent) {
                if (this.parent.leftAddress == 0 || this.position >= this.parent.capacity) {
                    data[0] = 0; data[1] = 0;
                    this.position = this.parent.capacity + 1;
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
            }
        }

        @Override
        public void dispose() {
            synchronized (this.parent) {
                this.position = this.parent.capacity + 1;
            }
        }
    }
}