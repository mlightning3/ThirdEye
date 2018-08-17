package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class DeviceControlFragment extends Fragment implements View.OnClickListener{

    public static int last_net_id = 0; // Network previously attached to
    private static String ssid;        // Name of network with camera stream
    private static String sharedkey;   // Password to above network
    private static String userkey;     // A preshared key that allows for elevated privileges on server

    private SharedPreferences sharedPreferences;
    private RequestQueue queue;
    private WifiManager wifiManager;

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
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load settings
        ssid = "\"" + sharedPreferences.getString("ssid", "Pi_AP") + "\"";
        sharedkey = "\"" + sharedPreferences.getString("passphrase", "raspberry") + "\"";
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
        initializeButtons(view);
        return view;
    }

    /**
     * Here we set up all the buttons so that when they are clicked on they preform an action
     *
     * @param view The view these come from (ideally from onCreateView)
     */
    private void initializeButtons(View view) {
        Button wifiConnect = (Button) view.findViewById(R.id.wifi_connect);
        wifiConnect.setOnClickListener(this);
        Button wifiDisconnect = (Button) view.findViewById(R.id.wifi_disconnect);
        wifiDisconnect.setOnClickListener(this);
        Button reboot = (Button) view.findViewById(R.id.reboot);
        reboot.setOnClickListener(this);
        Button shutdown = (Button) view.findViewById(R.id.poweroff);
        shutdown.setOnClickListener(this);
    }

    /**
     * Anything that needs to be saved between use of fragments should be saved here
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Here we capture all the click events on the buttons and perform the action we need based on their id in the xml
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.wifi_connect : wifi_connect(view);
                                    break;
            case R.id.wifi_disconnect : wifi_disconnect(view);
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
     * Creates a new network connection for the camera streaming computer, and connects to that
     * network. This will also save off the network that we are attached to before changing networks
     * so that we can try to reconnect to it when we are done.
     *
     * @param view
     */
    public void wifi_connect(View view) {
        if(!connected_to_network()) { // Only connect if we aren't already
            last_net_id = wifiManager.getConnectionInfo().getNetworkId();
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
     * Disconnects the phone from the camera streaming computer's network and tries to connect to the
     * network used before. Otherwise it will just leave you unconnected till either the user picks
     * a new network to attach to, or the OS connects to a network.
     *
     * @param view
     */
    public void wifi_disconnect(View view) {
        if(connected_to_network()) { // We only want to disconnect if we were connected to the server's network
            wifiManager.disconnect();
            if (last_net_id != 0) { // Try to reconnect to the network previously attached to
                wifiManager.enableNetwork(last_net_id, true);
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    /**
     * Tests if we are connected to the right network to view the camera stream.
     *
     * @return true if connected to the right network, false otherwise
     */
    public boolean connected_to_network() {
        if(wifiManager.getConnectionInfo().getSSID().equals(ssid)) {
            return true;
        }
        return false;
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
     * Displays a snackbar saying we are not connected to the right network
     *
     * @param view The view from which we need to send this message
     */
    private void network_error_snack(View view) {
        Snackbar errorbar = Snackbar.make(view, "Not connected to correct network", Snackbar.LENGTH_LONG);
        errorbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        TextView messagetext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        messagetext.setTextColor(Color.BLACK);
        errorbar.setAction("Connect", new DeviceControlFragment.ConnectListener());
        errorbar.show();
    }

    /**
     * Listens for a connect action and tries to connect to the server's wifi network
     */
    public class ConnectListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            wifi_connect(view);
        }
    }

    /**
     * Tells the computer running the server to reboot. It will display a snackbar with the status
     * of the request (everything ok, or error)
     * @param view
     */
    public void device_reboot(final View view) {
        if (connected_to_network()) {
            String url = "http://stream.pi:5000/reboot?key=" + userkey;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            snack_message(view, "Pi rebooting");
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
                                    case 500:
                                        snack_message(view, "Server unable to reboot pi");
                                        break;
                                    default:
                                        snack_message(view, "Unknown error while rebooting pi");
                                        break;
                                }
                            } else {
                                snack_message(view, "Unknown error while rebooting pi");
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
    public void device_shutdown(final View view) {
        if (connected_to_network()) {
            String url = "http://stream.pi:5000/shutdown?key=" + userkey;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            snack_message(view, "Pi shutting down");
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
                                    case 500:
                                        snack_message(view, "Server unable to shutdown pi");
                                        break;
                                    default:
                                        snack_message(view, "Unknown error while shutting down pi");
                                        break;
                                }
                            } else {
                                snack_message(view, "Unknown error while shutting down pi");
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
