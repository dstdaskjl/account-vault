package com.example.accountvault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Add extends DialogFragment {
    protected static Add newInstance(){
        return new Add();
    }

    public static String TAG = "AddDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
//        this.setCancelable(false);

        builder.setView(inflater.inflate(R.layout.add, null));
        builder.setPositiveButton(R.string.add, (dialog, id) -> {
            EditText websiteEditText = getDialog().getWindow().findViewById(R.id.website);
            EditText idEditText = getDialog().getWindow().findViewById(R.id.id);
            EditText passHintEditText = getDialog().getWindow().findViewById(R.id.pass_hint);

            String website = websiteEditText.getText().toString();
            String account = idEditText.getText().toString();
            String passHint = passHintEditText.getText().toString();

            try {
                Home.firestore.add(website, account, passHint);
            } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Add.this.getDialog().cancel();
            }
        });
        return builder.create();
    }
}
