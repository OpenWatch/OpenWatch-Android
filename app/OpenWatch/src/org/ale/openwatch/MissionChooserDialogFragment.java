package org.ale.openwatch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockDialogFragment;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.contentprovider.OWContentProvider;

/**
 * Created by davidbrodsky on 8/8/13.
 */
public class MissionChooserDialogFragment extends SherlockDialogFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = "MissionChooserDialogFragment";

    OWMissionAdapter mAdapter;
    Dialog dialog;
    ListView listView;


    private void restartLoader(){
        this.getLoaderManager().restartLoader(0, null, this);
    }

    public String[] PROJECTION = {
            DBConstants.ID,
            DBConstants.EXPIRES,
            DBConstants.RECORDINGS_TABLE_THUMB_URL,
            DBConstants.RECORDINGS_TABLE_TITLE,
            DBConstants.MEDIA_OBJECT_MISSION
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Uri baseUri = OWContentProvider.getMissionUri();
        String selection = null;
        String[] selectionArgs = null;
        String order = "expires ASC";
        return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = getActivity().getLayoutInflater().inflate(R.layout.list_view, null);
        listView = (ListView) v.findViewById(R.id.list_view);
        mAdapter = new OWMissionAdapter(getActivity(), null);
        getLoaderManager().initLoader(0, null, this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int model_id = (Integer) view.getTag(R.id.list_item_model);
                ((WhatHappenedActivity) getActivity()).onMissionSelected(model_id);
                dialog.dismiss();
            }
        });

        dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setIcon(R.drawable.mission)
                .setTitle(getString(R.string.choose_mission))
                .setNegativeButton(R.string.no_mission,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((WhatHappenedActivity) getActivity()).onMissionSelected(0);
                                dialog.dismiss();
                            }
                        }
                )
                .create();
        return dialog;
    }
}
