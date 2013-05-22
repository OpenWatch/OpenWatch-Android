package net.openwatch.reporter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.openwatch.reporter.constants.Constants;

/**
 * Created by davidbrodsky on 5/21/13.
 */
public final class OnBoardingFragment extends Fragment {
    private static final String KEY_CONTENT = "OnBoardingFragment:Content";

    public static OnBoardingFragment newInstance(int layout_resource_id) {
        OnBoardingFragment fragment = new OnBoardingFragment();
        fragment.layout_resource_id = layout_resource_id;
        return fragment;
    }

    private int layout_resource_id = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
            layout_resource_id = savedInstanceState.getInt(KEY_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setGravity(Gravity.CENTER);

        if(layout_resource_id != -1){
            inflater = (LayoutInflater) this.getActivity().getSystemService(this.getActivity().LAYOUT_INFLATER_SERVICE);
            inflater.inflate(layout_resource_id, layout);
        }
        //layout.addView(text);

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CONTENT, layout_resource_id);
    }

}