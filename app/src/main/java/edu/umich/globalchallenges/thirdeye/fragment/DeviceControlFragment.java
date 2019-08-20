package edu.umich.globalchallenges.thirdeye.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import edu.umich.globalchallenges.thirdeye.MainActivity;
import edu.umich.globalchallenges.thirdeye.R;
import edu.umich.globalchallenges.thirdeye.dialog.Dialogs;
import edu.umich.globalchallenges.thirdeye.dialog.RebootServerDialog;
import edu.umich.globalchallenges.thirdeye.dialog.ShutdownServerDialog;

/**
 * This fragment gives the user convenient buttons to control both their device as well as the server.
 * Buttons for connecting and disconnecting to the server's wifi network as well as buttons to power-off
 * and reboot the server live here.
 */
public class DeviceControlFragment extends Fragment implements View.OnClickListener{

    private static String userkey;     // A preshared key that allows for elevated privileges on server

    private MainActivity mainActivity;
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;
    private Button wifiConnect;
    private Button wifiDisconnect;
    private Button reboot;
    private Button shutdown;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        queue = Volley.newRequestQueue(getContext());

        // Load settings
        userkey = sharedPreferences.getString("userkey", "developmentkey");
    }

    /**
     * This is called when the fragment is creating it's view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_control_fragment, container, false);
        wifiConnect = (Button) view.findViewById(R.id.wifi_connect);
        wifiDisconnect = (Button) view.findViewById(R.id.wifi_disconnect);
        reboot = (Button) view.findViewById(R.id.reboot);
        shutdown = (Button) view.findViewById(R.id.poweroff);
        initializeListeners(view);
        return view;
    }

    /**
     * Here we set up all the listeners so that when buttons are clicked on they preform an action
     *
     * @param view The view these come from (ideally from onCreateView)
     */
    private void initializeListeners(View view) {
        wifiConnect.setOnClickListener(this);
        wifiDisconnect.setOnClickListener(this);
        reboot.setOnClickListener(this);
        shutdown.setOnClickListener(this);
    }

    /**
     * Anything that needs to be saved between use of fragments should be saved here
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.device_title);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " context is wrong (should be attached to MainActivity)");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    /**
     * Here we capture all the click events on the buttons and perform the action we need based on their id in the xml
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.wifi_connect : mainActivity.wifi_connect();
                                    break;
            case R.id.wifi_disconnect : mainActivity.wifi_disconnect();
                                    break;
            case R.id.poweroff :
                DialogFragment shutdownServerDialog = new ShutdownServerDialog();
                shutdownServerDialog.setTargetFragment(this, Dialogs.SHUTDOWNDIALOG);
                shutdownServerDialog.show(getFragmentManager(), "shutdownserverdialog");
                break;
            case R.id.reboot :
                DialogFragment rebootServerDialog = new RebootServerDialog();
                rebootServerDialog.setTargetFragment(this, Dialogs.REBOOTDIALOG);
                rebootServerDialog.show(getFragmentManager(), "rebootserverdialog");
                break;
            default: break;
        }
    }


    /**
     * Callback for after user uses the EmailLogsDialog
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Dialogs.SHUTDOWNDIALOG:
                device_shutdown(getView());
                break;
            case Dialogs.REBOOTDIALOG:
                device_reboot(getView());
                break;
        }
    }

    /**
     * Displays a snackbar saying we are not connected to the right network
     *
     * @param view The view from which we need to send this message
     */
    private void network_error_snack(View view) {
        Snackbar errorbar = Snackbar.make(view, R.string.incorrect_wifi, Snackbar.LENGTH_LONG);
        errorbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        TextView messagetext = (TextView) errorbar.getView().findViewById(R.id.snackbar_text);
        messagetext.setTextColor(Color.BLACK);
        errorbar.setAction(R.string.connect, new DeviceControlFragment.ConnectListener());
        errorbar.show();
    }

    /**
     * Listens for a connect action and tries to connect to the server's wifi network
     */
    public class ConnectListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            mainActivity.wifi_connect();
        }
    }

    /**
     * Tells the computer running the server to reboot. It will display a snackbar with the status
     * of the request (everything ok, or error)
     * @param view
     */
    private void device_reboot(final View view) {
        if (mainActivity != null && mainActivity.connected_to_network()) {
            String url = "http://stream.pi:5000/reboot?key=" + userkey;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.pi_rebooting);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.invalid_user_key);
                                        break;
                                    case 500:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.pi_unable_reboot);
                                        break;
                                    default:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.pi_error_reboot);
                                        break;
                                }
                            } else {
                                if(mainActivity != null)
                                    mainActivity.snack_message(R.string.pi_error_reboot);
                            }
                        }
                    }
            );
            queue.add(stringRequest);
        }
        else {
            network_error_snack(view);
        }
    }

    /**
     * Tells the computer running the server to shutdown. It will display a snackbar with the status
     * of the request (everything ok, or error)
     * @param view
     */
    private void device_shutdown(final View view) {
        if (mainActivity != null && mainActivity.connected_to_network()) {
            String url = "http://stream.pi:5000/shutdown?key=" + userkey;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.pi_shutdown);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.invalid_user_key);
                                        break;
                                    case 500:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.pi_unable_shutdown);
                                        break;
                                    default:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.pi_error_shutdown);
                                        break;
                                }
                            } else {
                                if(mainActivity != null)
                                    mainActivity.snack_message(R.string.pi_error_shutdown);
                            }
                        }
                    }
            );
            queue.add(stringRequest);
        }
        else {
            network_error_snack(view);
        }
    }
}
