package edu.umich.globalchallenges.thirdeye;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import edu.umich.globalchallenges.thirdeye.fragment.DeviceControlFragment;
import edu.umich.globalchallenges.thirdeye.fragment.DisplayStreamFragment;
import edu.umich.globalchallenges.thirdeye.fragment.ExternalSensorFragment;
import edu.umich.globalchallenges.thirdeye.fragment.FileViewerFragment;
import edu.umich.globalchallenges.thirdeye.fragment.SettingsFragment;

/**
 * This is the first part of the app to be loaded. It is responsible for drawing the navigation drawer
 * as well as updating the fragment that is shown. The fragments replace the frame layout in the
 * resource file.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
                                                        SharedPreferences.OnSharedPreferenceChangeListener {

    // Permission values (they are just any number, as long as we know what we are looking for later)
    private static final int FINE_LOCATION_PERMISSION = 338;
    private static final int EXTERNAL_WRITE_PERMISSION = 509;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load settings
        ssid = "\"" + sharedPreferences.getString(getString(R.string.key_ssid), getString(R.string.default_ssid)) + "\"";
        sharedkey = "\"" + sharedPreferences.getString(getString(R.string.key_passphrase), getString(R.string.default_passphrase)) + "\"";


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
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager = getSupportFragmentManager();
        if(savedInstanceState == null) {
            // Load initial fragment
            activeFragment = new DeviceControlFragment();
            fragmentManager.beginTransaction().add(R.id.fragment_container, activeFragment).commit();
        }

        // Request permissions the app will need
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_WRITE_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
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
     * When something on the DrawerMenu gets selected, this decides what happens
     * @param item Item from DrawerMenu
     * @return true, always
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        drawerLayout.closeDrawers();

        // Swap out Fragment
        Fragment newFragment = null;
        switch (item.getItemId()) {
            case R.id.view_stream :
                newFragment = getSupportFragmentManager().findFragmentById(R.id.display_frame);
                if (newFragment == null) {
                    newFragment = new DisplayStreamFragment();
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

    /**
     * Whenever a preference for the app changes, this gets called
     * @param sharedPreferences The app's preference object
     * @param key The preference that changed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "filename" :
                cleanFilename();
                break;
            default:
                break;
        }
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
     * When we ask for a permission, the result comes back here
     * @param requestCode The code we gave when asking for permission
     * @param permissions Names of all permissions
     * @param grantResults Results of each permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION :
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO : Decide what I want to do with this
                }
                break;
            case EXTERNAL_WRITE_PERMISSION :
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO : Decide what I want to do with this
                }
                break;
            default: break;
        }
    }

    /*
     * Anything that isn't an overload lives below here
     *
     */

    /**
     * Creates a new network connection for the camera streaming computer, and connects to that
     * network. This will also save off the network that we are attached to before changing networks
     * so that we can try to reconnect to it when we are done.
     */
    public boolean connected_to_network() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION);
        }
        return wifiManager.getConnectionInfo().getSSID().contentEquals(ssid);
    }

    /**
     * Tests if we are connected to the right network to view the camera stream.
     */
    public void wifi_connect() {
        if(!connected_to_network()) { // Only connect if we aren't already
            last_net_id = wifiManager.getConnectionInfo().getNetworkId();
            // setup a wifi configuration
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = ssid;
            wc.preSharedKey = sharedkey;
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            // connect to and enable the connection
            int netId = wifiManager.addNetwork(wc);
            if(netId == -1) { // Find the network id if it already exists
                List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
                if(networks != null) {
                    for(WifiConfiguration existing : networks) {
                        if(existing.SSID.contentEquals(ssid)) {
                            netId = existing.networkId;
                        }
                    }
                }
            }
            wifiManager.setWifiEnabled(true);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        }
    }

    /**
     * Disconnects the phone from the camera streaming computer's network and tries to connect to the
     * network used before. Otherwise it will just leave you unconnected till either the user picks
     * a new network to attach to, or the OS connects to a network.
     */
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
    public void snack_message(String message) {
        View view = findViewById(R.id.fragment_container);
        if(view != null) {
            Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            messagebar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            TextView messagetext = (TextView) messagebar.getView().findViewById(R.id.snackbar_text);
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
    public void snack_message(int string_id) {
        snack_message(getString(string_id));
    }

    /**
     * Displays a toast with a message
     *
     * @param message The message we want displayed
     */
    public void toast_message(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a toast with a message
     *
     * @param string_id The message we want displayed from string resource
     */
    public void toast_message(int string_id) {
        toast_message(getString(string_id));
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

    /**
     * Cleans whatever filename the user wants to use for saving images and video
     * Currently only needs to strip out whitespace
     */
    private void cleanFilename() {
        String filename = sharedPreferences.getString(getString(R.string.key_filename), "");
        if(filename != null) {
            filename = filename.replaceAll(" ", "_");
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.key_filename), filename);
        editor.apply();
    }
}