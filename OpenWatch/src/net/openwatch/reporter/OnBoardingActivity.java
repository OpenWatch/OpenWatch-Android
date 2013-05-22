package net.openwatch.reporter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.CompoundButton;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;
import net.openwatch.reporter.constants.Constants;

/**
     * Created by davidbrodsky on 5/21/13.
     */
    public class OnBoardingActivity extends SherlockFragmentActivity {

        OnBoardingFragmentAdapter mAdapter;
        ViewPager mPager;
        PageIndicator mIndicator;

        CompoundButton agentToggle;

        boolean didLogin = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.on_boarding);

        mAdapter = new OnBoardingFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        //agentToggle = (findViewById(R.id.))
    }

        @Override
        protected void onResume(){
            super.onResume();
            if(this.getIntent().getBooleanExtra(Constants.AUTHENTICATED, false)){
                didLogin = true;
            }
        }

    public void onNavigationButtonClick(View v){
        Intent i = new Intent(OnBoardingActivity.this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (didLogin)
            i.putExtra(Constants.AUTHENTICATED, true);

        // It's possible the sharedPreference setting won't be written by the
        // time MainActivity
        // checks its state, causing an erroneous redirect back to LoginActivity
        startActivity(i);
    }

}