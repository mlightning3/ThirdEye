package edu.umich.globalchallenges.thirdeye;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class DisplayStream extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_display_stream);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        seekbar = findViewById(R.id.seekBar);
        seekbar.setOnSeekBarChangeListener(new focusListener());
        queue = Volley.newRequestQueue(this);
        // Open video feed
        final WebView webview = (WebView) this.findViewById(R.id.web);
        webview.getSettings().setBuiltInZoomControls(true);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Disable hardware rendering on KitKat
        }
        webview.loadUrl("http://stream.pi:5000/video_feed");
    }

    /**
     * Listens for actions preformed on the seekbar, then changes the focus as appropriate
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
            snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Focus will change when you let go");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            String url = "http://stream.pi:5000/set_focus?value=" + value;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Adjusting focus...");
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Error changing focus");
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
                        snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Error changing autofocus");
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
