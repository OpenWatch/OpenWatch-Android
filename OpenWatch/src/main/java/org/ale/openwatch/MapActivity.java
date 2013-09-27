package org.ale.openwatch;

import android.app.Activity;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.model.OWServerObject;

/**
 * Created by davidbrodsky on 6/20/13.
 */
public class MapActivity extends SherlockFragmentActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        int model_id = getIntent().getIntExtra(Constants.INTERNAL_DB_ID, 0);
        if(model_id != 0){
            OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            this.getSupportActionBar().setTitle(serverObject.title.get());
            ((MapFragment) this.getSupportFragmentManager().findFragmentById(R.id.mapFragment)).populateViews(model_id, getApplicationContext());
        }

    }
}