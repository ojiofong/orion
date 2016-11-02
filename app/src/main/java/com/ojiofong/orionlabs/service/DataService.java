package com.ojiofong.orionlabs.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ojiofong.orionlabs.Const;
import com.ojiofong.orionlabs.Util;
import com.ojiofong.orionlabs.db.FeedTable;
import com.ojiofong.orionlabs.model.SFData;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataService extends IntentService {

    private static final String TAG = DataService.class.getSimpleName();


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_SERVER_DOWN, STATUS_SERVER_INVALID, STATUS_NO_INTERNET, STATUS_NO_DATA})
    @interface Status {

    }

    public static final int STATUS_OK = 0;
    public static final int STATUS_SERVER_DOWN = 1;
    public static final int STATUS_SERVER_INVALID = 2;
    public static final int STATUS_NO_INTERNET = 3;
    public static final int STATUS_NO_DATA = 4;

    private void setStatus(@Status int status) {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit().putInt(Const.PREF_STATUS_KEY, status).apply();
    }


    private static OkHttpClient client = new OkHttpClient();


    public DataService() {
        super(DataService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        setStatus(STATUS_OK);

        if (!Util.isNetworkConnected(this)) {
            setStatus(STATUS_NO_INTERNET);
            return;
        }

        sendLocalBroadCast(Const.ACTION_SHOW_PROGRESS_DIALOG);


        try {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                boolean shouldClearDb = bundle.getBoolean(Const.KEY_FROM_ONCREATE);
                int limit = bundle.getInt(Const.KEY_LIMIT);
                int offset = bundle.getInt(Const.KEY_OFFSET);
                String lat = bundle.getString(Const.KEY_LAT);
                String lng = bundle.getString(Const.KEY_LNG);
                String result;

                if (lat != null && lng != null) {
                    result = getWebDataByLocation(lat, lng);
                } else {
                    result = getWebData(limit, offset);
                }

                Type collectionTypeSF = new TypeToken<List<SFData>>() {
                }.getType();
                List<SFData> list = new Gson().fromJson(result, collectionTypeSF);

                if (list == null) {
                    setStatus(STATUS_SERVER_INVALID);
                    return;

                } else if (list.isEmpty()) {
                    setStatus(STATUS_NO_DATA);
                    return;
                }


                addToDatabase(this, list, shouldClearDb);

            }


        } catch (IOException e) {
            e.printStackTrace();
            setStatus(STATUS_SERVER_DOWN);
            sendLocalBroadCast(Const.ACTION_DISMISS_PROGRESS_DIALOG);

        }

        sendLocalBroadCast(Const.ACTION_DISMISS_PROGRESS_DIALOG);

    }

    private static String getWebDataByLocation(String lat, String lng) throws IOException {

        StringBuilder sb = new StringBuilder("https://data.sfgov.org/resource/ritf-b9ki.json?");
        sb.append(String.format("$where=within_circle(location, %s, %s, 100)", lat, lng));

        String url = sb.toString();

        Log.d(TAG, "url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }


    private static String getWebData(int limit, int offset) throws IOException {

        StringBuilder sb = new StringBuilder("https://data.sfgov.org/resource/ritf-b9ki.json?");
        sb.append("$limit=").append(limit);
        sb.append("&$offset=").append(offset);

        String url = sb.toString();

        Log.d(TAG, "final url: " + url);

        //String url = "https://data.sfgov.org/resource/ritf-b9ki.json?$limit=5&$offset=0";
        //String url = "https://data.sfgov.org/resource/ritf-b9ki.json";

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private static void addToDatabase(Context context, List<SFData> list, boolean shouldClearDB) {

        if (list == null || list.isEmpty()) return;

        if (shouldClearDB)
            context.getContentResolver().delete(FeedTable.CONTENT_URI, null, null);

        ContentValues values[] = new ContentValues[list.size()];

        for (int i = 0; i < list.size(); i++) {
            ContentValues value = new ContentValues();
            value.put(FeedTable.COLUMN_ID, list.get(i).getPdid());
            value.put(FeedTable.COLUMN_DESCRIPTION, list.get(i).getDescript());
            value.put(FeedTable.COLUMN_ADDRESS, list.get(i).getAddress());
            value.put(FeedTable.COLUMN_LATITUDE, list.get(i).getLocation().getLatitude());
            value.put(FeedTable.COLUMN_LONGITUDE, list.get(i).getLocation().getLongitude());
            values[i] = value;
        }

        context.getContentResolver().bulkInsert(FeedTable.CONTENT_URI, values);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendLocalBroadCast(Const.ACTION_DISMISS_PROGRESS_DIALOG);
    }

    private void sendLocalBroadCast(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
