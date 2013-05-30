package org.ale.openwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TabHost;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;

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


/**
     * Created by davidbrodsky on 5/21/13.
     */
    public class OnBoardingActivity extends SherlockFragmentActivity {
        private static final String TAG = "OnBoardingActivity";
        private static final int[] CONTENT = new int[] { R.layout.on_boarding_1, R.layout.on_boarding_2, R.layout.on_boarding_3, R.layout.on_boarding_4};

        ViewPager mPager;
        PageIndicator mIndicator;

        FeedFragmentActivity.TabsAdapter mTabsAdapter;
        TabHost mTabHost;
        CirclePageIndicator mCircleIndicator;

        LayoutInflater inflater;

        boolean didLogin = false;
        boolean agent_applicant = false;

        Fragment agentFragment;
        Fragment finalFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.on_boarding);

        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new FeedFragmentActivity.TabsAdapter(this, mTabHost, mPager);
        mCircleIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        mCircleIndicator.setViewPager(mPager);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        populateTabs();
    }

    private void populateTabs(){
        Bundle fragBundle;
        for(int layout : CONTENT){
            fragBundle = new Bundle(1);
            fragBundle.putInt("layout-id", layout);
            mTabsAdapter.addTab(mTabHost.newTabSpec("").setIndicator(""), OnBoardingFragment.class, fragBundle);
        }
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