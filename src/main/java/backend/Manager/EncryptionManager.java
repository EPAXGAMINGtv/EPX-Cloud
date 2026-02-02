package backend.Manager;

import backend.CloudServerMain;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class EncryptionManager {
    private static DBManager dbManager;

    public EncryptionManager() {
        dbManager = CloudServerMain.getDBManager();
    }

    public static void encryptFile(String filepath) {
        String  encryptionKey = dbManager.get_cryptionKey();
        if  (encryptionKey == null) {
            throw new NullPointerException("encryptionKey is null");
        }
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), "AES");
        File inputFile = new File(filepath);
        File encryptedFile = new File(filepath);
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile);
             CipherOutputStream cos = new CipherOutputStream(fos, getCipher(secretKey, Cipher.ENCRYPT_MODE))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }

            System.out.println("data succesfully encrypted: " + encryptedFile.getName());
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public static void decryptFile(String filename) throws Exception {
        String encryptionKey = dbManager.get_cryptionKey();
        if (encryptionKey == null) {
            throw new IllegalStateException("encryptionKey is null");
        }

        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), "AES");
        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("crypted data not found: " + filename);
        }
        File decryptedFile = new File(filename);
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(decryptedFile);
             CipherInputStream cis = new CipherInputStream(fis, getCipher(secretKey, Cipher.DECRYPT_MODE))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("Data succesfuly encrypted: " + decryptedFile.getName());
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public boolean is_file_crypted(String filename) {

        try {
            String encryptionKey = dbManager.get_cryptionKey();
            if (encryptionKey == null) {
                throw new IllegalStateException("no key found for encryption key");
            }

            SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), "AES");

            File inputFile = new File(filename);
            try (FileInputStream fis = new FileInputStream(inputFile);
                 CipherInputStream cis = new CipherInputStream(fis, getCipher(secretKey, Cipher.DECRYPT_MODE))) {
                byte[] buffer = new byte[1024];
                while (cis.read(buffer) != -1) {
                }
            }
            return false;

        } catch (Exception e) {
            return true;
        }
    }

    private static Cipher getCipher(SecretKey key, int cipherMode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(cipherMode, key);
        return cipher;
    }

}
