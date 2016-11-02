package com.ojiofong.orionlabs.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.ojiofong.orionlabs.Const;

/**
 * Created by ojiofong on 12/17/15.
 * .
 */
public class MyContentProvider extends ContentProvider {

    private static final String TAG = MyContentProvider.class.getSimpleName();

    private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
    private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

    public static String CONTENT_URI_BASE = "content://" + Const.AUTHORITY;

    private static final String QUERY_NOTIFY = "QUERY_NOTIFY";
    private static final String QUERY_GROUP_BY = "QUERY_GROUP_BY";

    private static final int URI_TYPE_FEED_DATA_TABLE = 0;
    private static final int URI_TYPE_FEED_DATA_TABLE_ID = 1;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private void addURIs() {

        CONTENT_URI_BASE = "content://" + Const.AUTHORITY;

        URI_MATCHER.addURI(Const.AUTHORITY, FeedTable.TABLE_NAME, URI_TYPE_FEED_DATA_TABLE);
        URI_MATCHER.addURI(Const.AUTHORITY, FeedTable.TABLE_NAME + "/#", URI_TYPE_FEED_DATA_TABLE_ID);


    }

    private MyDbHelper myDbHelper;
    private Context context;

    @Override
    public boolean onCreate() {
        this.context = getContext();
        addURIs();
        myDbHelper = new MyDbHelper(getContext(), Const.DATABASE_NAME, Const.DATABASE_VERSION);
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_TYPE_FEED_DATA_TABLE:
                return TYPE_CURSOR_DIR + FeedTable.TABLE_NAME;
            case URI_TYPE_FEED_DATA_TABLE_ID:
                return TYPE_CURSOR_ITEM + FeedTable.TABLE_NAME;
        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final String table = uri.getLastPathSegment();
        final long rowId = myDbHelper.getWritableDatabase().insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        String notify;
        if (rowId != -1 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            context.getContentResolver().notifyChange(uri, null, false);
        }
        return uri.buildUpon().appendEncodedPath(String.valueOf(rowId)).build();
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final String table = uri.getLastPathSegment();
        final SQLiteDatabase db = myDbHelper.getWritableDatabase();
        int res = 0;
        db.beginTransaction();
        try {
            for (final ContentValues v : values) {
                final long id = db.insertWithOnConflict(table, null, v, SQLiteDatabase.CONFLICT_IGNORE);
                if (id != -1) {
                    res++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            context.getContentResolver().notifyChange(uri, null, false);
        }

        return res;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final QueryParams queryParams = getQueryParams(uri, selection);
        int res = myDbHelper.getWritableDatabase().updateWithOnConflict(queryParams.table, values, queryParams.selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);

        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            context.getContentResolver().notifyChange(uri, null, false);
        }
        return res;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final QueryParams queryParams = getQueryParams(uri, selection);
        final int res = myDbHelper.getWritableDatabase().delete(queryParams.table, queryParams.selection, selectionArgs);
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            context.getContentResolver().notifyChange(uri, null, false);
        }
        return res;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final String groupBy = uri.getQueryParameter(QUERY_GROUP_BY);
        final QueryParams queryParams = getQueryParams(uri, selection);
        final Cursor res = myDbHelper.getReadableDatabase().query(queryParams.table, projection, queryParams.selection, selectionArgs, groupBy,
                null, sortOrder == null ? queryParams.orderBy : sortOrder);
        res.setNotificationUri(context.getContentResolver(), uri);
        return res;
    }

    private static class QueryParams {
        String table;
        String selection;
        String orderBy;
    }

    private QueryParams getQueryParams(Uri uri, String selection) {
        QueryParams res = new QueryParams();
        String id = null;
        int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_FEED_DATA_TABLE:
            case URI_TYPE_FEED_DATA_TABLE_ID:
                res.table = FeedTable.TABLE_NAME;
                res.orderBy = FeedTable.DEFAULT_ORDER;
                break;

            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }

        switch (matchedId) {
            case URI_TYPE_FEED_DATA_TABLE_ID:
                id = uri.getLastPathSegment();
        }
        if (id != null) {
            if (selection != null) {
                res.selection = BaseColumns._ID + "=" + id + " and (" + selection + ")";
            } else {
                res.selection = BaseColumns._ID + "=" + id;
            }
        } else {
            res.selection = selection;
        }
        return res;
    }

    public static Uri notify(Uri uri, boolean notify) {
        return uri.buildUpon().appendQueryParameter(QUERY_NOTIFY, String.valueOf(notify)).build();
    }

    public static Uri groupBy(Uri uri, String groupBy) {
        return uri.buildUpon().appendQueryParameter(QUERY_GROUP_BY, groupBy).build();
    }
}
