package edu.umich.globalchallenges.thirdeye;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * This fragment provides a view of the stream to the user. It also has a lot of controls for doing
 * various things on the server (take pictures, video, etc.) but also has shortcuts to change some
 * settings so the user doesn't have to leave the stream.
 */
public class DisplayStreamFragment extends Fragment implements View.OnClickListener {
    // Important Globals
    private static boolean grayscale = true;
    private static boolean lowresolution = true;
    private static boolean videostatus = true;
    private static boolean autofocusStatus = true;
    private static int imgCount = 1;
    private static int vidCount = 1;

    private static String ssid;
    private static String sharedkey;

    private SharedPreferences sharedPreferences;
    private WifiManager wifiManager;
    private SeekBar seekbar;
    private RequestQueue queue;
    private WebView webView;
    private View view;
    private Button snapshotButton;
    private Button recordButton;
    private Button grayscaleButton;
    private Button resolutionButton;
    private Button autofocusButton;

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
    }

    /**
     * Adds option buttons to actionbar
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_menu, menu);
        inflater.inflate(R.menu.refresh_menu, menu);
        inflater.inflate(R.menu.zoom_out_menu, menu);
        inflater.inflate(R.menu.extra_settings_menu, menu);
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
        view = inflater.inflate(R.layout.display_stream_fragment, container, false);
        // Set up things user will interact with
        seekbar = view.findViewById(R.id.seekBar);
        snapshotButton = (Button) view.findViewById(R.id.snapshot);
        recordButton = (Button) view.findViewById(R.id.record);
        grayscaleButton = (Button) view.findViewById(R.id.grayscale);
        resolutionButton = (Button) view.findViewById(R.id.resolution);
        autofocusButton = (Button) view.findViewById(R.id.autofocus);
        initializeListeners(view);
        // Open video feed
        webView = (WebView) view.findViewById(R.id.web);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Disable hardware rendering on KitKat
        }
        wifi_connect(); // Try to connect to the network with the server automatically
        webView.loadUrl("http://stream.pi:5000/video_feed");
        return view;
    }

    /**
     * Here we set up all the buttons so that when they are clicked on they preform an action
     *
     * @param view The view these come from (ideally from onCreateView)
     */
    private void initializeListeners(View view) {
        snapshotButton.setOnClickListener(this);
        recordButton.setOnClickListener(this);
        grayscaleButton.setOnClickListener(this);
        resolutionButton.setOnClickListener(this);
        autofocusButton.setOnClickListener(this);
        autofocusButton.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.green_light), getResources().getColor(R.color.green_dark)));
        seekbar.setOnSeekBarChangeListener(new focusListener());
        updateButtons();
    }

    /**
     * Updates the viability of buttons based on preferences
     */
    private void updateButtons() {
        if(sharedPreferences.getBoolean("media_saving", true)) {
            snapshotButton.setVisibility(View.VISIBLE);
            recordButton.setVisibility(View.VISIBLE);
        } else {
            snapshotButton.setVisibility(View.GONE);
            recordButton.setVisibility(View.GONE);
        }
        if(sharedPreferences.getBoolean("image_manip", true)) {
            grayscaleButton.setVisibility(View.VISIBLE);
            resolutionButton.setVisibility(View.VISIBLE);
        } else {
            grayscaleButton.setVisibility(View.GONE);
            resolutionButton.setVisibility(View.GONE);
        }
        if(sharedPreferences.getBoolean("focus_control", false)) {
            autofocusButton.setVisibility(View.VISIBLE);
            seekbar.setVisibility(View.VISIBLE);
        } else {
            autofocusButton.setVisibility(View.GONE);
            seekbar.setVisibility(View.GONE);
        }
    }

    /**
     * Anything that needs to be saved between use of fragments should be saved here
     */
    @Override
    public void onPause() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        webView.onPause();
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
            case R.id.snapshot:
                take_snapshot(view);
                break;
            case R.id.grayscale:
                toggle_grayscale(view);
                break;
            case R.id.resolution:
                toggle_resolution(view);
                break;
            case R.id.record:
                video_capture(view);
                break;
            case R.id.autofocus:
                toggle_autofocus(view);
                break;
            default: break;
        }
    }

    /**
     * Callback for after user uses the ChangeVideoSettingsDialog
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Dialogs.CHANGEVIDEOSETTINGS_DIALOG:
                if(resultCode == Activity.RESULT_OK) {
                    updateButtons();
                } else if(resultCode == Activity.RESULT_CANCELED) {
                    // Nothing needed
                }
                break;
            default: break;
        }
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
            case R.id.edit:
                DialogFragment filenameDialog = new ChangeFilenameDialog();
                filenameDialog.show(getFragmentManager(), "editfilename");
                break;
            case R.id.refresh:
                wifi_connect();
                webView.reload();
                break;
            case R.id.extra_settings:
                DialogFragment videoSettingsDialog = new ChangeVideoSettingsDialog();
                videoSettingsDialog.setTargetFragment(this, Dialogs.CHANGEVIDEOSETTINGS_DIALOG);
                videoSettingsDialog.show(getFragmentManager(), "editvideosettings");
                break;
            case R.id.zoomOut:
                fullZoomOut();
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Class for listening to things done with the seekbar to change the focus
     */
    public class focusListener implements SeekBar.OnSeekBarChangeListener {
        private double value;

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            if(user) {
                value = progress / 100.0;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            snack_message(view, "Focus will change when you let go");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            String url = "http://stream.pi:5000/set_focus?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            snack_message(view, "Adjusting focus...");
                            Button autofocus = (Button) view.findViewById(R.id.autofocus);
                            autofocus.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.red_light), getResources().getColor(R.color.red_dark)));
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
                                        snack_message(view, "Server does not support changing focus");
                                        break;
                                    case 500:
                                        snack_message(view, "Server unable to change focus");
                                        break;
                                    default:
                                        snack_message(view, "Unknown error while changing focus");
                                        break;
                                }
                            } else {
                                snack_message(view, "Unknown error while changing focus");
                            }
                        }
                    }
            );
            queue.add(stringRequest);
            autofocusStatus = true; // Change this so when the user presses the toggle autofocusButton button, it enables autofocusButton
        }
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

    private void fullZoomOut() {
        while(webView.zoomOut()){}
    }

    /**
     * Builds a string out of the current date
     *
     * @return String formatted as YYYY-MM-DD
     */
    private String getDate() {
        Date cur = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(cur);
    }

    /**
     * Sends a message to the server to take a picture. Sends filename to save picture as.
     *
     * @param view
     */
    public void take_snapshot(final View view) {
        final String pictureName = sharedPreferences.getString("filename", "default") + "_picture_" + imgCount;
        String url = "http://stream.pi:5000/snapshot?filename=" + pictureName + "&date=" + getDate();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        snack_message(view, "Picture taken, saved as " + pictureName);
                        imgCount++;
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
                                    snack_message(view, "Server unable to save snapshot");
                                    break;
                                default:
                                    snack_message(view, "Unknown error while taking snapshot");
                                    break;
                            }
                        } else {
                            snack_message(view, "Unknown error while taking snapshot");
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to start/stop recording video. Sends filename to save video as.
     *
     * @param view
     */
    public void video_capture(final View view) {
        videostatus = !videostatus; // Flip video capture status
        final String videoName = sharedPreferences.getString("filename", "default") + "_video_" + vidCount;
        String url = "http://stream.pi:5000/video_capture?filename=" + videoName + "&status=" + videostatus + "&date=" + getDate();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(videostatus) {
                            snack_message(view, "Done recoding, saved as " + videoName);
                            vidCount++;
                            Button recording = (Button) view.findViewById(R.id.record);
                            recording.setText("Record");
                            recording.getBackground().clearColorFilter();
                        }
                        else {
                            snack_message(view, "Recording...");
                            Button recording = (Button) view.findViewById(R.id.record);
                            recording.setText("Recording...");
                            recording.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.red_light), getResources().getColor(R.color.red_dark)));
                        }
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
                                    snack_message(view, "Server unable to save video");
                                    break;
                                default:
                                    snack_message(view, "Unknown error while recording video");
                                    break;
                            }
                        } else {
                            snack_message(view, "Unknown error while recording video");
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle grayscale video.
     *
     * @param view
     */
    public void toggle_grayscale(final View view) {
        grayscale = !grayscale; // Flip grayscale status
        String url = "http://stream.pi:5000/grayscale?status=" + grayscale;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Nothing needed
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        snack_message(view, "Error changing grayscale");
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle resolutionButton of stream.
     *
     * @param view
     */
    public void toggle_resolution(final View view) {
        lowresolution = !lowresolution; // Flip resolutionButton status
        String url = "http://stream.pi:5000/resolution?status=" + lowresolution;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Nothing needed
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        snack_message(view, "Error changing resolution");
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to set autofocusButton on or off
     *
     * @param status Set status of autofocusButton. True turns off autofocusButton, False turns autofocusButton on
     */
    private void set_autofocus_status(boolean status) {
        String url = "http://stream.pi:5000/autofocus?status=" + status;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Nothing needed
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
                                    snack_message(view, "Server does not support changing focus");
                                    break;
                                case 500:
                                    snack_message(view, "Server unable to change autofocus");
                                    break;
                                default:
                                    snack_message(view, "Unknown error while changing autofocus");
                                    break;
                            }
                        } else {
                            snack_message(view, "Unknown error while changing autofocus");
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle autofocusButton
     * ONLY WORKS ON SUPPORTED CAMERAS
     *
     * @param view
     */
    public void toggle_autofocus(View view) {
        autofocusStatus = !autofocusStatus; // Flip autofocusButton status
        set_autofocus_status(autofocusStatus);
        Button autofocus = (Button) view.findViewById(R.id.autofocus);
        if(autofocusStatus) {
            autofocus.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.red_light), getResources().getColor(R.color.red_dark)));
        }
        else {
            autofocus.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.green_light), getResources().getColor(R.color.green_dark)));
        }
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
