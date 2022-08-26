package com.example.accountvault;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {
    private final String ALGORITHM = "AES";
    private final String PROVIDER = "AndroidKeyStore";
    private final String TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private final byte[] IV = Arrays.copyOfRange(toBase64Bytes(PROVIDER), 0, 16);
    private SecretKey secretKey;

    public Cryptography(SecretKey secretKey){
        this.secretKey = secretKey;
    }

    public String hash(String plainText) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(toBase64Bytes(plainText));
        return toBase64String(hashedBytes);
    }

    public String encrypt(String plainText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
        byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherTextBytes = cipher.doFinal(plainTextBytes);
        return sanitize(toBase64String(cipherTextBytes));
    }

    public String decrypt(String cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        cipherText = sanitize(cipherText);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
        byte[] cipherTextBytes = toBase64Bytes(cipherText);
        byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);
        return new String(plainTextBytes, StandardCharsets.UTF_8);
    }

    public SecretKey generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    public SecretKey generateSecretKey(String stringKey){
        byte[] decodedKey = toBase64Bytes(stringKey);
        SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
        this.secretKey = secretKey;
        return secretKey;
    }

    public SecretKey getSecretKey(String alias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);
        return ((SecretKey)keyStore.getKey(alias,null));
    }

    public Cipher initCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        SecretKey secretKey = this.getSecretKey("account");
        if (secretKey == null){
            secretKey = this.generateSecretKey(
                new KeyGenParameterSpec.Builder(
                        "account",
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .setInvalidatedByBiometricEnrollment(true)
                        .build()
            );
        }

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher;
    }

    private String toBase64String(byte[] bytes){
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private byte[] toBase64Bytes(String string){
        return Base64.decode(string, Base64.DEFAULT);
    }

    // In Firestore, "/" is a path segment
    private String sanitize(String string){
        string = string.trim().replaceAll("\n", "").replaceAll("\r", "");
        if (string.contains("/")){
            return string.replaceAll("/", "~");
        }
        else if (string.contains("~")){
            return string.replaceAll("~", "/");
        }
        return string;
    }
}
