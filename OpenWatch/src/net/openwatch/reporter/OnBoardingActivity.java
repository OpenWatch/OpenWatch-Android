package net.openwatch.reporter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;
import net.openwatch.reporter.constants.Constants;

/**
     * Created by davidbrodsky on 5/21/13.
     */
    public class OnBoardingActivity extends SherlockFragmentActivity {
        private static final String TAG = "OnBoardingActivity";

        OnBoardingFragmentAdapter mAdapter;
        ViewPager mPager;
        PageIndicator mIndicator;

        public CompoundButton agentToggle;

        boolean didLogin = false;

        Fragment agentFragment;
        Fragment finalFragment;
        ImageView finalOnBoardingView;

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
        if(!isChecked){
            ((ImageView)finalFragment.getView().findViewById(R.id.on_boarding_4_image)).setImageDrawable(getResources().getDrawable(R.drawable.onbo_4a));
        }else{
            ((ImageView)finalFragment.getView().findViewById(R.id.on_boarding_4_image)).setImageDrawable(getResources().getDrawable(R.drawable.onbo_4));
        }
    }

}