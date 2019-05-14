package edu.umich.globalchallenges.thirdeye;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This fragment provides the user with a way to see information about the server, as well as email
 * it to another party. THIS SHOULD ONLY BE USED FOR DEBUGGING AND SHOULD BE REMOVED FOR PUBLIC RELEASE
 */
public class LogViewerFragment extends Fragment {

    private FragmentCommManager commManager;
    private FragmentWifiManager wifiManager;
    private LinearLayout loadingHeader;
    private LinearLayout logContent;
    private RequestQueue queue;
    private String jsonMessage;
    private List<List<String>> logs;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logs = new ArrayList<>();
        setHasOptionsMenu(true);
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
        wifiManager.wifi_connect(); // Try to connect to network automatically
        View view = inflater.inflate(R.layout.log_viewer_fragment, container, false);
        loadingHeader = (LinearLayout) view.findViewById(R.id.LoadingHeader);
        logContent = (LinearLayout) view.findViewById(R.id.LogContent);
        queue = Volley.newRequestQueue(getContext());
        fetchData();
        return view;
    }

    /**
     * Callback for after user uses the EmailLogsDialog
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Dialogs.EMAILLOGSDIALOG:
                if(resultCode == Activity.RESULT_OK) {
                    if(logs.size() > 0) { // Only send emails if we have logs
                        Date cur = Calendar.getInstance().getTime();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        String body = "Logs as of " + dateFormat.format(cur) + "\n";
                        for(int i = 0; i < logs.size(); i++) {
                            body = body + "\n" + logs.get(i).get(0) + "\n" + logs.get(i).get(1);
                        }
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, "ThridEye Logs");
                        intent.putExtra(Intent.EXTRA_TEXT, body);
                        intent.setData(Uri.parse("mailto:"));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(intent);
                    } else {
                        commManager.snack_message(R.string.no_logs_to_send);
                    }
                } else if(resultCode == Activity.RESULT_CANCELED) {
                    // Nothing needed
                }
                break;
            default:
                commManager.snack_message(R.string.oops);
                break;
        }
    }

    /**
     * Adds buttons to actionbar
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.email_menu, menu);
        inflater.inflate(R.menu.refresh_menu, menu);
    }

    /**
     * Anything that needs to be saved between use of fragments should be saved here
     */
    @Override
    public void onPause() {
        super.onPause();
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
                fetchData();
                break;
            case R.id.email:
                DialogFragment emailLogsDialog = new EmailLogsDialog();
                emailLogsDialog.setTargetFragment(this, Dialogs.EMAILLOGSDIALOG);
                emailLogsDialog.show(getFragmentManager(), "emaillogsdialog");
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
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
     * Fetches the data from the server and parses the data before reloading activity view
     */
    public void fetchData() {
        String url = "http://stream.pi:5000/logs";
        loadingHeader.setVisibility(View.VISIBLE);
        logContent.setVisibility(View.INVISIBLE);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                jsonMessage = response.toString();
                updateLogs();
                loadingHeader.setVisibility(View.GONE);
                logContent.setVisibility(View.VISIBLE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(commManager != null)
                    commManager.snack_message(R.string.error_fetching_logs);
                jsonMessage = "null";
                loadingHeader.setVisibility(View.GONE);
                logContent.setVisibility(View.VISIBLE);
            }
        });
        queue.add(jsonArrayRequest);
    }

    /**
     * Updates the textviews for the logs from the server
     */
    public void updateLogs() {
        if(!jsonMessage.contentEquals("null")) {
            TextView system = (TextView) getView().findViewById(R.id.SystemLog);
            TextView server = (TextView) getView().findViewById(R.id.ServerLog);
            logs = new JsonLogParser(jsonMessage).getLogs();
            for(int i = 0; i < logs.size(); i++) {
                if(logs.get(i).get(0).contentEquals("System")) {
                    system.setText(logs.get(i).get(1));
                }
                else if(logs.get(i).get(0).contentEquals("Server")) {
                    server.setText(logs.get(i).get(1));
                }
            }
        }
    }
}
