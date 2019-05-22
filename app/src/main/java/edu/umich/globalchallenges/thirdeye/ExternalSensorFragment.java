package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ExternalSensorFragment extends Fragment {
    // Important Globals
    private int timeoutDuration = 5000;
    private boolean fetchHeartrate;

    private FragmentCommManager commManager;
    private FragmentWifiManager wifiManager;
    private RequestQueue queue;
    private CountDownTimer countDownTimer;
    private TextView heartrate;
    private TextView heartrateUpdate;

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
        countDownTimer = null;
        fetchHeartrate = false;
    }

    /**
     * Adds option buttons to actionbar
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
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
        View view = inflater.inflate(R.layout.external_sensor_fragment, container, false);
        wifiManager.wifi_connect();
        heartrate = (TextView) view.findViewById(R.id.heartrate);
        heartrateUpdate = (TextView) view.findViewById(R.id.heart_rate_updated);
        heartrateUpdate.setVisibility(View.INVISIBLE);
        Switch heartrateEnable = (Switch) view.findViewById(R.id.heart_rate_switch);
        heartrateEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    resetTimer();
                } else {
                    heartrateUpdate.setVisibility(View.INVISIBLE);
                }
                fetchHeartrate = isChecked;
            }
        });
        return view;
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
            case R.id.refresh:
                wifiManager.wifi_connect();
                updateHeartRate();
                // TODO do the refresh thing
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fetches heart rate information from server, updating view as appropriate.
     * Currently fetches all controller data and assumes it is only heart rate info
     */
    public void updateHeartRate() {
        if(wifiManager != null && wifiManager.connected_to_network()) {
            String url = "http://stream.pi:5000/get_controller_data";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Only update if we get a single number (don't display an empty line or multiple numbers in same line)
                            if(response.matches("^[0-9]+[ \t\n\f\r]*$")) {
                                String bpm = response;
                                heartrate.setText(bpm);
                                Animation fadeout = new AlphaAnimation(1.0f, 0.0f);
                                fadeout.setDuration(3000);
                                fadeout.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) { }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        heartrateUpdate.clearAnimation();
                                        heartrateUpdate.setVisibility(View.INVISIBLE);
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) { }
                                });
                                heartrateUpdate.setVisibility(View.VISIBLE);
                                heartrateUpdate.startAnimation(fadeout);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(getView() != null && commManager != null) { // Only try to let us know of problems if we are still looking at fragment
                                if (response != null && response.data != null) {
                                    switch (response.statusCode) {
                                        case 401:
                                            commManager.snack_message(R.string.invalid_user_key);
                                            break;
                                        case 403:
                                            commManager.snack_message(R.string.unsupported_external_sensors);
                                            break;
                                        case 500:
                                            commManager.snack_message(R.string.error_pi_unable_read_sensors);
                                            break;
                                        default:
                                            commManager.snack_message(R.string.error_external_sensors);
                                            break;
                                    }
                                } else {
                                    commManager.snack_message(R.string.error_external_sensors);
                                }
                            }
                        }
                    }
            );
            queue.add(stringRequest);
        } else if(wifiManager != null) {
            wifiManager.wifi_connect();
        }
        if(fetchHeartrate) { // Only start a new timer if switch for fetching heart rate is enabled
            resetTimer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof  MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.sensor_title);
        }
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
     * Resets the timer to get updated sensor data if there isn't a timer running
     */
    public void resetTimer() {
        if(countDownTimer == null) {
            countDownTimer = new CountDownTimer(timeoutDuration, 1000) {

                public void onTick(long millisUntilFinished) {
                    // Nothing needed
                }

                public void onFinish() {
                    countDownTimer = null;
                    if (fetchHeartrate) {
                        updateHeartRate();
                    }
                }
            }.start();
        }
    }
}
