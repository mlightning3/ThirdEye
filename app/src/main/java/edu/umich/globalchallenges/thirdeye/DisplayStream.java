package edu.umich.globalchallenges.thirdeye;

import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
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

    private static final int UM_BLUE = 0xFF00274C;
    private static final int UM_MAZE = 0xFFFFCB05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_display_stream);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Open video feed
        final WebView webview = (WebView) this.findViewById(R.id.web);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.loadUrl("http://stream.pi:5000/video_feed");
    }

    /**
     * Displays a snackbar with a message
     *
     * @param view The view from which we need to send this message
     * @param message The message we want displayed
     */
    private void snack_message(View view, String message) {
        Snackbar messagebar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        messagebar.getView().setBackgroundColor(UM_BLUE);
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
        String url = "http://stream.pi:5000/snapshot?filename=default_picture";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        snack_message(view, "Picture taken");
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
        String url = "http://stream.pi:5000/video_capture?filename=default_video&status=" + videostatus;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(videostatus) {
                            snack_message(view, "Done recoding");
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
        RequestQueue queue = Volley.newRequestQueue(this);
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
        RequestQueue queue = Volley.newRequestQueue(this);
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
}
