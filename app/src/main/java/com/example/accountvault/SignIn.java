package com.example.accountvault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SignIn extends DialogFragment {

    protected static SignIn newInstance(){
        return new SignIn();
    }

    public static String TAG = "SignInDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        byte[] byteFingerCiphertext = getArguments().getByteArray("fingerCiphertext");
        SecretKey fingerKey = new SecretKeySpec(byteFingerCiphertext, 0, byteFingerCiphertext.length, "AES");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        this.setCancelable(false);

        builder.setView(inflater.inflate(R.layout.signin, null));
        builder.setPositiveButton(R.string.sign_in, (dialog, id) -> {
            EditText editText = (EditText) getDialog().getWindow().findViewById(R.id.password);
            String plaintextPass = editText.getText().toString();
            Cryptography crypto = new Cryptography();
            try {
                // Encrypt password and save with Shared Preferences
                String ciphertextPass = crypto.encrypt(plaintextPass, fingerKey);
                CustomSharedPreferences csp = new CustomSharedPreferences(getActivity(), "account");
                csp.putString("password", ciphertextPass);

                // Convert password to Secret Key
                SecretKey passKey = crypto.toSecretKey(plaintextPass.getBytes(StandardCharsets.UTF_8));
                Home.firestore = new Firestore(passKey);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
                e.printStackTrace();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SignIn.this.getDialog().cancel();
                getActivity().finish();
                System.exit(0);
            }
        });
        return builder.create();
    }
}
