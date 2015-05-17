//Простой класс настроек
package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Set;

class Preferences {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    static boolean admin = false; //возможности админа
    static final String PREFERENCES = "preferences";

    static void load(SharedPreferences sp) {
        sharedPreferences = sp;
        editor = sp.edit();
        editor.apply();
    }

    static void write(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    static void write(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    static void write(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    static void write(String key, Set<String> value) {
        editor.putStringSet(key, value);
        editor.commit();
    }

    static String read(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    static boolean read(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    static int read(String key, int in) {
        return sharedPreferences.getInt(key, in);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    static Set<String> read(String key, Set<String> def) {
        return sharedPreferences.getStringSet(key, def);
    }

    static boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    static void remove(String key) {
        editor.remove(key);
        editor.commit();
    }
}