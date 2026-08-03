package com.fw.main;

import javax.crypto.spec.SecretKeySpec;

public class Config {
    final String projectName;
    int initWindowWidth;
    int initWindowHeight;
    boolean useKoreanModule;
    boolean useEncryption;
    boolean useIntegerPhysicalScaling;
    String secretKey = null;
    public String getProjectName() { return projectName; }
    public int getInitWindowWidth() { return initWindowWidth; }
    public int getInitWindowHeight() { return initWindowHeight; }
    public boolean isUseKoreanModule() {return useKoreanModule; }
    public boolean isUseEncryption() {return useEncryption; }
    public boolean isUseIntegerPhysicalScaling() { return useIntegerPhysicalScaling; }
    public final SecretKeySpec encryptionKey;

    private Config(Config.Builder builder) {
        this.projectName = builder.projectName;
        this.initWindowWidth = builder.initWindowWidth;
        this.initWindowHeight = builder.initWindowHeight;
        this.useKoreanModule = builder.useKoreanModule;
        this.useEncryption = builder.useEncryption;
        this.useIntegerPhysicalScaling = builder.useIntegerPhysicalScaling;
        this.secretKey = builder.secretKey;
        if (secretKey.length() != 16) {
            System.err.println("Encryption Key is not 16-digit!");
            System.exit(0);
            encryptionKey = new SecretKeySpec(this.secretKey.getBytes(), "AES"); //어쨌든 상수는 대입을 시켜야함.
        } else { encryptionKey = new SecretKeySpec(secretKey.getBytes(), "AES"); }
    }

    public static class Builder {
        final String projectName;
        int initWindowWidth = 1200;
        int initWindowHeight = 300;
        boolean useKoreanModule = false;
        boolean useEncryption;
        boolean useIntegerPhysicalScaling;
        String secretKey = null;
        public Builder(String projectName) {
            this.projectName = projectName;
        }

        public Builder setWindowWidth(int size) {
            initWindowWidth = size;
            return this;
        }
        public Builder setWindowHeight(int size) {
            initWindowHeight = size;
            return this;
        }
        public Builder setUseKoreanModule(boolean bool) {
            useKoreanModule = bool;
            return this;
        }
        public Builder setUseEncryption(boolean bool) {
            useEncryption = bool;
            return this;
        }
        /**
         * Snap the final device scale of Java2D down to an integer at the available window size.
         * Performance is stable, but the resize result may introduce letterboxing and stepwise size changes.
         */
        public Builder setUseIntegerPhysicalScaling(boolean bool) {
            useIntegerPhysicalScaling = bool;
            return this;
        }
        /**
         * a 16-digit character
         */
        public Builder setEncryptionKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }
        public Config build() {
            return new Config(this);
        }
    }
}
