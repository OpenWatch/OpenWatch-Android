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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().setTitle(getString(R.string.choose_mission));
        View v = inflater.inflate(R.layout.list_view, container, false);
        listView = (ListView) v.findViewById(R.id.list_view);
        mAdapter = new OWMissionAdapter(getActivity(), null);
        getLoaderManager().initLoader(0, null, this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int model_id = (Integer) view.getTag(R.id.list_item_model);

            }
        });

        ((Button) v.findViewById(R.id.noMissionButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return v;
    }


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
        String order = null;
        Log.i("URI-MISSIONS-DIALOG", "createLoader on uri: " + baseUri.toString());
        return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(TAG, "onLoadFinished");
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }
}
