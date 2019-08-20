package edu.umich.globalchallenges.thirdeye.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import edu.umich.globalchallenges.thirdeye.R;

/**
 * A dialog that allows the user to change the controls that are visible when viewing the stream.
 * This will also make a callback to the activity/fragment that makes this dialog, so it can update
 * itself as needed.
 */
public class ChangeVideoSettingsDialog extends DialogFragment {

    private SharedPreferences sharedPreferences;
    private int NUMBEROFSETTINGS = 4;

    /**
     * Anything we need to do on the creation of the dialog
     * @param savedInstanceState
     * @return
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ArrayList selectedItems = new ArrayList();
        final ArrayList unselectedItems = new ArrayList();
        for(int i = 0; i < NUMBEROFSETTINGS; i++) { // Build arrays for the items from settings
            if(getChecked()[i] == true) {
                selectedItems.add(i);
            } else {
                unselectedItems.add(i);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.video_settings);
        builder.setMultiChoiceItems(buildArray(), getChecked(), new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if(isChecked) {
                    selectedItems.add(which);
                    unselectedItems.remove(Integer.valueOf(which));
                } else if(selectedItems.contains(which)) {
                    unselectedItems.add(which);
                    selectedItems.remove(Integer.valueOf(which));
                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                for(int i = 0; i < selectedItems.size(); i++) {
                    switch ((int) selectedItems.get(i)) { // Populate settings with all things checked
                        case 0:
                            editor.putBoolean("media_saving", true);
                            break;
                        case 1:
                            editor.putBoolean("image_manip", true);
                            break;
                        case 2:
                            editor.putBoolean("focus_control", true);
                            break;
                        case 3:
                            editor.putBoolean("light_control", true);
                            break;
                        default:
                            break;
                    }
                }
                for(int i = 0; i < unselectedItems.size(); i++) {
                    switch ((int) unselectedItems.get(i)) { // Populate settings with all things unchecked
                        case 0:
                            editor.putBoolean("media_saving", false);
                            break;
                        case 1:
                            editor.putBoolean("image_manip", false);
                            break;
                        case 2:
                            editor.putBoolean("focus_control", false);
                            break;
                        case 3:
                            editor.putBoolean("light_control", false);
                        default:
                            break;
                    }
                }
                editor.apply();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                dismiss();
            }
        });
        return builder.create();
    }

    /**
     * Builds array for populating our dialog box
     *
     * @return Array of Strings
     */
    private String[] buildArray() {
        ArrayList<String> settings = new ArrayList<>();
        settings.add("Media Saving");
        settings.add("Image Manipulation");
        settings.add("Focus Control");
        settings.add("Light Control");
        String[] strings = new String[settings.size()];
        for(int i = 0; i < settings.size(); i++) {
            strings[i] = settings.get(i);
        }
        return strings;
    }

    /**
     * Builds array for setting the check boxes in dialog box
     *
     * @return Array of booleans
     */
    private boolean[] getChecked() {
        boolean[] bools = new boolean[NUMBEROFSETTINGS];
        bools[0] = sharedPreferences.getBoolean("media_saving", true);
        bools[1] = sharedPreferences.getBoolean("image_manip", true);
        bools[2] = sharedPreferences.getBoolean("focus_control", false);
        bools[3] = sharedPreferences.getBoolean("light_control", true);
        return bools;
    }
}
