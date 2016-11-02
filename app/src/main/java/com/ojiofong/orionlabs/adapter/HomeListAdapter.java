package com.ojiofong.orionlabs.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.ojiofong.orionlabs.R;
import com.ojiofong.orionlabs.db.FeedTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeListAdapter extends CursorAdapter {

    private static final String TAG = HomeListAdapter.class.getSimpleName();


    public HomeListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        convertView.setTag(new ViewHolder(convertView));
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {


        int id  = cursor.getInt(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_ID));
        String title  = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_DESCRIPTION));
        String bodyTime  = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_TIMESTAMP));
        String address  = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_ADDRESS));

        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(bodyTime);
            date.getMonth();
            Log.d(TAG, "month: " + date.getMonth() + " stamp: "+ date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.idText.setText(String.valueOf(id));
        holder.titleText.setText(title);
        holder.bodyText.setText(bodyTime + "\n" + address);
    }

    private static class ViewHolder {
        TextView idText, titleText, bodyText;

        ViewHolder(View convertView) {
            idText = (TextView) convertView.findViewById(R.id.text_view_id);
            titleText = (TextView) convertView.findViewById(R.id.text_view_title);
            bodyText = (TextView) convertView.findViewById(R.id.text_view_body);

        }
    }
}
