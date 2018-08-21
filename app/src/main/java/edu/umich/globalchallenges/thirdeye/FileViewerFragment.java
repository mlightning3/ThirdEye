package edu.umich.globalchallenges.thirdeye;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.content.MimeTypeFilter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class FileViewerFragment extends Fragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    private static String ssid;
    private static String sharedkey;

    private SharedPreferences sharedPreferences;
    private WifiManager wifiManager;
    private RecyclerView recyclerView;
    private LinearLayout loadingHeader;
    private FrameLayout noFilesHeader;
    private FlexibleAdapter<IFlexible> adapter;
    private LinearLayoutManager layoutManager;
    private RequestQueue queue;
    private String jsonMessage;
    private List<List<String>> databaseMessage;
    private List<IFlexible> database;
    private String toDelete;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        toDelete = "";
        database = new ArrayList<>();
        database.add(new FileItem(this,"Nothing here yet!", "Try refreshing", false));
        setHasOptionsMenu(true);

        // Load settings
        ssid = "\"" + sharedPreferences.getString("ssid", "Pi_AP") + "\"";
        sharedkey = "\"" + sharedPreferences.getString("passphrase", "raspberry") + "\"";
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
        wifi_connect(); // Try to connect to network automatically
        View view = inflater.inflate(R.layout.file_viewer_fragment, container, false);
        loadingHeader = (LinearLayout) view.findViewById(R.id.LoadingHeader);
        noFilesHeader = (FrameLayout) view.findViewById(R.id.NoFilesHeader);
        recyclerView = (RecyclerView) view.findViewById(R.id.file_view);
        recyclerView.setHasFixedSize(true);
        // Set the layout we want for the list
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        // Load in data
        queue = Volley.newRequestQueue(getContext());
        adapter = new FlexibleAdapter<>(database);
        adapter.addListener(this);
        adapter.setMode(SelectableAdapter.Mode.SINGLE);
        recyclerView.setAdapter(adapter);
        fetchData();
        return view;
    }

    /**
     * Adds refresh button to actionbar
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
                wifi_connect();
                fetchData();
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * When there is just a short touch, we download and open the file from the server
     *
     * @param view View item was touched on
     * @param position The position of the item touched
     * @return True, always
     */
    @Override
    public boolean onItemClick(View view, int position) {
        // Build needed strings
        String filename = adapter.getItem(position).toString();
        filename = filename.substring(filename.indexOf(" ") + 1, filename.length());
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        // Check if we already have the file
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename;
        File file = new File(path);
        if(file.exists()) { // Open file if we already have it
            snack_message(view, "Opening " + filename);
            Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
            openAttachmentIntent.setDataAndType(Uri.fromFile(file), MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            openAttachmentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                getContext().startActivity(openAttachmentIntent);
                return true;
            } catch (ActivityNotFoundException e) {
                snack_message(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Unable to open file");
                return true;
            }
        } else { // Otherwise download the file and open it
            String url = "http://stream.pi:5000/media/" + filename;
            // Set up download and queue it up
            try {
                getContext().registerReceiver(downloadCompleteReceive, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle("Downloading " + filename);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                // Notify user
                snack_message(view, "Downloading " + filename);
                adapter.toggleSelection(position);
                return true;
            } catch (IllegalStateException e) {
                snack_message(view, "Error downloading " + filename + " from server");
                return true;
            }
        }
    }

    /**
     * When there is a long touch, we ask to delete the file on the server and delete if approved
     *
     * @param position The position of the item touched
     */
    @Override
    public void onItemLongClick(int position) {
        toDelete = databaseMessage.get(position).get(1); // Grab filename of thing we want deleted
        if(!toDelete.contentEquals("Try refreshing")) { // Don't try to remove the empty list message
            DialogFragment deleteFileDialog = new DeleteFileDialog();
            deleteFileDialog.setTargetFragment(this, Dialogs.DELETEFILEDIALOG);
            deleteFileDialog.show(getFragmentManager(), "deletefiledialog");
        }
    }

    /**
     * Callback for after user uses the EmailLogsDialog
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Dialogs.DELETEFILEDIALOG:
                String url = "http://stream.pi:5000/database/remove?filename=" + toDelete;
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                snack_message(getView(), toDelete + " deleted");
                                fetchData();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                snack_message(getView(), "Error deleting file on server");
                            }
                        }
                );
                queue.add(stringRequest);
                break;
        }
    }

    /**
     * This gets called when a download is completed and opens the file
     */
    BroadcastReceiver downloadCompleteReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                openDownload(context, downloadId);
            }
        }
    };

    /**
     * Opens file after downloading it from the server
     *
     * @param context Place download action came from
     * @param downloadId The download we wish to open
     */
    private void openDownload(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                Uri attachmentUri = Uri.parse(downloadLocalUri);
                if(attachmentUri!=null) {
                    // Get Content Uri.
                    if (ContentResolver.SCHEME_FILE.equals(attachmentUri.getScheme())) {
                        // FileUri - Convert it to contentUri.
                        File file = new File(attachmentUri.getPath());
                        attachmentUri = FileProvider.getUriForFile(getContext(), "edu.umich.globalchallenges.provider", file);
                    }

                    Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
                    openAttachmentIntent.setDataAndType(attachmentUri, downloadMimeType);
                    openAttachmentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        context.startActivity(openAttachmentIntent);
                    } catch (ActivityNotFoundException e) {
                        snack_message(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Unable to open file");
                    }
                }
            }
        }
        cursor.close();
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
     * Fetches the data from the server and parses the data before reloading activity view
     */
    public void fetchData() {
        String url = "http://stream.pi:5000/get_database";
        loadingHeader.setVisibility(View.VISIBLE);
        noFilesHeader.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                jsonMessage = response.toString();
                rebuildDataSet();
                if(database.size() == 0) {
                    loadingHeader.setVisibility(View.GONE);
                    noFilesHeader.setVisibility(View.VISIBLE);
                } else {
                    adapter.updateDataSet(database, true);
                    loadingHeader.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                snack_message(getView(), "Error retrieving file listing from server");
                jsonMessage = "null";
                loadingHeader.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
        queue.add(jsonArrayRequest);
    }

    /**
     * Reparses the JSON message and builds new database
     */
    public void rebuildDataSet() {
        if(!jsonMessage.contentEquals("null")) { // Only try to parse the message if we actually got something
            JsonDatabaseParser databaseParser = new JsonDatabaseParser();
            try {
                databaseMessage = databaseParser.genDatabaseArray(jsonMessage);
                database = new ArrayList<>();
                for(int i = 0; i < databaseMessage.size(); i++) {
                    database.add(new FileItem(this, databaseMessage.get(i).get(0), databaseMessage.get(i).get(1), true));
                }
            } catch (IOException e) { // Here we swallow the exception without doing anything about it
                snack_message(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), "Error parsing json message");
            }
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
