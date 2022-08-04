package com.example.accountvault;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class CustomSharedPreferences {
    final private SharedPreferences sharedPreferences;
    final private SharedPreferences.Editor editor;

    public CustomSharedPreferences(Activity activity, String preference){
        this.sharedPreferences = activity.getSharedPreferences(preference, Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    public boolean getBoolean(String key){
        return sharedPreferences.getBoolean(key, false);
    }

    public int getInt(String key){
        return sharedPreferences.getInt(key, 0);
    }

    public long getLong(String key) { return sharedPreferences.getLong(key, 0); }

    public String getString(String key){
        return sharedPreferences.getString(key, "");
    }

    public void putBoolean(String key, boolean value){
        editor.putBoolean(key, value).commit();
    }

    public void putInt(String key, int value){
        editor.putInt(key, value).commit();
    }

    public void putLong(String key, long value) { editor.putLong(key, value).commit(); }

    public void putString(String key, String value){
        editor.putString(key, value).commit();
    }

    public boolean contains(String key){
        return sharedPreferences.contains(key);
    }

    public void reset(){
        editor.clear().commit();
    }
}
