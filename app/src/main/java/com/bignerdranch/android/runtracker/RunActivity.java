package com.bignerdranch.android.runtracker;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class RunActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new RunFragment();
    }
}
