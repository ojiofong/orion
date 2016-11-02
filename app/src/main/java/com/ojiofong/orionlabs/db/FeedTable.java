package com.ojiofong.orionlabs.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class FeedTable {

    public static final String TABLE_NAME = "feed_data_table";
    public static final Uri CONTENT_URI = Uri.parse(MyContentProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    public static final String _ID = BaseColumns._ID;
    public static final String COLUMN_ID = "feed_data_id";
    public static final String COLUMN_DESCRIPTION = "feed_data_description";
    public static final String COLUMN_ADDRESS = "feed_data_address";
    public static final String COLUMN_CATEGORY = "feed_data_category";
    public static final String COLUMN_DAY_OF_WEEK = "feed_data_day_of_week";
    public static final String COLUMN_DISTRICT = "feed_data_district";
    public static final String COLUMN_LONGITUDE = "feed_data_longitude";
    public static final String COLUMN_LATITUDE = "feed_data_latitude";
    public static final String COLUMN_TIMESTAMP = "feed_data_time_stamp";
    public static final String DEFAULT_ORDER = COLUMN_TIMESTAMP + " DESC";
    public static final String ATOZ_ORDER = COLUMN_DESCRIPTION + " COLLATE NOCASE";

}
