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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.fw.internal.utils.Internal;

@Internal
public class InternalSoundModule {
    public static final String VERSION = "based TinySound 1.1.1";

    public static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,
            16,
            2,
            4,
            44100,
            false
    );

    public static Mixer mixer;
    private static SourceDataLine outLine;
    private static boolean inited = false;
    private static UpdateRunner autoUpdater;
    private static int soundCount = 0;

    public static void init() {
        if (InternalSoundModule.inited) {
            return;
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, InternalSoundModule.FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Unsupported output format!");
            return;
        }
        InternalSoundModule.outLine = InternalSoundModule.tryGetLine();
        if (InternalSoundModule.outLine == null) {
            System.err.println("Output line unavailable!");
            return;
        }
        InternalSoundModule.outLine.start();
        InternalSoundModule.finishInit();
    }

    public static void init(javax.sound.sampled.Mixer.Info info)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        if (InternalSoundModule.inited) {
            return;
        }
        javax.sound.sampled.Mixer mixer = AudioSystem.getMixer(info);
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, InternalSoundModule.FORMAT);
        InternalSoundModule.outLine = (SourceDataLine)mixer.getLine(lineInfo);
        InternalSoundModule.outLine.open(InternalSoundModule.FORMAT);
        InternalSoundModule.outLine.start();
        InternalSoundModule.finishInit();
    }

    private static void finishInit() {
        InternalSoundModule.mixer = new Mixer();
        InternalSoundModule.autoUpdater = new UpdateRunner(InternalSoundModule.mixer, InternalSoundModule.outLine);
        Thread updateThread = new Thread(InternalSoundModule.autoUpdater);
        try {
            updateThread.setDaemon(true);
            updateThread.setPriority(Thread.MAX_PRIORITY);
        } catch (Exception e) {}
        InternalSoundModule.inited = true;
        updateThread.start();
        Thread.yield();
    }

    public static void shutdown() {
        if (!InternalSoundModule.inited) {
            return;
        }
        InternalSoundModule.inited = false;
        InternalSoundModule.autoUpdater.stop();
        InternalSoundModule.autoUpdater = null;
        InternalSoundModule.outLine.stop();
        InternalSoundModule.outLine.flush();
        InternalSoundModule.mixer.clearMusic();
        InternalSoundModule.mixer.clearSounds();
        InternalSoundModule.mixer = null;
    }

    public static boolean isInitialized() {
        return InternalSoundModule.inited;
    }

    public static double getGlobalVolume() {
        if (!InternalSoundModule.inited) {
            return -1.0;
        }
        return InternalSoundModule.mixer.getVolume();
    }

    public static void setGlobalVolume(double volume) {
        if (!InternalSoundModule.inited) {
            return;
        }
        InternalSoundModule.mixer.setVolume(volume);
    }

    public static MusicAsset loadMusic(InputStream is, boolean streamFromFile) {
        if (!InternalSoundModule.inited || is == null) {
            System.err.println("TinySound not initialized or InputStream is null!");
            return null;
        }
        AudioInputStream audioStream = InternalSoundModule.getValidAudioStream(is);
        if (audioStream == null) return null;

        byte[][] data = InternalSoundModule.readAllBytes(audioStream);
        if (data == null) {
            return null;
        }

        if (streamFromFile) {
            StreamInfo info = InternalSoundModule.createFileStream(data);
            if (info == null) {
                return null;
            }
            try {
                return new StreamMusic(info.URL, info.NUM_BYTES_PER_CHANNEL, InternalSoundModule.mixer);
            } catch (IOException e) {
                System.err.println("Failed to create StreamMusic: " + e.getMessage());
                return null;
            }
        }

        return new MemMusic(data[0], data[1], InternalSoundModule.mixer);
    }

    public static SoundAsset loadSound(InputStream is) {
        if (!InternalSoundModule.inited || is == null) {
            System.err.println("TinySound not initialized or InputStream is null!");
            return null;
        }
        AudioInputStream audioStream = InternalSoundModule.getValidAudioStream(is);
        if (audioStream == null) return null;

        byte[][] data = InternalSoundModule.readAllBytes(audioStream);
        if (data == null) {
            return null;
        }

        int soundId = InternalSoundModule.soundCount++;
        return new MemSound(data[0], data[1], InternalSoundModule.mixer, soundId);
    }

    private static byte[][] readAllBytes(AudioInputStream stream) {
        byte[][] data = null;
        int numChannels = stream.getFormat().getChannels();
        if (numChannels == 1) {
            byte[] left = InternalSoundModule.readAllBytesOneChannel(stream);
            if (left == null) {
                return null;
            }
            data = new byte[2][];
            data[0] = left;
            data[1] = left;
        } else if (numChannels == 2) {
            data = InternalSoundModule.readAllBytesTwoChannel(stream);
        } else {
            System.err.println("Unable to read " + numChannels + " channels!");
        }
        return data;
    }

    private static byte[] readAllBytesOneChannel(AudioInputStream stream) {
        byte[] data = null;
        try {
            data = InternalSoundModule.getBytes(stream);
        } catch (IOException e) {
            System.err.println("Error reading all bytes from stream!");
            return null;
        } finally {
            try { stream.close(); } catch (IOException e) {}
        }
        return data;
    }

    private static byte[][] readAllBytesTwoChannel(AudioInputStream stream) {
        byte[][] data = null;
        try {
            byte[] allBytes = InternalSoundModule.getBytes(stream);
            byte[] left = new byte[allBytes.length / 2];
            byte[] right = new byte[allBytes.length / 2];
            for (int i = 0, j = 0; i < allBytes.length; i += 4, j += 2) {
                left[j] = allBytes[i];
                left[j + 1] = allBytes[i + 1];
                right[j] = allBytes[i + 2];
                right[j + 1] = allBytes[i + 3];
            }
            data = new byte[2][];
            data[0] = left;
            data[1] = right;
        } catch (IOException e) {
            System.err.println("Error reading all bytes from stream!");
            return null;
        } finally {
            try { stream.close(); } catch (IOException e) {}
        }
        return data;
    }

    private static AudioInputStream getValidAudioStream(InputStream is) {
        AudioInputStream audioStream = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            audioStream = AudioSystem.getAudioInputStream(bis);
            AudioFormat streamFormat = audioStream.getFormat();
            AudioFormat mono16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
            AudioFormat mono8 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 8, 1, 1, 44100, false);
            AudioFormat stereo8 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 8, 2, 2, 44100, false);

            if (streamFormat.matches(InternalSoundModule.FORMAT) || streamFormat.matches(mono16)) {
                return audioStream;
            } else if (AudioSystem.isConversionSupported(InternalSoundModule.FORMAT, streamFormat)) {
                audioStream = AudioSystem.getAudioInputStream(InternalSoundModule.FORMAT, audioStream);
            } else if (AudioSystem.isConversionSupported(mono16, streamFormat)) {
                audioStream = AudioSystem.getAudioInputStream(mono16, audioStream);
            } else if (streamFormat.matches(stereo8) || AudioSystem.isConversionSupported(stereo8, streamFormat)) {
                if (!streamFormat.matches(stereo8)) {
                    audioStream = AudioSystem.getAudioInputStream(stereo8, audioStream);
                }
                audioStream = InternalSoundModule.convertStereo8Bit(audioStream);
            } else if (streamFormat.matches(mono8) || AudioSystem.isConversionSupported(mono8, streamFormat)) {
                if (!streamFormat.matches(mono8)) {
                    audioStream = AudioSystem.getAudioInputStream(mono8, audioStream);
                }
                audioStream = InternalSoundModule.convertMono8Bit(audioStream);
            } else {
                System.err.println("Unable to convert audio resource format: " + streamFormat);
                audioStream.close();
                return null;
            }

            if (audioStream.getFrameLength() > Integer.MAX_VALUE) {
                System.err.println("Audio resource too long!");
                return null;
            }
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Unsupported audio resource format: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Error reading audio resource stream: " + e.getMessage());
            return null;
        }
        return audioStream;
    }

    private static AudioInputStream convertMono8Bit(AudioInputStream stream) {
        byte[] newData = null;
        try {
            byte[] data = InternalSoundModule.getBytes(stream);
            int newNumBytes = data.length * 2;
            if (newNumBytes < 0) {
                System.err.println("Audio resource too long!");
                return null;
            }
            newData = new byte[newNumBytes];
            for (int i = 0, j = 0; i < data.length; i++, j += 2) {
                double floatVal = (double) data[i];
                floatVal /= (floatVal < 0) ? 128 : 127;
                if (floatVal < -1.0) floatVal = -1.0;
                else if (floatVal > 1.0) floatVal = 1.0;
                int val = (int) (floatVal * Short.MAX_VALUE);
                newData[j + 1] = (byte) ((val >> 8) & 0xFF);
                newData[j] = (byte) (val & 0xFF);
            }
        } catch (IOException e) {
            System.err.println("Error reading all bytes from stream!");
            return null;
        } finally {
            try { stream.close(); } catch (IOException e) {}
        }
        AudioFormat mono16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
        return new AudioInputStream(new ByteArrayInputStream(newData), mono16, newData.length / 2);
    }

    private static AudioInputStream convertStereo8Bit(AudioInputStream stream) {
        byte[] newData = null;
        try {
            byte[] data = InternalSoundModule.getBytes(stream);
            int newNumBytes = data.length * 2 * 2;
            if (newNumBytes < 0) {
                System.err.println("Audio resource too long!");
                return null;
            }
            newData = new byte[newNumBytes];
            for (int i = 0, j = 0; i < data.length; i += 2, j += 4) {
                double leftFloatVal = (double) data[i];
                double rightFloatVal = (double) data[i + 1];
                leftFloatVal /= (leftFloatVal < 0) ? 128 : 127;
                rightFloatVal /= (rightFloatVal < 0) ? 128 : 127;
                if (leftFloatVal < -1.0) leftFloatVal = -1.0;
                else if (leftFloatVal > 1.0) leftFloatVal = 1.0;
                if (rightFloatVal < -1.0) rightFloatVal = -1.0;
                else if (rightFloatVal > 1.0) rightFloatVal = 1.0;

                int leftVal = (int) (leftFloatVal * Short.MAX_VALUE);
                int rightVal = (int) (rightFloatVal * Short.MAX_VALUE);
                newData[j + 1] = (byte) ((leftVal >> 8) & 0xFF);
                newData[j] = (byte) (leftVal & 0xFF);
                newData[j + 3] = (byte) ((rightVal >> 8) & 0xFF);
                newData[j + 2] = (byte) (rightVal & 0xFF);
            }
        } catch (IOException e) {
            System.err.println("Error reading all bytes from stream!");
            return null;
        } finally {
            try { stream.close(); } catch (IOException e) {}
        }
        AudioFormat stereo16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
        return new AudioInputStream(new ByteArrayInputStream(newData), stereo16, newData.length / 4);
    }

    private static byte[] getBytes(AudioInputStream stream) throws IOException {
        int bufSize = (int) InternalSoundModule.FORMAT.getSampleRate() *
                InternalSoundModule.FORMAT.getChannels() *
                InternalSoundModule.FORMAT.getFrameSize();
        byte[] buf = new byte[bufSize];
        ByteList list = new ByteList(bufSize);
        int numRead = 0;
        while ((numRead = stream.read(buf)) > -1) {
            for (int i = 0; i < numRead; i++) {
                list.add(buf[i]);
            }
        }
        return list.asArray();
    }

    private static StreamInfo createFileStream(byte[][] data) {
        File temp = null;
        try {
            temp = File.createTempFile("tiny", "sound");
            temp.deleteOnExit();
        } catch (IOException e) {
            System.err.println("Failed to create file for streaming!");
            return null;
        }
        URL url = null;
        try {
            url = temp.toURI().toURL();
        } catch (MalformedURLException e1) {
            System.err.println("Failed to get URL for stream file!");
            return null;
        }
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(temp), (512 * 1024));
        } catch (FileNotFoundException e) {
            System.err.println("Failed to open stream file for writing!");
            return null;
        }
        try {
            for (int i = 0; i < data[0].length; i += 2) {
                try {
                    out.write(data[0], i, 2);
                    out.write(data[1], i, 2);
                } catch (IOException e) {
                    System.err.println("Failed writing bytes to stream file!");
                    return null;
                }
            }
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println("Failed closing stream file after writing!");
            }
        }
        return new StreamInfo(url, data[0].length);
    }

    private static SourceDataLine tryGetLine() {
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, InternalSoundModule.FORMAT);
        javax.sound.sampled.Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfos.length; i++) {
            javax.sound.sampled.Mixer mixer = null;
            try {
                mixer = AudioSystem.getMixer(mixerInfos[i]);
            } catch (SecurityException | IllegalArgumentException e) {
                continue;
            }
            if (mixer == null || !mixer.isLineSupported(lineInfo)) {
                continue;
            }
            SourceDataLine line = null;
            try {
                line = (SourceDataLine) mixer.getLine(lineInfo);
                if (!line.isOpen()) {
                    line.open(InternalSoundModule.FORMAT);
                }
            } catch (LineUnavailableException | SecurityException e) {
                continue;
            }
            if (line != null && line.isOpen()) {
                return line;
            }
        }
        return null;
    }
}