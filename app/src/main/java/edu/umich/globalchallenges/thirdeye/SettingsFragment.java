package edu.umich.globalchallenges.thirdeye;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * This provides a way for the user to see and edit the settings of the app
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    /**
     * Builds our preference fragment from the information in the xml file
     *
     * @param savedInstanceState
     * @param rootKey
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.perf_general, rootKey);

        Preference sendFeedback = findPreference("about");
        if (sendFeedback != null) {
            sendFeedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(getActivity() instanceof  MainActivity) {
                        ((MainActivity) getActivity()).addFragmentToBackStack(new AboutFragment());
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof  MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.settings);
        }
    }
}
