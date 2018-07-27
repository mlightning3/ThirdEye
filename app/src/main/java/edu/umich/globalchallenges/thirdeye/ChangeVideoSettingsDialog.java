package edu.umich.globalchallenges.thirdeye;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

public class ChangeVideoSettingsDialog extends DialogFragment {

    private SharedPreferences sharedPreferences;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ArrayList selectedItems = new ArrayList();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Video Settings");
        builder.setMultiChoiceItems(buildArray(), getChecked(), new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if(isChecked) {
                    selectedItems.add(which);
                } else if(selectedItems.contains(which)) {
                    selectedItems.remove(Integer.valueOf(which));
                }
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                for(int i = 0; i < selectedItems.size(); i++) {
                    switch ((int) selectedItems.get(i)) {
                        case 0:
                            editor.putBoolean("media_saving", true);
                            break;
                        case 1:
                            editor.putBoolean("image_manip", true);
                            break;
                        case 2:
                            editor.putBoolean("focus_control", true);
                            break;
                        default:
                            break;
                    }
                }
                editor.apply();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
        boolean[] bools = new boolean[3];
        bools[0] = sharedPreferences.getBoolean("media_saving", true);
        bools[1] = sharedPreferences.getBoolean("image_manip", true);
        bools[2] = sharedPreferences.getBoolean("focus_control", false);
        return bools;
    }
}
