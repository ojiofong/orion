package com.ojiofong.orionlabs.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ojiofong.orionlabs.Const;
import com.ojiofong.orionlabs.R;
import com.ojiofong.orionlabs.adapter.HomeListAdapter;
import com.ojiofong.orionlabs.db.FeedTable;
import com.ojiofong.orionlabs.service.DataService;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {
    // private static final String TAG = MainActivity.class.getSimpleName();
    private static final int LOADER_ID = 5;
    private static Cursor cursor;
    private HomeListAdapter homeListAdapter;
    private TextView emptyView;
    private static int LIMIT = 10;
    private static int OFFSET = 0;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Const.ACTION_SHOW_PROGRESS_DIALOG.equals(action)) {
                showProgressDialog(true);
            }

            if (Const.ACTION_DISMISS_PROGRESS_DIALOG.equals(action)) {
                showProgressDialog(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolBar();
        setupListView();

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        if (savedInstanceState == null) {
            LIMIT = 5;
            OFFSET = 0;
            startDataService(true);
        }
    }

    private void startDataService(boolean isFromOncreate) {

        Intent intent = new Intent(this, DataService.class);
        intent.putExtra(Const.KEY_FROM_ONCREATE, isFromOncreate);
        intent.putExtra(Const.KEY_LIMIT, LIMIT);
        intent.putExtra(Const.KEY_OFFSET, OFFSET);
        startService(intent);

        OFFSET += LIMIT;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        registerMyReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        unRegisterMyReciever();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }

    private void setupToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            toolbar.inflateMenu(R.menu.main);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_map) {
                        goToMap();
                    }
                    return false;
                }
            });
        }

    }

    private void setupListView() {
        ListView mListView = (ListView) findViewById(R.id.list_view);
        if (mListView != null) {

            emptyView = (TextView) findViewById(R.id.empty_view);
            mListView.setEmptyView(emptyView);

            try {
                cursor = getContentResolver().query(
                        FeedTable.CONTENT_URI,
                        null, // leaving "columns" null returns all the columns.
                        null, // cols for "where" clause
                        null, // values for "where" clause
                        null  // sort order
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

            homeListAdapter = new HomeListAdapter(this, cursor, 0);
            mListView.setAdapter(homeListAdapter);

            Button buttonLoadMore = new Button(this);
            buttonLoadMore.setText(R.string.load_more);
            buttonLoadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startDataService(false);
                }
            });

            mListView.addFooterView(buttonLoadMore);
        }
    }

    private void showProgressDialog(boolean shouldShow) {
        View progressView = findViewById(R.id.progressBarLayout);
        if (progressView != null)
            progressView.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void goToMap() {
        startActivity(new Intent(this, MapActivity.class));
    }

    private void setEmptyViewMessage(String message) {
        if (emptyView != null)
            emptyView.setText(message);
    }

    private void registerMyReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.ACTION_SHOW_PROGRESS_DIALOG);
        filter.addAction(Const.ACTION_DISMISS_PROGRESS_DIALOG);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    private void unRegisterMyReciever() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, FeedTable.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        homeListAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        homeListAdapter.changeCursor(null);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Const.PREF_STATUS_KEY)) {
            switch (sharedPreferences.getInt(key, DataService.STATUS_OK)) {

                case DataService.STATUS_NO_INTERNET:
                    setEmptyViewMessage(getString(R.string.check_internet_connection));
                    Toast.makeText(this, getString(R.string.check_internet_connection), Toast.LENGTH_SHORT).show();
                    showProgressDialog(false);
                    break;

                case DataService.STATUS_NO_DATA:
                    setEmptyViewMessage(getString(R.string.no_data_returned));
                    break;

                case DataService.STATUS_SERVER_DOWN:
                    setEmptyViewMessage(getString(R.string.server_down));
                    break;

                case DataService.STATUS_SERVER_INVALID:
                    setEmptyViewMessage(getString(R.string.server_invalid));
                    break;
            }
        }
    }
}
