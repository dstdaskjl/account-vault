package com.example.accountvault;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

// https://firebase.google.com/docs/firestore/quickstart#secure_your_data

public class Firestore {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Cryptography crypto = new Cryptography();
    private final SecretKey secretKey;

    public Firestore(SecretKey secretKey){
        this.secretKey = secretKey;
    }

    public void add(String website, String id, String passHint) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        String encryptedWebsite = this.crypto.encrypt(website);
        String encryptedId = this.crypto.encrypt(id);
        String encryptedPassHint = this.crypto.encrypt(passHint);

        Map<String, Object> account = new HashMap<>();
        account.put("id", encryptedId);
        account.put("password hint", encryptedPassHint);

        this.db.collection(encryptedWebsite)
                .add(account)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }
}