package com.example.accountvault;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

// https://firebase.google.com/docs/firestore/quickstart#secure_your_data

/*
    // Firebase Format

    Collection      Document        Collection
    "websites"      website1
                    website2
                    website3

    website1        id1             pass hint
                    id2             pass hint
    website2        id3             pass hint
                    id4             pass hint
                    id5             pass hint
 */

public class Firestore {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Cryptography crypto = new Cryptography();
    private final SecretKey secretKey;
    private final Adapter adapter;
    private Map<String, List<Map<String, String>>> accounts;

    public Firestore(SecretKey secretKey, Adapter adapter){
        this.secretKey = secretKey;
        this.adapter = adapter;
    }

    public void add(String website, String id, String passHint) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        String cipherTextWebsite = sanitize(crypto.encrypt(secretKey, website));
        String cipherTextId = sanitize(crypto.encrypt(secretKey, id));
        String cipherTextPassHint = sanitize(crypto.encrypt(secretKey, passHint));

        Map<String, Object> pass_hint = new HashMap<>();
        pass_hint.put("password hint", cipherTextPassHint);

        db.collection(cipherTextWebsite).document(cipherTextId).set(pass_hint);
        db.collection("websites").document(cipherTextWebsite).set(new HashMap<>());
        refresh();
    }

    public void delete(String collection, String document){
        db.collection(collection).document(document)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
    }

    public void refresh(){
        accounts = (Map<String, List<Map<String, String>>>) new HashMap<String, List<Map<String, String>>>();
        setAccounts();
    }

    private void setAccounts(){
        db.collection("websites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String website = document.getId();
                            db.collection(website)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()){
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    String accountID = document.getId();
                                                    db.collection(website).document(accountID)
                                                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                    DocumentSnapshot ds = task.getResult();
                                                                    if (ds.exists()){
                                                                        String passHint = (String) ds.getData().get("password hint");

                                                                        try {
                                                                            String plainTextWebsite = crypto.decrypt(secretKey, sanitize(website));
                                                                            String plainTextId = crypto.decrypt(secretKey, sanitize(accountID));
                                                                            String plainTextPassHint = crypto.decrypt(secretKey, sanitize(passHint));

                                                                            Map<String, String> account = new HashMap<>();
                                                                            account.put("id", plainTextId);
                                                                            account.put("password hint", plainTextPassHint);

                                                                            if (accounts.containsKey(plainTextWebsite)){
                                                                                accounts.get(plainTextWebsite).add(account);
                                                                            }
                                                                            else{
                                                                                accounts.put(plainTextWebsite, new ArrayList<Map<String, String>>(){{add(account);}});
                                                                            }
                                                                            Firestore.this.updateAdapter();

                                                                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
                                                                            e.printStackTrace();
                                                                        }

                                                                    }
                                                                    else{
                                                                        Log.e("Refresh error", "The document snapshot does not exist");
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                            else {
                                                Log.w(TAG, "Error getting documents.", task.getException());
                                            }

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }
                    }
                    else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }

                });
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

    @SuppressLint("NotifyDataSetChanged")
    private void updateAdapter(){
        List<String> websites = new ArrayList<>(accounts.keySet());
        Collections.sort(websites);
        adapter.urls = websites;
        adapter.notifyDataSetChanged();
    }
}