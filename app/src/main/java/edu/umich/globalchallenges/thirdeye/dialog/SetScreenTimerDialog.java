package edu.umich.globalchallenges.thirdeye.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import edu.umich.globalchallenges.thirdeye.R;

/**
 * A dialog that allows the user to set the amount of time they want the screen to be kept on before
 * it can turn itself off. Has two options, either the user enters an amount or disables the timeout
 */
public class SetScreenTimerDialog extends DialogFragment {

    private String screen_timeout_value;

    public SetScreenTimerDialog(Context context) {
        screen_timeout_value = context.getString(R.string.key_screen_timeout_value);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.set_timer_dialog, null);

        RadioButton allowTimeout = (RadioButton) view.findViewById(R.id.allow_timeout);
        final RadioButton neverTimeout = (RadioButton) view.findViewById(R.id.never_timeout);
        final EditText timeoutText = (EditText) view.findViewById(R.id.screen_timeout);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final int timeout = sharedPreferences.getInt(screen_timeout_value, 2);

        if(timeout > 0) {
            timeoutText.setText(Integer.toString(timeout));
            allowTimeout.toggle();
        } else {
            timeoutText.setText("2");
            neverTimeout.toggle();
        }

        return builder
                .setTitle(getString(R.string.screen_timeout_title))
                .setView(view)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                        if(neverTimeout.isChecked()) {
                            editor.putInt(screen_timeout_value, 0);
                        } else {
                            int timeout = Integer.valueOf(timeoutText.getText().toString());
                            editor.putInt(screen_timeout_value, timeout);
                        }
                        editor.apply();
                        if(getTargetFragment() != null) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(getTargetFragment() != null) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                        }
                        dismiss();
                    }
                })
                .create();
    }
}
