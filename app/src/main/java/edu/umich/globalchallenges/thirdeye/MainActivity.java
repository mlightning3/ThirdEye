package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the first part of the app to be loaded. It is responsible for drawing the navigation drawer
 * as well as updating the fragment that is shown. The fragments replace the frame layout in the
 * resource file.
 */
public class MainActivity extends AppCompatActivity implements FragmentWifiManager, FragmentCommManager {
    // Important Globals
    private static String ssid;
    private static String sharedkey;
    public static int last_net_id = 0; // Network previously attached to
    private WifiManager wifiManager;

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private FragmentManager fragmentManager;
    private Fragment activeFragment;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        listener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if(key.contentEquals("filename")) { // Convert all spaces to underscore whenever we get a new filename
                        String filename = prefs.getString("filename", "");
                        filename = filename.replaceAll(" ", "_");
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("filename", filename);
                        editor.apply();
                    }
                }
            };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load settings
        ssid = "\"" + sharedPreferences.getString("ssid", "Pi_AP") + "\"";
        sharedkey = "\"" + sharedPreferences.getString("passphrase", "raspberry") + "\"";


        // Set up layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        // Set up drawer icon changing
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerToggle.setToolbarNavigationClickListener(null);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        // Set up actions for selecting things in nav menu
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        item.setChecked(true);
                        drawerLayout.closeDrawers();

                        // Swap out Fragment
                        Fragment newFragment = null;
                        switch (item.getItemId()) {
                            case R.id.view_stream :
                                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://stream.pi:5000"));
                                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(browserIntent); // For KitKat based devices, just open a browser
                                    }
                                }
                                else {
                                    newFragment = getSupportFragmentManager().findFragmentById(R.id.display_frame);
                                    if (newFragment == null) {
                                        newFragment = new DisplayStreamFragment();
                                    }
                                }
                                break;
                            case R.id.view_files :
                                newFragment = getSupportFragmentManager().findFragmentById(R.id.file_view);
                                if(newFragment == null) {
                                    newFragment = new FileViewerFragment();
                                }
                                break;
                            case R.id.device_control :
                                newFragment = getSupportFragmentManager().findFragmentById(R.id.DeviceControlFragment);
                                if(newFragment == null) {
                                    newFragment = new DeviceControlFragment();
                                }
                                break;
                            case R.id.settings :
                                newFragment = getSupportFragmentManager().findFragmentById(R.id.settings);
                                if(newFragment == null) {
                                    newFragment = new SettingsFragment();
                                }
                                break;
                            case R.id.external_sensor :
                                newFragment = getSupportFragmentManager().findFragmentById(R.id.external_sensor);
                                if(newFragment == null) {
                                    newFragment = new ExternalSensorFragment();
                                }
                                break;
                            default : break;
                        }
                        if(newFragment != null) {
                            for(int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                                fragmentManager.popBackStack(); // Clear out anything in the backstack
                            }
                            fragmentManager.beginTransaction().replace(R.id.fragment_container, newFragment).commit();
                            activeFragment = newFragment;
                        }

                        return true;
                    }
                }
        );

        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            // Load initial fragment
            activeFragment = new DeviceControlFragment();
            fragmentManager.beginTransaction().add(R.id.fragment_container, activeFragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
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
     */
    @Override
    public boolean connected_to_network() {
        if(wifiManager.getConnectionInfo().getSSID().equals(ssid)) {
            return true;
        }
        return false;
    }

    /**
     * Tests if we are connected to the right network to view the camera stream.
     *
     * @return true if connected to the right network, false otherwise
     */
    @Override
    public void wifi_connect() {
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
     */
    @Override
    public void wifi_disconnect() {
        if(connected_to_network()) { // We only want to disconnect if we were connected to the server's network
            wifiManager.disconnect();
            if (last_net_id != 0) { // Try to reconnect to the network previously attached to
                wifiManager.enableNetwork(last_net_id, true);
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    /**
     * Displays a snackbar with a message
     *
     * @param message The message we want displayed
     */
    @Override
    public void snack_message(String message) {
        View view = getCurrentFocus();
        if(view != null) {
            Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            messagebar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            TextView messagetext = (TextView) messagebar.getView().findViewById(android.support.design.R.id.snackbar_text);
            messagetext.setTextColor(Color.WHITE);
            messagebar.show();
        } else {
            toast_message(message);
        }
    }

    /**
     * Displays a snackbar with a message
     *
     * @param string_id The message we want displayed from string resource
     */
    @Override
    public void snack_message(int string_id) {
        View view = getCurrentFocus();
        if(view != null) {
            Snackbar messagebar = Snackbar.make(view, string_id, Snackbar.LENGTH_LONG);
            messagebar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            TextView messagetext = (TextView) messagebar.getView().findViewById(android.support.design.R.id.snackbar_text);
            messagetext.setTextColor(Color.WHITE);
            messagebar.show();
        } else {
            toast_message(string_id);
        }
    }

    /**
     * Displays a toast with a message
     *
     * @param message The message we want displayed
     */
    @Override
    public void toast_message(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a toast with a message
     *
     * @param string_id The message we want displayed from string resource
     */
    @Override
    public void toast_message(int string_id) {
        Toast.makeText(this, string_id, Toast.LENGTH_SHORT).show();
    }

    /**
     * Change the hamburger menu out for a back arrow when in a sub-menu fragment
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        fragmentManager.popBackStack();
        fragmentManager.executePendingTransactions();
        if(fragmentManager.getBackStackEntryCount() == 0) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.setToolbarNavigationClickListener(null);
        }
    }

    /**
     * Change the title on the action bar of the app
     *
     * @param string_id String resource we want to display
     */
    public void setActionBarTitle(int string_id) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(string_id);
        }
    }

    /**
     * Show the given fragment, adding it to the back stack
     * @param fragment The fragment we want to show
     */
    public void addFragmentToBackStack(Fragment fragment) {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerToggle.setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(drawerToggle.getToolbarNavigationClickListener() == null) {
            drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
            drawerToggle.syncState();
        }

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        activeFragment = fragment;
    }
}