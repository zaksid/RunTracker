package com.bignerdranch.android.runtracker;

import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.ResourceBundle;


public class RunMapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor> {
    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_LOCATIONS = 0;

    private GoogleMap googleMap;
    private RunDatabaseHelper.LocationCursor locationCursor;

    public static RunMapFragment newInstance(long runId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RunMapFragment runMapFragment = new RunMapFragment();
        runMapFragment.setArguments(args);
        return runMapFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            if (runId != -1) {
                LoaderManager manager = getLoaderManager();
                manager.initLoader(LOAD_LOCATIONS, args, this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, parent, savedInstanceState);

        googleMap = getMap();
        googleMap.setMyLocationEnabled(true);

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long runId = args.getLong(ARG_RUN_ID, -1);
        return new LocationListCursorLoader(getActivity(), runId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        locationCursor = (RunDatabaseHelper.LocationCursor) data;
        updateUI();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        locationCursor.close();
        locationCursor = null;
    }

    public void updateUI() {
        if (googleMap == null || locationCursor == null) {
            return;
        }

        // set up an overlay on the map for this run's locations
        // create a polyline with all of the points
        PolylineOptions line = new PolylineOptions();

        // also create a LatLngBounds so we can zoom to fit
        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();

        // iterate over the locations
        locationCursor.moveToFirst();
        while (!locationCursor.isAfterLast()) {
            Location location = locationCursor.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            Resources resources = getResources();

            // if this is the first location, add a marker for it
            if (locationCursor.isFirst()) {
                String startDate = new Date(location.getTime()).toString();
                MarkerOptions startMarkerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(resources.getString(R.string.run_start))
                        .snippet(resources.getString(R.string.run_started_at_format, startDate));
                googleMap.addMarker(startMarkerOptions);
            } else if (locationCursor.isAfterLast()){
                // if this is the last location, and not also the first, add a marker
                String endDate = new Date(location.getTime()).toString();
                MarkerOptions finishMarkerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(resources.getString(R.string.run_finish))
                        .snippet(resources.getString(R.string.run_finished_at_format, endDate));
                googleMap.addMarker(finishMarkerOptions);
            }

            line.add(latLng);
            latLngBuilder.include(latLng);
            locationCursor.moveToNext();
        }

        // add the polyline to the map
        googleMap.addPolyline(line);


        // make the map zoom to show the track, with some padding
        // use the size of the current display in pixels as a bounding box
        Display display = getActivity().getWindowManager().getDefaultDisplay();

        // construct a movement instruction for the map camera
        LatLngBounds latLngBounds = latLngBuilder.build();
        CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBounds,
                display.getWidth(), display.getHeight(), 15);
        googleMap.moveCamera(movement);
    }
}
