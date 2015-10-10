package com.bignerdranch.android.runtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.Date;

public class RunDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "runs.sqlite";
    private static final int VERSION = 1;

    private static final String TABLE_RUN = "run";
    private static final String COLUMN_RUN_ID = "id";
    private static final String COLUMN_RUN_START_DATE = "start_date";

    private static final String TABLE_LOCATION = "location";
    private static final String COLUMN_LOCATION_LATITUDE = "latitude";
    private static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
    private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_PROVIDER = "provider";
    private static final String COLUMN_LOCATION_RUN_ID = "run_id";

    public RunDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create table 'run'
        db.execSQL("create table run (" +
                "id integer primary key autoincrement, " +
                "start_date integer)");

        // Create table 'location'
        db.execSQL("create table location (" +
                "timestamp integer, " +
                "latitude  real," +
                "longitude real," +
                "altitude  real," +
                "provider  varchar(100)," +
                "run_id    integer references run(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long insertRun(Run run) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_RUN_START_DATE, run.getStartDate().getTime());
        return getWritableDatabase().insert(TABLE_RUN, null, values);
    }

    public long insertLocation(long runId, Location location) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOCATION_ALTITUDE, location.getAltitude());
        values.put(COLUMN_LOCATION_LATITUDE, location.getLatitude());
        values.put(COLUMN_LOCATION_LONGITUDE, location.getLongitude());
        values.put(COLUMN_LOCATION_TIMESTAMP, location.getTime());
        values.put(COLUMN_LOCATION_PROVIDER, location.getProvider());
        values.put(COLUMN_LOCATION_RUN_ID, runId);
        return getWritableDatabase().insert(TABLE_LOCATION, null, values);
    }

    public RunCursor queryRuns() {
        // equivalent to "select * from run order by start_date asc"
        Cursor wrapper = getReadableDatabase().query(TABLE_RUN,
                null, null, null, null, null, COLUMN_RUN_START_DATE + "asc");
        return new RunCursor(wrapper);
    }

    public RunCursor queryRun(long id) {
        Cursor wrapper = getReadableDatabase().query(TABLE_RUN,
                null, // all columns
                COLUMN_RUN_ID + " = ?",   // look for a run ID
                new String[]{String.valueOf(id)}, //with this value
                null,   // group by
                null,   // having
                null,   // order by
                "1");   // limit 1 row
        return new RunCursor(wrapper);
    }

    public LocationCursor queryLastLocationForRun(long runId) {
        Cursor wrapper = getReadableDatabase().query(TABLE_LOCATION,
                null, // all columns
                COLUMN_RUN_ID + " = ?",   // limit to the given run
                new String[]{String.valueOf(runId)},
                null,   // group by
                null,   // having
                COLUMN_LOCATION_TIMESTAMP + " desc",    // order by latest first
                "1");   // limit 1
        return new LocationCursor(wrapper);
    }

    public LocationCursor queryLocationsForRun(long runId) {
        Cursor wrapper = getReadableDatabase().query(TABLE_LOCATION,
                null,
                COLUMN_LOCATION_RUN_ID + " =? ",
                new String[]{String.valueOf(runId)},
                null,
                null,
                COLUMN_LOCATION_TIMESTAMP + "ASC");
        return new LocationCursor(wrapper);
    }

    /**
     * A convenience class to wrap a cursor that returns rows from table "run"
     * The {@link getRun()} method will give you a Run instance representing the current row.
     */
    public static class RunCursor extends CursorWrapper {

        /**
         * Creates a cursor wrapper.
         *
         * @param cursor The underlying cursor to wrap.
         */
        public RunCursor(Cursor cursor) {
            super(cursor);
        }


        /**
         * Returns a Run object configured for the current row,
         * or null if the current row is invalid.
         */
        public Run getRun() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }
            Run run = new Run();
            long runId = getLong(getColumnIndex(COLUMN_RUN_ID));
            run.setId(runId);

            long startDate = getLong(getColumnIndex(COLUMN_RUN_START_DATE));
            run.setStartDate(new Date(startDate));

            return run;
        }
    }

    public static class LocationCursor extends CursorWrapper {

        public LocationCursor(Cursor cursor) {
            super(cursor);
        }

        public Location getLocation() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }

            String provider = getString(getColumnIndex(COLUMN_LOCATION_PROVIDER));
            Location location = new Location(provider);
            location.setLongitude(getDouble(getColumnIndex(COLUMN_LOCATION_LONGITUDE)));
            location.setLatitude(getDouble(getColumnIndex(COLUMN_LOCATION_LATITUDE)));
            location.setAltitude(getDouble(getColumnIndex(COLUMN_LOCATION_ALTITUDE)));
            location.setTime(getLong(getColumnIndex(COLUMN_LOCATION_TIMESTAMP)));

            return location;
        }
    }

}
