package com.ojiofong.orionlabs;

import android.content.Context;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

public class Util {

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
