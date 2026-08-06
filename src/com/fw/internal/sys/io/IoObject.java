package com.fw.internal.sys.io;

import com.fw.internal.utils.InternalUtils;
import com.fw.main.Core;
import com.fw.main.api.io.IoInterface;

import javax.crypto.Cipher;
import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

public class IoObject {
    public final String fileName;
    public final String fullPath;
    private final IoInterface ioInterface;

    public IoObject(String fileName, IoInterface ioInterface) {
        this.fileName = fileName;
        this.ioInterface = ioInterface;
        fullPath = InternalUtils.getProjectFolder() + File.separator + fileName + ".fw";
    }

    public void internalLoad() {
        Properties p = new Properties();
        File file = new File(fullPath);
        if (!file.exists()) {
            ioInterface.initLoad(p);
            return;
        }

        if (Core.get().isUseEncryption()) {
            try {
                String encryptedData = Files.readString(Path.of(fullPath));
                String decryptedData = decrypt(encryptedData);

                if (decryptedData == null) {
                    throw new Exception("Wrong decryption key or corrupted data.");
                }

                try (StringReader reader = new StringReader(decryptedData)) {
                    p.load(reader);
                }
                ioInterface.load(p);
            } catch (IOException | NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "loadTexture fail: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileInputStream in = new FileInputStream(fullPath)) {
                p.load(in);
                ioInterface.load(p);
            } catch (IOException | NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "loadTexture fail: " + e.getMessage());
            }
        }

    }

    public void internalSave() {
        Properties props = new Properties();

        ioInterface.save(props);

        File file = new File(fullPath);
        file.getParentFile().mkdirs();

        if (Core.get().isUseEncryption()) {
            try {
                StringWriter writer = new StringWriter();
                props.store(writer, "Data");

                String encryptedData = encrypt(writer.toString());
                if (encryptedData == null) {
                    throw new IOException("Encryption failed.");
                }

                Files.writeString(Path.of(fullPath), encryptedData);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "save fail: " + e.getMessage());
            }
        } else {
            try (FileOutputStream out = new FileOutputStream(fullPath)) {
                props.store(out, "Data");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "save fail: " + e.getMessage());
            }
        }

    }

    private String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, Core.get().encryptionKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
        } catch (Exception e) { return null; }
    }

    private String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, Core.get().encryptionKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) { return null; }
    }
}
