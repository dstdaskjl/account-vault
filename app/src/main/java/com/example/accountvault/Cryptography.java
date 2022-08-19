package com.example.accountvault;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
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

    public String hash(String plainText) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedTextBytes = md.digest(Base64.decode(plainText, Base64.DEFAULT));
        return this.toHexString(hashedTextBytes);
    }

    // https://www.javacodegeeks.com/2018/03/aes-encryption-and-decryption-in-javacbc-mode.html
//    public String encrypt(String plainText, SecretKey secretKey, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
//            BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
//        try{
//            Cipher cipher = this.getCipher();
////            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
////            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey, cipher.getParameters());
//            Log.e("TESTSTSTSTSTSTSTSTS", plainText);
//            byte[] plaintextBytes = Base64.decode(plainText, Base64.DEFAULT);
//            Log.e("TESTSTSTSTSTSTSTSTS", plainText + " " + plaintextBytes.length);
//            byte[] encrypted = cipher.doFinal(plainText.getBytes());
//            Log.e("TESTSTSTSTSTSTSTSTS", plainText);
//            return Base64.encodeToString(encrypted, Base64.DEFAULT);
//        }
//        catch (Exception e){
//            Log.e("ErrrrrrrrORORORORR", e.toString());
//            e.printStackTrace();
//        }
//        return "";
//    }

    public String encrypt(String plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
        try{
//            Cipher cipher = this.initCipher();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKey secretKey = this.getSecretKey("crypto");
            if (secretKey == null){
                secretKey = this.generateSecretKey(
                        new KeyGenParameterSpec.Builder(
                                "crypto",
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build()
                );
            }

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);


            byte[] plainTextBytes = Base64.decode(plainText, Base64.DEFAULT);
            byte[] encrypted = cipher.doFinal(plainTextBytes);
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public String decrypt(String cipherText, SecretKey secretKey, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decrypted = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));
            return Base64.encodeToString(decrypted, Base64.DEFAULT);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public byte[] generateIVBytes(int size){
        byte[] iv = new byte[size];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public SecretKey generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    public SecretKey generateSecretKey(String stringKey){
        byte[] decodedKey = Base64.decode(stringKey, Base64.DEFAULT);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    public SecretKey getSecretKey(String alias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);
        return ((SecretKey)keyStore.getKey(alias,null));
    }

//    public Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
////        return Cipher.getInstance("AES/CBC/PKCS5PADDING");
//        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
//                + KeyProperties.BLOCK_MODE_CBC + "/"
//                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//    }

    public Cipher initCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
//        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
//                + KeyProperties.BLOCK_MODE_CBC + "/"
//                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
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

    public SecretKey toSecretKey(byte[] ciphertext){
        return new SecretKeySpec(ciphertext, 0, ciphertext.length, ALGORITHM);
    }

    private String toHexString(byte[] byteArray){
        StringBuilder hexString = new StringBuilder(2 * byteArray.length);
        for (byte b : byteArray) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
