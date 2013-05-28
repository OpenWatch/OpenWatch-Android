package net.openwatch.reporter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWUser;

/**
     * Created by davidbrodsky on 5/21/13.
     */
    public class OnBoardingActivity extends SherlockFragmentActivity {
        private static final String TAG = "OnBoardingActivity";

        OnBoardingFragmentAdapter mAdapter;
        ViewPager mPager;
        PageIndicator mIndicator;

        boolean didLogin = false;
        boolean agent_applicant = false;

        Fragment agentFragment;
        Fragment finalFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.on_boarding);

        mAdapter = new OnBoardingFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

        @Override
        protected void onResume(){
            super.onResume();
            if(this.getIntent().getBooleanExtra(Constants.AUTHENTICATED, false)){
                didLogin = true;
            }


        }

    public void onAttachFragment (Fragment fragment){
        if(((OnBoardingFragment)fragment).layout_resource_id == R.layout.on_boarding_4){
            finalFragment = fragment;
        }else if(((OnBoardingFragment)fragment).layout_resource_id == R.layout.on_boarding_3){
            agentFragment = fragment;
        }
    }

    public void onNavigationButtonClick(View v){
        // Sync preferences
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

    public void onAgentChecked(boolean isChecked){
        if(finalFragment == null){
            Log.e(TAG, "onAgentChecked. finalFragment is still null");
            return;
        }
        agent_applicant = isChecked;
        if(!isChecked){
            ((ImageView)finalFragment.getView().findViewById(R.id.on_boarding_4_image)).setImageDrawable(getResources().getDrawable(R.drawable.onbo_4a));
        }else{
            ((ImageView)finalFragment.getView().findViewById(R.id.on_boarding_4_image)).setImageDrawable(getResources().getDrawable(R.drawable.onbo_4));
        }
    }

}