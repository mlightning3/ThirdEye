package edu.umich.globalchallenges.thirdeye.fragment;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import edu.umich.globalchallenges.thirdeye.MainActivity;
import edu.umich.globalchallenges.thirdeye.R;

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
                    if(getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).addFragmentToBackStack(new AboutFragment());
                    }
                    return true;
                }
            });
        }
        Preference debugLogs = findPreference("debug_logs");
        if (debugLogs != null) {
            debugLogs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(getActivity() instanceof  MainActivity) {
                        ((MainActivity) getActivity()).addFragmentToBackStack(new LogViewerFragment());
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
