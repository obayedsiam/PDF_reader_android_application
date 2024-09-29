package com.example.iqra.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileHashUtils {
    public static String calculateFileHash(File file, String md5) {
        try {
            // Use SHA-256 algorithm for hashing
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            // Read the file and update the digest with the file's bytes
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            fis.close();

            // Get the byte array representing the hash
            byte[] bytes = digest.digest();

            // Convert the byte array to a hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();  // Return the hex string of the SHA-256 hash
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
