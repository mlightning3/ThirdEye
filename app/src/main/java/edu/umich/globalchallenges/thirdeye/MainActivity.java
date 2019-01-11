package edu.umich.globalchallenges.thirdeye;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * This is the first part of the app to be loaded. It is responsible for drawing the navigation drawer
 * as well as updating the fragment that is shown. The fragments replace the frame layout in the
 * resource file.
 */
public class MainActivity extends AppCompatActivity {
    // Important Globals
    private DrawerLayout drawerLayout;
    private FragmentManager fragmentManager;
    private Fragment activeFragment;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if(key.contentEquals("filename")) { // Convert all spaces to underscore whenever we get a new filename
                        String filename = prefs.getString("filename", "");
                        filename = filename.replaceAll(" ", "_");
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                        editor.putString("filename", filename);
                        editor.apply();
                    }
                }
            };
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
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
                            case R.id.view_logs :
                                newFragment = getSupportFragmentManager().findFragmentById(R.id.LogContent);
                                if(newFragment == null) {
                                    newFragment = new LogViewerFragment();
                                }
                                break;
                            default : break;
                        }
                        if(newFragment != null) {
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
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
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
}