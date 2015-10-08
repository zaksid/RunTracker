package com.bignerdranch.android.runtracker;

import android.content.Context;

public class RunLoader extends DataLoader<Run> {
    private long runId;

    public RunLoader(Context context, long runId) {
        super(context);
        this.runId = runId;
    }

    @Override
    public Run loadInBackground() {
        return RunManager.get(getContext()).getRun(runId);
    }
}
