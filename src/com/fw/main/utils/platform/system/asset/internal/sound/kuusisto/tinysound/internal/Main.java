package com.fw.main.utils.platform.system.asset.internal.sound.kuusisto.tinysound.internal;

import sun.misc.Unsafe;

import javax.sound.sampled.AudioInputStream;
import java.lang.reflect.Field;

class Main extends InternalSoundModule {

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

    public static MemMusic loadMemMusicDirectly(AudioInputStream stream) throws Exception {
        return (MemMusic) loadOffHeapAudio(stream, -1, true);
    }

    public static MemSound loadMemSoundDirectly(AudioInputStream stream, int soundId) throws Exception {
        return (MemSound) loadOffHeapAudio(stream, soundId, false);
    }

    private static Object loadOffHeapAudio(AudioInputStream stream, int soundId, boolean isMusic) throws Exception {
        long frameLength = stream.getFrameLength();

        if (frameLength <= 0 || frameLength > (Integer.MAX_VALUE / 2)) {
            stream.close();
            throw new RuntimeException("Stream size is invalid or too large for off-heap allocation.");
        }

        // 16-bit stereo = 1 프레임당 (Left 2bytes + Right 2bytes) = 총 4bytes
        // 채널당 필요한 바이트 수 = frameLength * 2
        int bytesPerChannel = (int) frameLength * 2;
        long leftAddr = 0;
        long rightAddr = 0;

        try {
            leftAddr = unsafe.allocateMemory(bytesPerChannel);
            rightAddr = unsafe.allocateMemory(bytesPerChannel);

            byte[] chunk = new byte[4096];
            int numRead;
            int channelOffset = 0; // 채널별 바이트 오프셋

            while ((numRead = stream.read(chunk)) > -1) {
                // Interleaved Stereo (L1_low, L1_high, R1_low, R1_high, ...) 처리
                int limit = numRead - (numRead % 4); // 4바이트(1 프레임) 단위로 정렬

                for (int i = 0; i < limit; i += 4) {
                    if (channelOffset >= bytesPerChannel) {
                        break;
                    }
                    // Left Channel (2 Bytes)
                    unsafe.putByte(leftAddr + channelOffset, chunk[i]);
                    unsafe.putByte(leftAddr + channelOffset + 1, chunk[i + 1]);

                    // Right Channel (2 Bytes)
                    unsafe.putByte(rightAddr + channelOffset, chunk[i + 2]);
                    unsafe.putByte(rightAddr + channelOffset + 1, chunk[i + 3]);

                    channelOffset += 2; // 채널당 2바이트씩 누적
                }

                if (channelOffset >= bytesPerChannel) {
                    break;
                }
            }

            if (isMusic) {
                return new MemMusic(leftAddr, rightAddr, bytesPerChannel, InternalSoundModule.mixer);
            } else {
                return new MemSound(leftAddr, rightAddr, bytesPerChannel, InternalSoundModule.mixer, soundId);
            }

        } catch (Throwable t) {
            if (leftAddr != 0) unsafe.freeMemory(leftAddr);
            if (rightAddr != 0) unsafe.freeMemory(rightAddr);

            if (t instanceof Exception) throw (Exception) t;
            throw new RuntimeException("Failed to allocate or read off-heap memory", t);
        } finally {
            stream.close();
        }
    }
}