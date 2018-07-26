package edu.umich.globalchallenges.thirdeye;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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

public class MainActivity extends AppCompatActivity {
    // Important Globals
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
                                    newFragment = new DisplayStreamFragment();
                                }
                                break;
                            case R.id.view_files :
                                newFragment = new FileViewerFragment();
                                break;
                            case R.id.device_control :
                                newFragment = new DeviceControlFragment();
                                break;
                            case R.id.settings :
                                newFragment = new SettingsFragment();
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
}