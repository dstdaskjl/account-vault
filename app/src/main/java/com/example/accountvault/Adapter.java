package com.example.accountvault;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    List<String> urls;
    LayoutInflater inflater;
    Context context;

    public Adapter(Context context, List<String> titles){
        this.urls = titles;
        this.inflater = LayoutInflater.from(context);
        this.context = context;
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
            this.setImages(holder.image, url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    public String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public void setImages(ImageView imageView, String url){
        Picasso.with(this.context)
                .load("https://logo.clearbit.com/" + url)
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .into(imageView);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView website;
        ImageView image;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            website = itemView.findViewById(R.id.textView2);
            image = itemView.findViewById(R.id.imageView2);
        }
    }
}
