package com.bignerdranch.android.runtracker;

import android.content.Context;
import android.location.Location;

public class LastLocationLoader extends DataLoader<Location> {
    private long runId;

    public LastLocationLoader(Context context, long runId) {
        super(context);
        this.runId = runId;
    }

    @Override
    public Location loadInBackground() {
        return RunManager.get(getContext()).getLastLocationForRun(runId);
    }
}
