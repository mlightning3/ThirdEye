package edu.umich.globalchallenges.thirdeye.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.os.CountDownTimer;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

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

import edu.umich.globalchallenges.thirdeye.MainActivity;
import edu.umich.globalchallenges.thirdeye.R;
import edu.umich.globalchallenges.thirdeye.dialog.ChangeFilenameDialog;
import edu.umich.globalchallenges.thirdeye.dialog.ChangeVideoSettingsDialog;
import edu.umich.globalchallenges.thirdeye.dialog.Dialogs;
import edu.umich.globalchallenges.thirdeye.dialog.SetScreenTimerDialog;
import edu.umich.globalchallenges.thirdeye.gui.DisplayStreamLayout;
import edu.umich.globalchallenges.thirdeye.gui.VerticalSeekBar;

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
    private int visibleButtonDuration = 30000;
    private int forceScreenOnDuration;

    private MainActivity mainActivity;
    private CountDownTimer hideButtonsTimer;
    private CountDownTimer enableScreenOffTimer;
    private SharedPreferences sharedPreferences;
    private VerticalSeekBar focusBar;
    private VerticalSeekBar lightBar;
    private RequestQueue queue;
    private WebView webView;
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

        updateScreenTimeout();

        // Start countdown timers
        hideButtonsTimer = new CountDownTimer(visibleButtonDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // Nothing needed
            }

            public void onFinish() {
                updateButtons(false);
            }
        }.start();
        if(forceScreenOnDuration > 0) {
            enableScreenOffTimer = new CountDownTimer(forceScreenOnDuration, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Nothing needed
                }

                @Override
                public void onFinish() {
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }.start();
        }
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
        inflater.inflate(R.menu.screen_timeout_menu, menu);
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
        View view = inflater.inflate(R.layout.display_stream_fragment, container, false);
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
        mainActivity.wifi_connect(); // Try to connect to the network with the server automatically
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
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.stream_title);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " context is wrong (should be attached to MainActivity)");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
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
                take_snapshot();
                break;
            case R.id.grayscale:
                toggle_grayscale();
                break;
            case R.id.resolution:
                toggle_resolution();
                break;
            case R.id.record:
                video_capture(view);
                break;
            case R.id.autofocus:
                toggle_autofocus();
                break;
            case R.id.light:
                toggle_light();
                break;
            default: break;
        }
    }

    /**
     * Callback for after user uses some dialogs
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
            case Dialogs.SCREENTIMERDIALOG:
                if(resultCode == Activity.RESULT_OK) {
                    updateScreenTimeout();
                    resetTimer();
                }
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
        FragmentManager fragmentManager = getFragmentManager();
        DialogFragment fragment = null;
        String tag = null;
        switch (item.getItemId()) {
            case R.id.edit:
                fragment = new ChangeFilenameDialog();
                tag = "editfilename";
                break;
            case R.id.refresh:
                mainActivity.wifi_connect();
                webView.reload();
                break;
            case R.id.extra_settings:
                DialogFragment videoSettingsDialog = new ChangeVideoSettingsDialog();
                videoSettingsDialog.setTargetFragment(this, Dialogs.CHANGEVIDEOSETTINGS_DIALOG);
                tag = "editvideosettings";
                break;
            case R.id.zoomOut:
                fullZoomOut();
                break;
            case R.id.color_picker:
                ColorPicker colorPicker = new ColorPicker(getActivity(), 255, 255, 255);
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
            case R.id.screenTimeout:
                fragment = new SetScreenTimerDialog();
                fragment.setTargetFragment(this, Dialogs.SCREENTIMERDIALOG);
                tag = "setscreentimeout";
                break;
            default: break;
        }
        if(fragmentManager != null && fragment != null) {
            fragment.show(fragmentManager, tag);
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
            mainActivity.snack_message(R.string.focus_change_let_go);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            String url = "http://stream.pi:5000/set_focus?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.adjusting_focus);
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
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.invalid_user_key);
                                        break;
                                    case 403:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.unsupported_camera_focus);
                                        break;
                                    case 500:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.error_server_cant_focus);
                                        break;
                                    default:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.error_camera_focus);
                                        break;
                                }
                            } else {
                                if(mainActivity != null)
                                    mainActivity.snack_message(R.string.error_camera_focus);
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
            mainActivity.snack_message(R.string.light_change_let_go);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekbar) {
            String url = "http://stream.pi:5000/set_brightness?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.adjusting_brightness);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.invalid_user_key);
                                        break;
                                    case 403:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.unsupported_camera_light);
                                        break;
                                    case 500:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.error_server_cant_brightness);
                                        break;
                                    default:
                                        if(mainActivity != null)
                                            mainActivity.snack_message(R.string.error_camera_brightness);
                                        break;
                                }
                            } else {
                                if(mainActivity != null)
                                    mainActivity.snack_message(R.string.error_camera_brightness);
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
     * Builds a string out of the current date to send to server
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
     */
    private void take_snapshot() {
        final String pictureName = sharedPreferences.getString("filename", "default") + "_picture_" + imgCount;
        String url = "http://stream.pi:5000/snapshot?filename=" + pictureName + "&date=" + getDate();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(mainActivity != null)
                            mainActivity.snack_message("Picture taken, saved as " + pictureName);
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
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.invalid_user_key);
                                    break;
                                case 500:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_server_cant_snapshot);
                                    break;
                                default:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_camera_snapshot);
                                    break;
                            }
                        } else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.error_camera_snapshot);
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
    private void video_capture(final View view) {
        videostatus = !videostatus; // Flip video capture status
        final String videoName = sharedPreferences.getString("filename", "default") + "_video_" + vidCount;
        String url = "http://stream.pi:5000/video_capture?filename=" + videoName + "&status=" + videostatus + "&date=" + getDate();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(videostatus) {
                            if(mainActivity != null)
                                mainActivity.snack_message("Done recoding, saved as " + videoName);
                            vidCount++;
                            Button recording = (Button) view.findViewById(R.id.record);
                            recording.setText(R.string.record);
                            recording.getBackground().clearColorFilter();
                        }
                        else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.recording);
                            Button recording = (Button) view.findViewById(R.id.record);
                            recording.setText(R.string.recording);
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
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.invalid_user_key);
                                    break;
                                case 500:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_server_cant_record);
                                    break;
                                default:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_camera_record);
                                    break;
                            }
                        } else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.error_camera_record);
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle grayscale video.
     */
    private void toggle_grayscale() {
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
                        if(mainActivity != null)
                            mainActivity.snack_message(R.string.error_grayscale);
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle resolutionButton of stream.
     */
    private void toggle_resolution() {
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
                        if(mainActivity != null)
                            mainActivity.snack_message(R.string.error_resolution);
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
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.invalid_user_key);
                                    break;
                                case 403:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.unsupported_camera_focus);
                                    break;
                                case 500:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_server_cant_autofocus);
                                    break;
                                default:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_camera_autofocus);
                                    break;
                            }
                        } else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.error_camera_autofocus);
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle autofocus
     * ONLY WORKS ON SUPPORTED CAMERAS
     */
    private void toggle_autofocus() {
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
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.invalid_user_key);
                                    break;
                                case 403:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.unsupported_camera_light);
                                    break;
                                case 500:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_server_cant_light);
                                    break;
                                default:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_camera_light);
                                    break;
                            }
                        } else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.error_camera_light);
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    /**
     * Sends message to server to toggle the light on and off
     */
    private void toggle_light() {
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
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.invalid_user_key);
                                    break;
                                case 403:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.unsupported_camera_light);
                                    break;
                                case 500:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_server_cant_color);
                                    break;
                                default:
                                    if(mainActivity != null)
                                        mainActivity.snack_message(R.string.error_camera_color);
                                    break;
                            }
                        } else {
                            if(mainActivity != null)
                                mainActivity.snack_message(R.string.error_camera_color);
                        }
                    }
                }
        );
        queue.add(stringRequest);
        lightStatus = true; // Change this so when the user presses the toggle light button, it turns off the light
    }

    /**
     * Update the value for forceScreenOnDuration from the value stored in sharedPreferences
     *
     * If there isn't a value, this defaults to a 2 minute timeout.
     * Otherwise it sets to the value stored in sharedPreferences (making any negative 0)
     */
    private void updateScreenTimeout() {
        int temp = sharedPreferences.getInt("screen_timeout_value", 2);
        if(temp < 1) {
            temp = 0;
        }
        forceScreenOnDuration = temp * 60000;
    }

    /**
     * Resets the timer to make all the controls disappear
     */
    public void resetTimer() {
        updateButtons(true);
        hideButtonsTimer.cancel();
        hideButtonsTimer = new CountDownTimer(visibleButtonDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // Nothing needed
            }

            public void onFinish() {
                updateButtons(false);
            }
        }.start();
        enableScreenOffTimer.cancel();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(forceScreenOnDuration > 0) {
            enableScreenOffTimer = new CountDownTimer(forceScreenOnDuration, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Nothing needed
                }

                @Override
                public void onFinish() {
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }.start();
        }
    }
}
