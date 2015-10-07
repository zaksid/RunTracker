package com.bignerdranch.android.runtracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class RunManager {

    public static final String ACTION_LOCATION =
            "com.bignerdranch.android.runtracker.ACTION_LOCATION";

    private static final String TEST_PROVIDER = "TEST_PROVIDER";
    private static final String TAG = "RunManager";
    private static final String PREFERENCE_FILE = "runs";
    private static final String PREFERENCE_CURRENT_RUN_ID = "RunManager.currentRunId";

    private static RunManager runManager;
    private Context appContext;
    private LocationManager locationManager;
    private RunDatabaseHelper helper;
    private SharedPreferences preferences;
    private long currentRunId;

    private RunManager(Context context) {
        appContext = context;
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        helper = new RunDatabaseHelper(appContext);
        preferences = appContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        currentRunId = preferences.getLong(PREFERENCE_CURRENT_RUN_ID, -1);
    }

    public static RunManager get(Context context) {
        if (runManager == null) {
            runManager = new RunManager(context.getApplicationContext());
        }

        return runManager;
    }

    public Run startNewRun() {
        Run run = insertRun();
        startTrackingRun(run);
        return run;
    }

    public Run insertRun() {
        Run run = new Run();
        run.setId(helper.insertRun(run));
        return run;
    }

    public Run getRun(long id) {
        Run run = null;
        RunDatabaseHelper.RunCursor cursor = helper.queryRun(id);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            run = cursor.getRun();
        }
        cursor.close();
        return run;
    }

    public RunDatabaseHelper.RunCursor queryRuns() {
        return helper.queryRuns();
    }

    public Location getLastLocationForRun(long runId) {
        Location location = null;
        RunDatabaseHelper.LocationCursor cursor = helper.queryLastLocationForRun(runId);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            location = cursor.getLocation();
        }
        cursor.close();
        return location;
    }

    public void insertLocation(Location location) {
        if (currentRunId != -1) {
            helper.insertLocation(currentRunId, location);
        } else {
            Log.e(TAG, "Location received with no tracking run; ignoring.");
        }
    }

    public void startTrackingRun(Run run) {
        currentRunId = run.getId();
        preferences.edit().putLong(PREFERENCE_CURRENT_RUN_ID, currentRunId).commit();
        startLocationUpdates();
    }

    public void stopRun() {
        stopLocationUpdates();
        currentRunId = -1;
        preferences.edit().remove(PREFERENCE_CURRENT_RUN_ID).commit();
    }

    public boolean isTrackingRun() {
        return getLocationPendingIntent(false) != null;
    }

    public boolean isTrackingRun(Run run) {
        return run != null && run.getId() == currentRunId;
    }

    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
        return PendingIntent.getBroadcast(appContext, 0, broadcast, flags);
    }

    private void startLocationUpdates() {
        String provider = LocationManager.GPS_PROVIDER;

        // Если имеется поставщик тестовых данных и он активен,
        // использовать его.
        if (locationManager.getProvider(TEST_PROVIDER) != null &&
                locationManager.isProviderEnabled(TEST_PROVIDER)) {
            provider = TEST_PROVIDER;
        }
        Log.d(TAG, "Using provider " + provider);

        Location lastKnown = locationManager.getLastKnownLocation(provider);
        if (lastKnown != null) {
            lastKnown.setTime(System.currentTimeMillis());
            broadcastLocation(lastKnown);
        }

        PendingIntent pendingIntent = getLocationPendingIntent(true);
        locationManager.requestLocationUpdates(provider, 0, 0, pendingIntent);
    }

    private void broadcastLocation(Location location) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        appContext.sendBroadcast(broadcast);
    }

    private void stopLocationUpdates() {
        PendingIntent pendingIntent = getLocationPendingIntent(false);
        if (pendingIntent != null) {
            locationManager.removeUpdates(pendingIntent);
            pendingIntent.cancel();
        }
    }

}
