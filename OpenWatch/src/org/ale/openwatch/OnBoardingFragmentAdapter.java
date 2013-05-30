package org.ale.openwatch;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by davidbrodsky on 5/21/13.
 */
public class OnBoardingFragmentAdapter extends FragmentPagerAdapter {
    protected static final int[] CONTENT = new int[] { R.layout.on_boarding_1, R.layout.on_boarding_2, R.layout.on_boarding_3, R.layout.on_boarding_4};

    private int mCount = CONTENT.length;

    public OnBoardingFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return OnBoardingFragment.newInstance(CONTENT[position % CONTENT.length]);
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    public void setCount(int count) {
        if (count > 0 && count <= 10) {
            mCount = count;
            notifyDataSetChanged();
        }
    }
}