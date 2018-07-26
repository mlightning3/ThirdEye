package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {
    // Important Globals
    public static int last_net_id = 0;                // Network previously attached to
    private static String ssid;
    private static String sharedkey;
    //private static String ssid = "\"MD-02\"";          // SSID of network with camera stream
    //private static String sharedkey = "\"WoN1ukARBG81EWI\"";  // Password to above network
    private static String userkey; // = "QtM0lly02RH18";  // A preshared key that allows for elevated privileges on server

    private SharedPreferences sharedPreferences;
    private RequestQueue queue;
    private WifiManager wifiManager;
    private DrawerLayout drawerLayout;
    private FragmentManager fragmentManager;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        item.setChecked(true);
                        drawerLayout.closeDrawers();
                        return true;
                    }
                }
        );

        // Initialize our global variables
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        queue = Volley.newRequestQueue(this);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load settings
        ssid = "\"" + sharedPreferences.getString("ssid", "Pi_AP") + "\"";
        sharedkey = "\"" + sharedPreferences.getString("passphrase", "raspberry") + "\"";
        userkey = sharedPreferences.getString("userkey", "developmentkey");

        // Load initial fragment
        fragmentManager = getSupportFragmentManager();
        activeFragment = new DeviceControlFragment();
        fragmentManager.beginTransaction().add(R.id.fragment_container, activeFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        errorbar.setAction("Connect", new MainActivity.ConnectListener());
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
     * Listens for a reboot action and tries to reboot the server
     */
    public class RebootListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
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
                            snack_message(view, "Error rebooting Pi");
                        }
                    }
            );
            queue.add(stringRequest);
        }
    }

    /**
     * Listens for a shutdown action and tries to shutdown the server
     */
    public class ShutdownListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
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
                            snack_message(view, "Error shutting down Pi");
                        }
                    }
            );
            queue.add(stringRequest);
        }
    }

    /**
     * Tells the computer running the server to reboot. It will display a snackbar with the status
     * of the request (everything ok, or error)
     * @param view
     */
    public void device_reboot(final View view) {
        if (connected_to_network()) {
            Snackbar rebootbar = Snackbar.make(view, "Reboot Pi?", Snackbar.LENGTH_LONG);
            rebootbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            TextView messagetext = (TextView) rebootbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            messagetext.setTextColor(Color.BLACK);
            rebootbar.setAction("Yes", new MainActivity.RebootListener());
            rebootbar.show();
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
            Snackbar shutdownbar = Snackbar.make(view, "Shutdown Pi?", Snackbar.LENGTH_LONG);
            shutdownbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            TextView messagetext = (TextView) shutdownbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            messagetext.setTextColor(Color.BLACK);
            shutdownbar.setAction("Yes", new MainActivity.ShutdownListener());
            shutdownbar.show();
        }
        else {
            network_error_snack(view);
        }
    }
}