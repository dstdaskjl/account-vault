package com.example.accountvault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

public class SignIn extends DialogFragment {
    public static String TAG = "SignInDialog";
    private AlertDialog dialog;

    protected static SignIn newInstance(){
        return new SignIn();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        this.setCancelable(false);

        builder.setView(inflater.inflate(R.layout.signin, null));
        builder.setPositiveButton(R.string.sign_in, (dialog, id) -> {
            Cryptography crypto = new Cryptography(null);
            CustomSharedPreferences csp = new CustomSharedPreferences(getActivity(), "account");
            EditText passEditText = getDialog().getWindow().findViewById(R.id.password);
            EditText passConfirmEditText = getDialog().getWindow().findViewById(R.id.password_confirm);

            try {
                String plainTextPass = passEditText.getText().toString();
                String plainTextPassConfirm = passConfirmEditText.getText().toString();
                String hashedPass = crypto.hash(plainTextPass);
                csp.putString("master_password", hashedPass);

                SecretKey secretKey = crypto.generateSecretKey(hashedPass);
                Home.firestore = new Firestore(secretKey, ((Home)getActivity()).adapter);
                Home.firestore.refresh();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });

        dialog = builder.create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        setTextWatcher();
        dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void setTextWatcher(){
        EditText passEditText = getDialog().getWindow().findViewById(R.id.password);
        EditText passconfirmEditText = getDialog().getWindow().findViewById(R.id.password_confirm);
        TextView warningTextView = getDialog().getWindow().findViewById(R.id.warning);

        passEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String pass1 = passEditText.getText().toString();
                String pass2 = passconfirmEditText.getText().toString();
                if (!pass1.equals("") && pass1.equals(pass2)){
                    warningTextView.setVisibility(View.INVISIBLE);
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                }
                else{
                    warningTextView.setVisibility(View.VISIBLE);
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        passconfirmEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String pass1 = passEditText.getText().toString();
                String pass2 = passconfirmEditText.getText().toString();
                if (!pass1.equals("") && pass1.equals(pass2)){
                    warningTextView.setVisibility(View.INVISIBLE);
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                }
                else{
                    warningTextView.setVisibility(View.VISIBLE);
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }
}
