package com.example.accountvault;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    Context context;
    List<String> urls;
    Map<String, List<String>> accounts;
    LayoutInflater inflater;

    public Adapter(Context context, List<String> websites, Map<String, List<String>> accounts){
        this.context = context;
        this.urls = websites;
        this.accounts = accounts;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.custom_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            String url = this.urls.get(position);
            holder.website.setText(this.getDomainName(url));
            this.setImages(holder.imageView, url);

            final String[] items = accounts.get(url).toArray(new String[0]);
            final boolean[] checkedItems = new boolean[items.length];

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Accounts");
                    builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                            checkedItems[i] = b;
                        }
                    });
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (int j=0; j < checkedItems.length; j++){
                                if (checkedItems[j]){
                                    String id = items[j].split("\n")[0];
                                    try {
                                        String cipherTextWebsite = Home.firestore.crypto.encrypt(url);
                                        String cipherTextId = Home.firestore.crypto.encrypt(id);
                                        Home.firestore.delete(cipherTextWebsite, cipherTextId);
                                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            Arrays.fill(checkedItems, false);
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    public String getDomainName(String url) throws URISyntaxException {
        if (!url.startsWith("https://")){
            url = "https://" + url;
        }
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    // https://square.github.io/picasso/
    public void setImages(ImageView imageView, String url){
        Picasso.with(this.context)
                .load("https://logo.clearbit.com/" + url)
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .into(imageView);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView website;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            website = itemView.findViewById(R.id.textView2);
            imageView = itemView.findViewById(R.id.imageView2);
        }
    }
}
