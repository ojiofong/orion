package com.ojiofong.orionlabs.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ojiofong.orionlabs.Const;
import com.ojiofong.orionlabs.R;
import com.ojiofong.orionlabs.db.FeedTable;
import com.ojiofong.orionlabs.service.DataService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MapActivity extends AppCompatActivity implements LocationListener, LoaderManager.LoaderCallbacks<Cursor> {

    private final static String TAG = MapActivity.class.getSimpleName();

    SupportMapFragment mf;
    GoogleMap googlemap;
    String actionTitleFromIntent;
    Double getLat, getLng;
    boolean didUserPreviouslyDisablePermission = false;
    private static final int PERMISSION_REQ_LOCATION = 1;
    private static final int LOADER_ID = 1;
    ArrayAdapter<String> adapter = null;
    JSONArray myJsonArray;
    Set<String> markerSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initToolBar();
        setupAutoCompleteTextView();

        mf = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (savedInstanceState == null) {
            mf.setRetainInstance(true);
        }

        setupMapIfNeeded();

        shouldAskForLocationPermission();

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    private void initToolBar() {
        // initialize ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(actionTitleFromIntent);
            }
        }


    }


    private void setupMapIfNeeded() {
        if (googlemap == null) {
            mf.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mGoogleMap) {
                    initMap(mGoogleMap);
                }
            });
        }
    }

    private void initMap(GoogleMap googleMap) {
        googlemap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(this, "no location permission here", Toast.LENGTH_SHORT).show();
            return;
        }
        googlemap.setMyLocationEnabled(true);
        googlemap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Somewhere in SF
        getLat = 37.773972;
        getLng = -122.389977;
        googlemap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getLat, getLng), 12));
//        googlemap.animateCamera(CameraUpdateFactory.newCameraPosition(getCameraPosition()));

        addMarkertoMap();
        makeMarkerInfoWindowClickable();
        detectMarkerTouch();
        makeMapBodyClickable();
        setUpCameraChangeListener();


    }

//    private CameraPosition getCameraPosition() {
//        return new CameraPosition.Builder().target(new LatLng(getLat, getLng))
//                .zoom(11.0f)
//                .bearing(0)
//                .tilt(25)
//                .build();
//    }

    private void addMarkertoMap() {

        // Adding the ArrayList of HashMap
//        for (int i = 0; i < PlaceListActivity.placesListItems.size(); i++) {
//
//            HashMap<String, String> currentP = PlaceListActivity.placesListItems.get(i);
//            String placeName = currentP.get(PlaceListActivity.KEY_NAME);
//            String placeAddy = currentP.get(PlaceListActivity.KEY_ADDRESS);
//            String lat = currentP.get(PlaceListActivity.KEY_LATITUDE);
//            String lon = currentP.get(PlaceListActivity.KEY_LONGITUDE);
//            Double latDouble = Double.parseDouble(lat); // converting string to double
//            Double lonDouble = Double.parseDouble(lon); // converting string to double
//
//            Marker marker = googlemap.addMarker(new MarkerOptions().title(placeName).snippet(placeAddy).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker))
//                    .position(new LatLng(latDouble, lonDouble)));
//
//           // dropPinEffect(marker);
//        }

    }

    private void makeMarkerInfoWindowClickable() {
        googlemap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                // TODO Auto-generated method stub

            }
        });
    }

    private void makeMapBodyClickable() {
        googlemap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {
                // hide map direction icons
                Log.d(TAG, "onMapClick");

            }
        });
    }

    private void detectMarkerTouch() {
        googlemap.setOnMarkerClickListener(new OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

        Context mContext = new ContextThemeWrapper(this, R.style.AppTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(getString(R.string.gps_location_settings));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.enable_gps_message), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Intent startGps = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(startGps);

            }
        });

        // to markerSet when the negative button is clicked
        builder.setNegativeButton(getString(R.string.leave_gps_off), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.cancel();

            }
        });

        // Now let's call our alert Dialog
        AlertDialog alert = builder.create();
        alert.show();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.map, menu);
        // MenuItem mapItem = menu.findItem(R.id.action_grid);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                this.finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }


    private boolean shouldAskForLocationPermission() {

        // If permission is not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (didUserPreviouslyDisablePermission) {
                    // If user previously disabled rationale - needs to go to app details to enable permission
                    showRationaleViewForUserDisable();
                } else {
                    // User never previously denied permission
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_LOCATION);
                }

            } else {
                // should show rationale
                // user did not disable rationale - request for permission directly
                showRationaleView();
            }

            return true;
        }

        return false;

    }

    private void showRationaleView() {
        Snackbar.make(findViewById(R.id.map_layout)
                , getString(R.string.location_permission_required)
                , Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.okay), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // After the user sees the explanation and clicks ok, try again to request the permission.
                ActivityCompat.requestPermissions(MapActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_LOCATION);

            }
        }).show();
    }

    private void showRationaleViewForUserDisable() {
        Snackbar.make(findViewById(R.id.map_layout)
                , getString(R.string.location_permission_required)
                , Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.settings), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // must take user to App info settings
                Intent intent = appDetailsIntent(getPackageName());
                startActivity(intent);

            }
        }).show();
    }

    public static Intent appDetailsIntent(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + packageName));
            return intent;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails");
            intent.putExtra("pkg", packageName);
            return intent;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.android.settings",
                "com.android.settings.InstalledAppDetails");
        intent.putExtra("com.android.settings.ApplicationPkgName", packageName);
        return intent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    didUserPreviouslyDisablePermission = false;

                    // permission was granted, yay! Do the
                    // permission-related task you need to do.
                    //mGoogleApiClient.connect();
