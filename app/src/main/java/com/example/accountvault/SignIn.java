package com.example.accountvault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
            CustomSharedPreferences csp = new CustomSharedPreferences(getActivity(), "account");
            try {
                // Generate IV for password
                byte[] ivBytes = crypto.generateIVBytes(16);
                IvParameterSpec iv = new IvParameterSpec(ivBytes);
                csp.putString("iv", Base64.encodeToString(ivBytes, Base64.DEFAULT));

                // Encrypt password and save with Shared Preferences
                Log.e("DDDDDDDDDDDDDDDDDDDDDDDDDDd", fingerKey.toString());
                String ciphertextPass = crypto.encrypt(plaintextPass, fingerKey, iv);
                csp.putString("password", ciphertextPass);


                // Convert password to Secret Key
                SecretKey passKey = crypto.toSecretKey(Base64.decode(plaintextPass, Base64.DEFAULT));
                Home.firestore = new Firestore(passKey, iv);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
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
