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
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
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
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

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
    private static boolean lightStatus = false;
    private static int imgCount = 1;
    private static int vidCount = 1;
    private int timeoutDuration = 30000;

    private FragmentCommManager commManager;
    private FragmentWifiManager wifiManager;
    private CountDownTimer countDownTimer;
    private SharedPreferences sharedPreferences;
    private VerticalSeekBar focusBar;
    private VerticalSeekBar lightBar;
    private RequestQueue queue;
    private WebView webView;
    private View view;
    private Button snapshotButton;
    private Button recordButton;
    private Button grayscaleButton;
    private Button resolutionButton;
    private Button autofocusButton;
    private Button lightButton;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        queue = Volley.newRequestQueue(getContext());
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Start countdown timer
        countDownTimer = new CountDownTimer(timeoutDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // Nothing needed
            }

            public void onFinish() {
                updateButtons(false);
            }
        }.start();
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
        inflater.inflate(R.menu.color_menu, menu);
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
        DisplayStreamLayout ds = (DisplayStreamLayout) view.findViewById(R.id.display_frame);
        ds.attachDisplayStream(this); // Used so the timer can be reset on touch
        focusBar = (VerticalSeekBar) view.findViewById(R.id.focus_bar);
        lightBar = (VerticalSeekBar) view.findViewById(R.id.light_bar);
        snapshotButton = (Button) view.findViewById(R.id.snapshot);
        recordButton = (Button) view.findViewById(R.id.record);
        grayscaleButton = (Button) view.findViewById(R.id.grayscale);
        resolutionButton = (Button) view.findViewById(R.id.resolution);
        autofocusButton = (Button) view.findViewById(R.id.autofocus);
        lightButton = (Button) view.findViewById(R.id.light);
        initializeListeners(view);
        // Open video feed
        webView = (WebView) view.findViewById(R.id.web);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Disable hardware rendering on KitKat
        }
        wifiManager.wifi_connect(); // Try to connect to the network with the server automatically
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
        focusBar.setOnSeekBarChangeListener(new focusListener());
        lightBar.setOnSeekBarChangeListener(new lightListener());
        lightButton.setOnClickListener(this);
        updateButtons(true);
    }

    /**
     * Updates the viability of buttons based on preferences and countdown timer
     * @param visible Should the controls be visible or not
     */
    private void updateButtons(boolean visible) {
        if(sharedPreferences.getBoolean("media_saving", true) && visible) {
            snapshotButton.setVisibility(View.VISIBLE);
            recordButton.setVisibility(View.VISIBLE);
        } else {
            snapshotButton.setVisibility(View.GONE);
            recordButton.setVisibility(View.GONE);
        }
        if(sharedPreferences.getBoolean("image_manip", true) && visible) {
            grayscaleButton.setVisibility(View.VISIBLE);
            resolutionButton.setVisibility(View.VISIBLE);
        } else {
            grayscaleButton.setVisibility(View.GONE);
            resolutionButton.setVisibility(View.GONE);
        }
        if(sharedPreferences.getBoolean("focus_control", false) && visible) {
            autofocusButton.setVisibility(View.VISIBLE);
            focusBar.setVisibility(View.VISIBLE);
        } else {
            autofocusButton.setVisibility(View.GONE);
            focusBar.setVisibility(View.GONE);
        }
        if(sharedPreferences.getBoolean("light_control", true) && visible) {
            lightButton.setVisibility(View.VISIBLE);
            lightBar.setVisibility(View.VISIBLE);
        } else {
            lightButton.setVisibility(View.GONE);
            lightBar.setVisibility(View.GONE);
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentWifiManager && context instanceof FragmentCommManager) {
            wifiManager = (FragmentWifiManager) context;
            commManager = (FragmentCommManager) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FragmentCommManager & FragmentWifiManager");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        wifiManager = null;
        commManager = null;
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
            case R.id.light:
                toggle_light(view);
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
                    updateButtons(true);
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
                wifiManager.wifi_connect();
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
            case R.id.color_picker:
                ColorPicker colorPicker = new ColorPicker(getActivity(), 0, 0, 255);
                colorPicker.show();
                colorPicker.enableAutoClose();
                colorPicker.setCallback(new ColorPickerCallback() {
                    @Override
                    public void onColorChosen(int color) {
                        String hexcolor = Integer.toHexString(color).substring(2, 8);
                        set_light_color(hexcolor);
                    }
                });
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Class for listening to things done with the seekbar to change the focus
     */
    public class focusListener implements AppCompatSeekBar.OnSeekBarChangeListener {
        private double value;

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            if(!user) {
                value = progress / 100.0;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            commManager.snack_message("Focus will change when you let go");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            String url = "http://stream.pi:5000/set_focus?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(commManager != null)
                                commManager.snack_message("Adjusting focus...");
                            autofocusButton.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.red_light), getResources().getColor(R.color.red_dark)));
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:
                                        if(commManager != null)
                                            commManager.snack_message("Invalid User Key");
                                        break;
                                    case 403:
                                        if(commManager != null)
                                            commManager.snack_message("Server does not support changing focus");
                                        break;
                                    case 500:
                                        if(commManager != null)
                                            commManager.snack_message("Server unable to change focus");
                                        break;
                                    default:
                                        if(commManager != null)
                                            commManager.snack_message("Unknown error while changing focus");
                                        break;
                                }
                            } else {
                                if(commManager != null)
                                    commManager.snack_message("Unknown error while changing focus");
                            }
                        }
                    }
            );
            queue.add(stringRequest);
            autofocusStatus = true; // Change this so when the user presses the toggle autofocus button, it enables autofocus
        }
    }

    public class lightListener implements AppCompatSeekBar.OnSeekBarChangeListener {
        private double value;

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            if(!user) {
                value = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            commManager.snack_message("Light brightness will change when you let go");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekbar) {
            String url = "http://stream.pi:5000/set_brightness?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(commManager != null)
                                commManager.snack_message("Adjusting brightness...");
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:
                                        if(commManager != null)
                                            commManager.snack_message("Invalid User Key");
                                        break;
                                    case 403:
                                        if(commManager != null)
                                            commManager.snack_message("Server does not support changing brightness");
                                        break;
                                    case 500:
                                        if(commManager != null)
                                            commManager.snack_message("Server unable to change brightness");
                                        break;
                                    default:
                                        if(commManager != null)
                                            commManager.snack_message("Unknown error while changing brightness");
                                        break;
                                }
                            } else {
                                if(commManager != null)
                                    commManager.snack_message("Unknown error while changing brightness");
                            }
                        }
                    }
            );
            queue.add(stringRequest);
            lightStatus = true; // Change this so when the user presses the toggle light button, it turns off the light
        }
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
                        if(commManager != null)
                            commManager.snack_message("Picture taken, saved as " + pictureName);
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
                                    if(commManager != null)
                                        commManager.snack_message("Invalid User Key");
                                    break;
                                case 500:
                                    if(commManager != null)
                                        commManager.snack_message("Server unable to save snapshot");
                                    break;
                                default:
                                    if(commManager != null)
                                        commManager.snack_message("Unknown error while taking snapshot");
                                    break;
                            }
                        } else {
                            if(commManager != null)
                                commManager.snack_message("Unknown error while taking snapshot");
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
                            if(commManager != null)
                                commManager.snack_message("Done recoding, saved as " + videoName);
                            vidCount++;
                            Button recording = (Button) view.findViewById(R.id.record);
                            recording.setText("Record");
                            recording.getBackground().clearColorFilter();
                        }
                        else {
                            if(commManager != null)
                                commManager.snack_message("Recording...");
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
                                    if(commManager != null)
                                        commManager.snack_message("Invalid User Key");
                                    break;
                                case 500:
                                    if(commManager != null)
                                        commManager.snack_message("Server unable to save video");
                                    break;
                                default:
                                    if(commManager != null)
                                        commManager.snack_message("Unknown error while recording video");
                                    break;
                            }
                        } else {
                            if(commManager != null)
                                commManager.snack_message("Unknown error while recording video");
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
                        if(commManager != null)
                            commManager.snack_message("Error changing grayscale");
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
                        if(commManager != null)
                            commManager.snack_message("Error changing resolution");
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to set autofocus on or off
     *
     * @param status Set status of autofocus. True turns off autofocus, False turns autofocus on
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
                                    if(commManager != null)
                                        commManager.snack_message("Invalid User Key");
                                    break;
                                case 403:
                                    if(commManager != null)
                                        commManager.snack_message("Server does not support changing focus");
                                    break;
                                case 500:
                                    if(commManager != null)
                                        commManager.snack_message("Server unable to change autofocus");
                                    break;
                                default:
                                    if(commManager != null)
                                        commManager.snack_message("Unknown error while changing autofocus");
                                    break;
                            }
                        } else {
                            if(commManager != null)
                                commManager.snack_message("Unknown error while changing autofocus");
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle autofocus
     * ONLY WORKS ON SUPPORTED CAMERAS
     *
     * @param view
     */
    public void toggle_autofocus(View view) {
        autofocusStatus = !autofocusStatus; // Flip autofocusButton status
        set_autofocus_status(autofocusStatus);
        if(autofocusStatus) {
            autofocusButton.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.red_light), getResources().getColor(R.color.red_dark)));
        }
        else {
            autofocusButton.getBackground().setColorFilter(new LightingColorFilter(getResources().getColor(R.color.green_light), getResources().getColor(R.color.green_dark)));
        }
    }

    /**
     * Sends message to server to set light on or off
     *
     * @param status Set status of light. True turns off light, False turns light on
     */
    private void  set_light_status(boolean status) {
        String url = "http://stream.pi:5000/light?status=" + status;
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
                                    if(commManager != null)
                                        commManager.snack_message("Invalid User Key");
                                    break;
                                case 403:
                                    if(commManager != null)
                                        commManager.snack_message("Server does not support changing light");
                                    break;
                                case 500:
                                    if(commManager != null)
                                        commManager.snack_message("Server unable to change light");
                                    break;
                                default:
                                    if(commManager != null)
                                        commManager.snack_message("Unknown error while changing light");
                                    break;
                            }
                        } else {
                            if(commManager != null)
                                commManager.snack_message("Unknown error while changing light");
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle the light on and off
     *
     * @param view
     */
    public void toggle_light(View view) {
        lightStatus = !lightStatus;
        set_light_status(lightStatus);
    }

    /**
     * Sends message to server to change color to that encoded as a hex string
     *
     * @param color A string with the hex code for a color
     */
    private void set_light_color(String color) {
        String url = "http://stream.pi:5000/set_color?value=" + color;
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
                                    if(commManager != null)
                                        commManager.snack_message("Invalid User Key");
                                    break;
                                case 403:
                                    if(commManager != null)
                                        commManager.snack_message("Server does not support changing light color");
                                    break;
                                case 500:
                                    if(commManager != null)
                                        commManager.snack_message("Server unable to change light color");
                                    break;
                                default:
                                    if(commManager != null)
                                        commManager.snack_message("Unknown error while changing light color");
                                    break;
                            }
                        } else {
                            if(commManager != null)
                                commManager.snack_message("Unknown error while changing light color");
                        }
                    }
                }
        );
        queue.add(stringRequest);
        lightStatus = true; // Change this so when the user presses the toggle light button, it turns off the light
    }

    /**
     * Resets the timer to make all the controls disappear
     */
    public void resetTimer() {
        updateButtons(true);
        countDownTimer = new CountDownTimer(timeoutDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // Nothing needed
            }

            public void onFinish() {
                updateButtons(false);
            }
        }.start();
    }
}
