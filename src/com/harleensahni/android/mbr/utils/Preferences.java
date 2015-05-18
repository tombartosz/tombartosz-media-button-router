package com.harleensahni.android.mbr.utils;

import android.content.SharedPreferences;

import com.harleensahni.android.mbr.Constants;

/**
 * Created by tombartosz on 18.05.15.
 */
public class Preferences {

    private static SharedPreferences sharedPreferences;

    public static void init(SharedPreferences sharedPreferences) {
        Preferences.sharedPreferences = sharedPreferences;
    }

    public static boolean isEnabled() {
        boolean isEnabled = sharedPreferences.getBoolean(Constants.ENABLED_PREF_KEY, true);
        return isEnabled;
    }
}
