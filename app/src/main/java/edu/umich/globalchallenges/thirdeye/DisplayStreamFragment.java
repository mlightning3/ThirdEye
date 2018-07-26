package edu.umich.globalchallenges.thirdeye;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class DisplayStreamFragment extends Fragment implements View.OnClickListener{
    // Important Globals
    private static boolean grayscale = true;
    private static boolean lowresolution = true;
    private static boolean videostatus = true;
    private static boolean autofocusStatus = true;
    private static int imgCount = 1;
    private static int vidCount = 1;

    private SharedPreferences sharedPreferences;
    private SeekBar seekbar;
    private RequestQueue queue;
    private WebView webView;
    private View view;

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
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        initializeButtons(view);
        seekbar = view.findViewById(R.id.seekBar);
        seekbar.setOnSeekBarChangeListener(new focusListener());
        // Open video feed
        webView = (WebView) view.findViewById(R.id.web);
        webView.getSettings().setBuiltInZoomControls(true);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Disable hardware rendering on KitKat
        }
        webView.loadUrl("http://stream.pi:5000/video_feed");
        return view;
    }

    /**
     * Here we set up all the buttons so that when they are clicked on they preform an action
     *
     * @param view The view these come from (ideally from onCreateView)
     */
    private void initializeButtons(View view) {
        Button snapshot = (Button) view.findViewById(R.id.snapshot);
        snapshot.setOnClickListener(this);
        Button grayscale = (Button) view.findViewById(R.id.grayscale);
        grayscale.setOnClickListener(this);
        Button resolution = (Button) view.findViewById(R.id.resolution);
        resolution.setOnClickListener(this);
        Button record = (Button) view.findViewById(R.id.record);
        record.setOnClickListener(this);
        Button autofocus = (Button) view.findViewById(R.id.autofocus);
        autofocus.setOnClickListener(this);
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
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            snack_message(view, "Error changing focus");
                        }
                    }
            );
            queue.add(stringRequest);
            autofocusStatus = true; // Change this so when the user presses the toggle autofocus button, it enables autofocus
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

    /**
     * Sends a message to the server to take a picture. Sends filename to save picture as.
     *
     * @param view
     */
    public void take_snapshot(final View view) {
        final String pictureName = sharedPreferences.getString("filename", "default") + "_picture_" + imgCount;
        String url = "http://stream.pi:5000/snapshot?filename=" + pictureName;
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
                        snack_message(view, "Error taking snapshot");
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
        String url = "http://stream.pi:5000/video_capture?filename=" + videoName + "&status=" + videostatus;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(videostatus) {
                            snack_message(view, "Done recoding, saved as " + videoName);
                            vidCount++;
                        }
                        else {
                            snack_message(view, "Recording...");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        snack_message(view, "Error recording video");
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
     * Sends message to server to toggle resolution of stream.
     *
     * @param view
     */
    public void toggle_resolution(final View view) {
        lowresolution = !lowresolution; // Flip resolution status
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
                        snack_message(view, "Error changing autofocus");
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
        autofocusStatus = !autofocusStatus; // Flip autofocus status
        set_autofocus_status(autofocusStatus);
    }
}
