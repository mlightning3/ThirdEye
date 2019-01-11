package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ExternalSensorFragment extends Fragment {
    // Important Globals
    private static String ssid;
    private static String sharedkey;

    private int timeoutDuration = 1000;

    private SharedPreferences sharedPreferences;
    private WifiManager wifiManager;
    private RequestQueue queue;
    private CountDownTimer countDownTimer;
    private View view;
    private TextView heartrate;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        queue = Volley.newRequestQueue(getContext());
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load settings
        ssid = "\"" + sharedPreferences.getString("ssid", "Pi_AP") + "\"";
        sharedkey = "\"" + sharedPreferences.getString("passphrase", "raspberry") + "\"";

        // Start countdown timer
        resetTimer();
    }

    /**
     * Adds option buttons to actionbar
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
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
        view = inflater.inflate(R.layout.external_sensor_fragment, container, false);
        wifi_connect();
        heartrate = (TextView) view.findViewById(R.id.heartrate);
        return view;
    }

    /**
     * Preforms actions when things in actionbar are clicked
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                wifi_connect();
                updateHeartRate();
                // TODO do the refresh thing
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateHeartRate() {
        String url = "http://stream.pi:5000/get_controller_data";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String bpm = response + "bpm";
                        heartrate.setText(bpm);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse response = error.networkResponse;
                        if(response != null && response.data != null) {
                            switch(response.statusCode) {
                                case 401:
                                    snack_message(view, "Invalid User Key");
                                    break;
                                case 403:
                                    snack_message(view, "Server does not support external sensors");
                                    break;
                                case 500:
                                    snack_message(view, "Server unable to get sensor data");
                                    break;
                                default:
                                    snack_message(view, "Unknown error while getting sensor data");
                                    break;
                            }
                        } else {
                            snack_message(view, "Unknown error while changing brightness");
                        }
                    }
                }
        );
        queue.add(stringRequest);
        resetTimer();
    }

    // Repeated functions from other fragments

    /**
     * Resets the timer to make all the controls disappear
     */
    public void resetTimer() {
        countDownTimer = new CountDownTimer(timeoutDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // Nothing needed
            }

            public void onFinish() {
                updateHeartRate();
            }
        }.start();
    }

    /**
     * Displays a snackbar with a message
     *
     * @param view The view from which we need to send this message
     * @param message The message we want displayed
     */
    private void snack_message(View view, String message) {
        Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        messagebar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        TextView messagetext = (TextView) messagebar.getView().findViewById(android.support.design.R.id.snackbar_text);
        messagetext.setTextColor(Color.WHITE);
        messagebar.show();
    }

    /**
     * Creates a new network connection for the camera streaming computer, and connects to that
     * network. This will also save off the network that we are attached to before changing networks
     * so that we can try to reconnect to it when we are done.
     */
    private void wifi_connect() {
        if(!connected_to_network()) { // Only connect if we aren't already
            // setup a wifi configuration
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = ssid;
            wc.preSharedKey = sharedkey;
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            // connect to and enable the connection
            int netId = wifiManager.addNetwork(wc);
            wifiManager.enableNetwork(netId, true);
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * Tests if we are connected to the right network to view the camera stream.
     *
     * @return true if connected to the right network, false otherwise
     */
    private boolean connected_to_network() {
        if(wifiManager.getConnectionInfo().getSSID().equals(ssid)) {
            return true;
        }
        return false;
    }
}
