package edu.umich.globalchallenges.thirdeye;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

public class AboutFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about_fragment, container, false);
        TextView buildNumber = (TextView) view.findViewById(R.id.text_build_number);
        buildNumber.setText(BuildConfig.VERSION_NAME);
        TextView buildDateText = (TextView) view.findViewById(R.id.text_build_date);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        buildDateText.setText(buildDate.toString());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof  MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.about_title);
        }
    }
}
