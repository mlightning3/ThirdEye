package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;

public class MainActivity extends AppCompatActivity {
    // Important Globals
    public static int last_net_id = 0;                // Network previously attached to
    private static String ssid = "\"MD-02\"";          // SSID of network with camera stream
    private static String sharedkey = "\"WoN1ukARBG81EWI\"";  // Password to above network
    private static String userkey = "QtM0lly02RH18";  // A preshared key that allows for elevated privileges on server

    private static final int UM_BLUE = 0xFF00274C;
    private static final int UM_MAZE = 0xFFFFCB05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Creates a new network connection for the camera streaming computer, and connects to that
     * network. This will also save off the network that we are attached to before changing networks
     * so that we can try to reconnect to it when we are done.
     *
     * @param view
     */
    public void wifi_connect(View view) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

    /**
     * Disconnects the phone from the camera streaming computer's network and tries to connect to the
     * network used before. Otherwise it will just leave you unconnected till either the user picks
     * a new network to attach to, or the OS connects to a network.
     *
     * @param view
     */
    public void wifi_disconnect(View view) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.disconnect();
        if(last_net_id != 0) { // Try to reconnect to the network previously attached to
            wifiManager.enableNetwork(last_net_id, true);
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * Tests if we are connected to the right network to view the camera stream.
     *
     * @return true if connected to the right network, false otherwise
     */
    public boolean connected_to_network() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.getConnectionInfo().getSSID().equals(ssid)) {
            return true;
        }
        return false;
    }

    /**
     * Launches activity to view the camera stream if we are connected to the right network, or gives
     * a snackbar message that we aren't connected.
     * @param view
     */
    public void browser_launch(View view) {
        if (connected_to_network()) {
            Intent intent = new Intent(this, DisplayStream.class);
            startActivity(intent);
        }
        else {
            String message = "Not connected to correct network";
            Snackbar errorbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            errorbar.getView().setBackgroundColor(UM_BLUE);
            TextView errortext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            errortext.setTextColor(Color.WHITE);
            errorbar.show();
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
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            String message = "Pi rebooting";
                            Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
                            messagebar.getView().setBackgroundColor(UM_BLUE);
                            TextView messagetext = (TextView) messagebar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            messagetext.setTextColor(Color.WHITE);
                            messagebar.show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String message = "Error rebooting Pi";
                            Snackbar errorbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                            errorbar.getView().setBackgroundColor(UM_BLUE);
                            TextView errortext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            errortext.setTextColor(Color.WHITE);
                            errorbar.show();
                        }
                    }
            );
            queue.add(stringRequest);
        }
        else {
            String message = "Not connected to correct network";
            Snackbar errorbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            errorbar.getView().setBackgroundColor(UM_BLUE);
            TextView errortext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            errortext.setTextColor(Color.WHITE);
            errorbar.show();
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
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            String message = "Pi shutting down";
                            Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
                            messagebar.getView().setBackgroundColor(UM_BLUE);
                            TextView messagetext = (TextView) messagebar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            messagetext.setTextColor(Color.WHITE);
                            messagebar.show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String message = "Error shutting down Pi";
                            Snackbar errorbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                            errorbar.getView().setBackgroundColor(UM_BLUE);
                            TextView errortext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            errortext.setTextColor(Color.WHITE);
                            errorbar.show();
                        }
                    }
            );
            queue.add(stringRequest);
        }
        else {
            String message = "Not connected to correct network";
            Snackbar errorbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            errorbar.getView().setBackgroundColor(UM_BLUE);
            TextView errortext = (TextView) errorbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            errortext.setTextColor(Color.WHITE);
            errorbar.show();
        }
    }
}