package com.bignerdranch.android.runtracker;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class RunListFragment extends ListFragment {
    private static final int REQUEST_NEW_RUN = 0;

    private RunDatabaseHelper.RunCursor cursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cursor = RunManager.get(getActivity()).queryRuns();

        RunCursorAdapter adapter = new RunCursorAdapter(getActivity(), cursor);
        setListAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        cursor.close();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.run_list_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new_run:
                Intent intent = new Intent(getActivity(), RunActivity.class);
                startActivityForResult(intent, REQUEST_NEW_RUN);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_NEW_RUN == requestCode) {
            cursor.requery();
            ((RunCursorAdapter) getListAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), RunActivity.class);
        intent.putExtra(RunActivity.EXTRA_RUN_ID, id);
        startActivity(intent);
    }

    private static class RunCursorAdapter extends CursorAdapter {
        private RunDatabaseHelper.RunCursor runCursor;

        public RunCursorAdapter(Context context, RunDatabaseHelper.RunCursor cursor) {
            super(context, cursor, 0);
            runCursor = cursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Run run = runCursor.getRun();

            TextView startDateTextView = (TextView) view;
            String cellText = context.getString(R.string.cell_text, run.getStartDate());
            startDateTextView.setText(cellText);
        }
    }
}
