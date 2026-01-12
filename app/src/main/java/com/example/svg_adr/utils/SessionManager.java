package com.example.svg_adr.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.svg_adr.model.User;
import com.google.gson.Gson;

/**
 * SessionManager - Manages user session using SharedPreferences
 */
public class SessionManager {
    private static final String PREF_NAME = "svg_adr_session";
    private static final String KEY_USER = "current_user";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        editor.putString(KEY_USER, userJson);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public User getUser() {
        String userJson = prefs.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public String getUserRole() {
        User user = getUser();
        return user != null ? user.getRole() : null;
    }

    public String getUserId() {
        User user = getUser();
        return user != null ? user.getId() : null;
    }
}
