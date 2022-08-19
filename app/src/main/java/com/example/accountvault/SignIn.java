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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        this.setCancelable(false);

        builder.setView(inflater.inflate(R.layout.signin, null));
        builder.setPositiveButton(R.string.sign_in, (dialog, id) -> {
            Cryptography crypto = new Cryptography();
            CustomSharedPreferences csp = new CustomSharedPreferences(getActivity(), "account");
            EditText editText = getDialog().getWindow().findViewById(R.id.password);

            try {
                String plainTextPass = editText.getText().toString();
                String hashedPass = crypto.hash(plainTextPass);
                csp.putString("password", hashedPass);
                SecretKey secretKey = crypto.generateSecretKey(hashedPass);
                Home.firestore = new Firestore(secretKey);
                ((Home)getActivity()).initGridLayout();
            } catch (NoSuchAlgorithmException e) {
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
