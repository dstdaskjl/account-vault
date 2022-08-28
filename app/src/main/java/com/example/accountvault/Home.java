package com.example.accountvault;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Home extends AppCompatActivity {
    public static Firestore firestore;
    RecyclerView recyclerView;
    Adapter adapter;
    boolean authenticated = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        recyclerView = findViewById(R.id.recycleView);
        recyclerView.getLayoutParams().width = 1080;
        recyclerView.getLayoutParams().height = 1920;

        try {
            this.authenticate();
        } catch (UnrecoverableKeyException | NoSuchPaddingException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_menu, menu);
        menu.findItem(R.id.add_button).setIcon(resizeMenuButton(R.drawable.add, 200, 200));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.add_button) {
            if (authenticated){
                new Add().show(getSupportFragmentManager(), Add.TAG);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void initWebsiteList(){
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter = new Adapter(this, new ArrayList<>(), new HashMap<>());
        recyclerView.setAdapter(adapter);
    }

    private void authenticate() throws UnrecoverableKeyException, NoSuchPaddingException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Executor executor = ContextCompat.getMainExecutor(this);

        // Check if biometric is available
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS){
            authenticated = false;
            String message = "Biometric login should be available";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Home.this.finish();
            System.exit(0);
        }

        // Prompt biometric login
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

                Cryptography crypto = new Cryptography(null);
                CustomSharedPreferences csp = new CustomSharedPreferences(Home.this, "account");
                String hashedPass = csp.getString("master_password");
                Home.this.initWebsiteList();

                if (hashedPass.equals("")){
                    SignIn signIn = new SignIn();
                    signIn.show(getSupportFragmentManager(), SignIn.TAG);

                }
                else{
                    SecretKey secretKey = crypto.generateSecretKey(hashedPass);
                    firestore = new Firestore(secretKey, adapter);
                    firestore.refresh();
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
                new BiometricPrompt.CryptoObject(new Cryptography(null).initCipher())
        );
    }

    private Drawable resizeMenuButton(int resId, int w, int h)
    {
        Bitmap BitmapOrg = BitmapFactory.decodeResource(getResources(), resId);
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        float scaleWidth = ((float) w) / width;
        float scaleHeight = ((float) h) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0,width, height, matrix, true);
        return new BitmapDrawable(resizedBitmap);
    }
}