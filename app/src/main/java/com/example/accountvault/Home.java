package com.example.accountvault;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Home extends AppCompatActivity {
    public static Firestore firestore;
    RecyclerView recyclerView;
    List<String> urls;
    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        recyclerView = findViewById(R.id.recycleView);

        try {
            this.authenticate();
        } catch (UnrecoverableKeyException | NoSuchPaddingException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    private void authenticate() throws UnrecoverableKeyException, NoSuchPaddingException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(Home.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Home.this.finish();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    Cryptography crypto = new Cryptography();
                    CustomSharedPreferences csp = new CustomSharedPreferences(Home.this, "account");
                    byte[] byteFingerCiphertext = result.getCryptoObject().getCipher().doFinal();

                    // Initialize firestore class
                    if (csp.getString("password").equals("")){
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("fingerCiphertext", byteFingerCiphertext);
                        SignIn signIn = new SignIn();
                        signIn.setArguments(bundle);
                        signIn.show(getSupportFragmentManager(), SignIn.TAG);
                    }
                    else{
                        SecretKey fingerKey = crypto.toSecretKey(byteFingerCiphertext);
                        String password = crypto.decrypt(csp.getString("password"), fingerKey);
                        byte[] bytePassword = password.getBytes(StandardCharsets.UTF_8);
                        SecretKey passKey = crypto.toSecretKey(bytePassword);
                        firestore = new Firestore(passKey);
                        Home.this.initGridLayout();
                    }
                } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(
                promptInfo,
                new BiometricPrompt.CryptoObject(new Cryptography().initCipher())
        );
    }


    // https://www.youtube.com/watch?v=cYjX6_TL_EA&ab_channel=SmallAcademy
    // https://square.github.io/picasso/
    public void setImages(ImageView imageView, String url){
        Picasso.with(this)
                .load("https://logo.clearbit.com/" + url)
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .into(imageView);
    }

    public void initGridLayout(){
        this.urls = new ArrayList<String>(){
            {
                add("google.com");
            }
        };
        adapter = new Adapter(this, urls);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }
}