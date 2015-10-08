package com.bignerdranch.android.runtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RunFragment extends Fragment {
    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_RUN = 0;
    private static final int LOAD_LOCATION = 1;

    private RunManager runManager;
    private Run run;
    private Location lastLocation;

    private Button startButton;
    private Button stopButton;
    private TextView startedTextView, latitudeTextView,
            longitudeTextView, altitudeTextView, durationTextView;

    private BroadcastReceiver locationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location location) {
            if (!runManager.isTrackingRun(run)) {
                return;
            }
            lastLocation = location;
            if (isVisible())
                updateUI();
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    };

    public static RunFragment newInstance(long runId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RunFragment runFragment = new RunFragment();
        runFragment.setArguments(args);
        return runFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        runManager = RunManager.get(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            if (runId != -1) {
                LoaderManager loaderManager = getLoaderManager();
                loaderManager.initLoader(LOAD_RUN, args, new RunLoaderCallbacks());
                loaderManager.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run, parent, false);

        startedTextView = (TextView) view.findViewById(R.id.run_startedTextView);
        latitudeTextView = (TextView) view.findViewById(R.id.run_latitudeTextView);
        longitudeTextView =
                (TextView) view.findViewById(R.id.run_longitudeTextView);
        altitudeTextView = (TextView) view.findViewById(R.id.run_altitudeTextView);
        durationTextView = (TextView) view.findViewById(R.id.run_durationTextView);

        startButton = (Button) view.findViewById(R.id.run_startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (run == null) {
                    run = runManager.startNewRun();
                } else {
                    runManager.startTrackingRun(run);
                }
                updateUI();
            }
        });

        stopButton = (Button) view.findViewById(R.id.run_stopButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runManager.stopRun();
                updateUI();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(locationReceiver,
                new IntentFilter(RunManager.ACTION_LOCATION));
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(locationReceiver);
        super.onStop();
    }

    private void updateUI() {
        boolean started = runManager.isTrackingRun();
        boolean trackingThisRun = runManager.isTrackingRun(run);

        if (run != null) {
            startedTextView.setText(run.getStartDate().toString());
        }

        int durationSeconds = 0;

        if (run != null && lastLocation != null) {
            durationSeconds = run.getDurationInSeconds(lastLocation.getTime());
            latitudeTextView.setText(Double.toString(lastLocation.getLatitude()));
            longitudeTextView.setText(Double.toString(lastLocation.getLongitude()));
            altitudeTextView.setText(Double.toString(lastLocation.getAltitude()));
        }

        durationTextView.setText(Run.formatDuration(durationSeconds));

        startButton.setEnabled(!started);
        stopButton.setEnabled(started && trackingThisRun);
    }

    private class RunLoaderCallbacks implements LoaderManager.LoaderCallbacks<Run> {

        @Override
        public Loader<Run> onCreateLoader(int id, Bundle args) {
            return new RunLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Run> loader, Run data) {
            run = data;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Run> loader) {
            // do nothing
        }
    }

    private class LocationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Location> {

        @Override
        public Loader<Location> onCreateLoader(int id, Bundle args) {
            return new LastLocationLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Location> loader, Location data) {
            lastLocation = data;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Location> loader) {
            // do nothing
        }
    }

}
