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
    public final Cryptography crypto;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Adapter adapter;
    private Map<String, List<String>> accounts;

    public Firestore(SecretKey secretKey, Adapter adapter){
        this.crypto = new Cryptography(secretKey);
        this.adapter = adapter;
    }

    public void add(String website, String id, String passHint) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        String cipherTextWebsite = crypto.encrypt(website);
        String cipherTextId = crypto.encrypt(id);
        String cipherTextPassHint = crypto.encrypt(passHint);

        Map<String, Object> pass_hint = new HashMap<>();
        pass_hint.put("password hint", cipherTextPassHint);

        db.collection(cipherTextWebsite).document(cipherTextId).set(pass_hint);
        db.collection("websites").document(cipherTextWebsite).set(new HashMap<>());
    }

    public void delete(String collection, String document){
        db.collection(collection).document(document).delete();
    }

    public void refresh(){
        accounts = new HashMap<>();
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
                                                                            String plainTextWebsite = crypto.decrypt(website);
                                                                            String plainTextId = crypto.decrypt(accountID);
                                                                            String plainTextPassHint = crypto.decrypt(passHint);

                                                                            String idPass = plainTextId + "\n" + plainTextPassHint;
                                                                            if (accounts.containsKey(plainTextWebsite)){
                                                                                accounts.get(plainTextWebsite).add(idPass);
                                                                            }
                                                                            else{
                                                                                accounts.put(plainTextWebsite, new ArrayList<String>(){{add(idPass);}});
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

    @SuppressLint("NotifyDataSetChanged")
    private void updateAdapter(){
        List<String> websites = new ArrayList<>(accounts.keySet());
        Collections.sort(websites);
        adapter.urls = websites;
        adapter.accounts = accounts;
        adapter.notifyDataSetChanged();
    }
}