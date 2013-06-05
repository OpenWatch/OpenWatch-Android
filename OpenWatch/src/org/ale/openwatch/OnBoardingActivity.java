package org.ale.openwatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;


/**
     * Created by davidbrodsky on 5/21/13.
     */
    public class OnBoardingActivity extends SherlockFragmentActivity implements ViewSwitcher.ViewFactory {
        private static final String TAG = "OnBoardingActivity";

        ImageSwitcher imageSwitcher;
        ToggleButton agentToggle;
        Button continueButton;
        boolean didLogin = false;
        boolean agent_applicant = false;

        int state = 0;
        private static final int[] drawale_states = new int[] {R.drawable.onbo_1, R.drawable.onbo_2, R.drawable.onbo_3, R.drawable.onbo_4agent};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.on_boarding);
        imageSwitcher = (ImageSwitcher) findViewById(R.id.image_switcher);
        imageSwitcher.setFactory(this);
        imageSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        imageSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
        imageSwitcher.setImageResource(drawale_states[state]);

        agentToggle = (ToggleButton) findViewById(R.id.agent_toggle);
        agentToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                agent_applicant = isChecked;
            }
        });
        continueButton = (Button) findViewById(R.id.button_continue);
    }


    @Override
    protected void onResume(){
        super.onResume();
        if(this.getIntent().getBooleanExtra(Constants.AUTHENTICATED, false)){
            didLogin = true;
        }
    }

    public void onNavigationButtonClick(View v){
        state ++;
        if(state <= 3){
            // Progress through onBoarding
            imageSwitcher.setImageResource(drawale_states[state]);
            if(state == 3){
                // Show last on boarding screen depending on agent selection
                continueButton.setText(getString(R.string.get_started_button_text));
                if(agent_applicant)
                    imageSwitcher.setImageResource(R.drawable.onbo_4agent);
                else
                    imageSwitcher.setImageResource(R.drawable.onbo_4);
            }
            if(state == 2){
                agentToggle.setVisibility(View.VISIBLE);
            }else{
                agentToggle.setVisibility(View.INVISIBLE);
            }
        }
        else{
            // Sync preferences and go to MainActivity
            if(OWApplication.user_data != null && OWApplication.user_data.containsKey(Constants.INTERNAL_USER_ID)){
                OWUser user = OWUser.objects(getApplicationContext(), OWUser.class).get((Integer)OWApplication.user_data.get(Constants.INTERNAL_USER_ID));
                user.agent_applicant.set(agent_applicant);
                user.save(getApplicationContext());
                OWServiceRequests.syncOWUser(getApplicationContext(), user);
            }
            Intent i = new Intent(OnBoardingActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // It's possible the sharedPreference setting won't be written by the
            // time MainActivity
            // checks its state, causing an erroneous redirect back to LoginActivity
            if (didLogin)
                i.putExtra(Constants.AUTHENTICATED, true);

            startActivity(i);
        }
    }


    @Override
    public View makeView() {
        ImageView iView = new ImageView(this);
        iView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iView.setLayoutParams(new
                ImageSwitcher.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
        return iView;
    }
}