//                    startLocationUpdates();
                    initMap(googlemap);

                } else {

                    didUserPreviouslyDisablePermission = true;

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //mGoogleApiClient.disconnect();
//                    stopLocationUpdates();
                }
            }
        }
    }

    private void setUpCameraChangeListener() {
        googlemap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                Log.d(TAG, "onCameraMove");
            }
        });

        googlemap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.d(TAG, "onCameraIdle");
                getSupportLoaderManager().restartLoader(LOADER_ID, null, MapActivity.this);

            }
        });
    }


    private void addMarkerToBounds(LatLngBounds bounds, Cursor cursor) {

        try {

            if (cursor.moveToFirst()) {
                while (cursor.moveToNext()) {

                    String pdId = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_ID));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_DESCRIPTION));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_ADDRESS));
                    Double lat = Double.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_LATITUDE)));
                    Double lng = Double.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(FeedTable.COLUMN_LONGITUDE)));
                    LatLng latlng = new LatLng(lat, lng);

                    Log.d(TAG, "lat:" + lat + " lng:" + lng);

                    if (!markerSet.contains(pdId) && bounds.contains(latlng)) {

                        Marker marker = googlemap.addMarker(
                                new MarkerOptions()
                                        .title(description)
                                        .snippet(address)
                                        .icon(getMarkerIcon("#FF0000"))
                                        .position(latlng));
                        markerSet.add(pdId);

                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

    }

    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, FeedTable.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished");
        LatLngBounds bounds = googlemap.getProjection().getVisibleRegion().latLngBounds;
        addMarkerToBounds(bounds, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void setupAutoCompleteTextView() {

        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);

        adapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1);
        adapter.setNotifyOnChange(true);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String s = charSequence.toString();
                if (s.length() >= 3 && count > before) {
                    new GetPlaces().execute(s);
                    Log.d(TAG, "onTextChanged " + s);
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {

                hideKeyBoard();

                try {
                    String reference = myJsonArray.getJSONObject(pos).getString("reference");
                    new GetFurtherDetailsTask().execute(reference);
//                    Toast.makeText(MapActivity.this, reference, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class GetPlaces extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            ArrayList<String> predictionsArr = new ArrayList<>();

            try {
                URL googlePlaces = new URL(
                        // URLEncoder.encode(url,"UTF-8");
                        "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" + URLEncoder.encode(params[0], "UTF-8")
                                + "&types=geocode&language=en&sensor=false&key=" + Const.GOOGLE_API_KEY);

                URLConnection conn = googlePlaces.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String line;
                StringBuilder sb = new StringBuilder();
                // take Google's legible JSON and turn it into one big string.
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }

                // turn that string into a JSON object
                JSONObject predictions = new JSONObject(sb.toString());
                // now get the JSON array that's inside that object
                myJsonArray = new JSONArray(predictions.getString("predictions"));

                for (int i = 0; i < myJsonArray.length(); i++) {
                    JSONObject jo = (JSONObject) myJsonArray.get(i);
                    // add each entry to our array
                    predictionsArr.add(jo.getString("description"));
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();

            }

            return predictionsArr;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);
            // update the adapter
            setupAutoCompleteTextView();
            adapter.setNotifyOnChange(true);

            for (String string : result) {
                adapter.add(string);
                adapter.notifyDataSetChanged();

            }
        }

    }

    // Additional methods to retrieve lat and lng starts here ---------
    private String getUrlContents(String reference) {

        StringBuilder builder = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?");
        builder.append("reference=");
        builder.append(reference);
        builder.append("&sensor=false&key=").append(Const.GOOGLE_API_KEY);

        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(builder.toString());
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 8);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return content.toString();

    }

    private class GetFurtherDetailsTask extends AsyncTask<String, Void, Void> {
        String jsonResult;
        String myReference;
        String latitude;
        String longitude;
        JSONObject jsonobject;

        @Override
        protected Void doInBackground(String... params) {

            myReference = params[0];
            jsonResult = getUrlContents(myReference);

            try {
                jsonobject = new JSONObject(jsonResult).getJSONObject("result").getJSONObject("geometry").getJSONObject("location");

                latitude = jsonobject.getString("lat");
                longitude = jsonobject.getString("lng");

                Log.d(TAG, String.format("auto lat:%s lng:%s", latitude, longitude));

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (latitude == null || longitude == null) return;
            // move map to location
            googlemap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(Double.valueOf(latitude), Double.valueOf(longitude)), 14));

            // Fetch data from this location
            Intent intent = new Intent(MapActivity.this, DataService.class);
            intent.putExtra(Const.KEY_LAT, latitude);
            intent.putExtra(Const.KEY_LNG, longitude);
            startService(intent);

        }
    }

    private void hideKeyBoard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    // ---
}