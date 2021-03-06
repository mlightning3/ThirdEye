package edu.umich.globalchallenges.thirdeye.fragment;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import edu.umich.globalchallenges.thirdeye.adapter.FileItem;
import edu.umich.globalchallenges.thirdeye.adapter.FileItemAdapter;
import edu.umich.globalchallenges.thirdeye.json.JsonDatabaseParser;
import edu.umich.globalchallenges.thirdeye.MainActivity;
import edu.umich.globalchallenges.thirdeye.adapter.OnRecycleItemInteractionListener;
import edu.umich.globalchallenges.thirdeye.R;
import edu.umich.globalchallenges.thirdeye.dialog.DeleteFileDialog;
import edu.umich.globalchallenges.thirdeye.dialog.Dialogs;

/**
 * This fragment displays the list of pictures and videos that are on the server. The user can then
 * download and view them on their device by selecting an item, or delete a file off the server.
 */
public class FileViewerFragment extends Fragment implements OnRecycleItemInteractionListener,
                                                            SwipeRefreshLayout.OnRefreshListener {

    private static final int EXTERNAL_WRITE_PERMISSION = 509;

    private MainActivity mainActivity;
    private RecyclerView recyclerView;
    private LinearLayout loadingHeader;
    private FrameLayout noFilesHeader;
    private FileItemAdapter adapter;
    private RequestQueue queue;
    private String jsonMessage;
    private List<FileItem> database;
    private String toDelete;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String tempFilename;

    /**
     * This is called when the fragment is created, but before its view is
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toDelete = "";
        database = new ArrayList<>();
        tempFilename = "";
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
        mainActivity.wifi_connect(); // Try to connect to network automatically
        View view = inflater.inflate(R.layout.file_viewer_fragment, container, false);
        loadingHeader = (LinearLayout) view.findViewById(R.id.LoadingHeader);
        noFilesHeader = (FrameLayout) view.findViewById(R.id.NoFilesHeader);
        recyclerView = (RecyclerView) view.findViewById(R.id.file_view);
        recyclerView.setHasFixedSize(true);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        // Set the layout we want for the list
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Load in data
        queue = Volley.newRequestQueue(getContext());
        adapter = new FileItemAdapter(this, this);
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
                mainActivity.wifi_connect();
                fetchData();
                break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * When there is just a short touch, we download and open the file from the server
     *
     * @param position The position of the item touched
     */
    @Override
    public void onRecycleItemClick(int position) {
        // Build needed strings
        String filename = adapter.getItem(position).toString();
        filename = filename.substring(filename.indexOf(" ") + 1, filename.length());
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        // Check if we already have the file
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename;
        File file = new File(path);
        if(file.exists()) { // Open file if we already have it
            mainActivity.snack_message("Opening " + filename);
            Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
            openAttachmentIntent.setDataAndType(Uri.fromFile(file), MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            openAttachmentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                getContext().startActivity(openAttachmentIntent);
            } catch (ActivityNotFoundException e) {
                mainActivity.snack_message(R.string.error_opening_file);
            }
        } else { // Otherwise download the file and open it
            // Check for ability to save files before download
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                tempFilename = filename;
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_WRITE_PERMISSION);
            } else {
                startDownload(filename);
            }
        }
    }

    /**
     * When there is a long touch, we ask to delete the file on the server and delete if approved
     *
     * @param position The position of the item touched
     */
    @Override
    public void onRecycleItemLongClick(int position) {
        toDelete = database.get(position).getFilename(); // Grab filename of thing we want deleted
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
                                mainActivity.snack_message(toDelete + " deleted");
                                fetchData();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                mainActivity.snack_message(R.string.error_deleting_file_server);
                            }
                        }
                );
                queue.add(stringRequest);
                break;
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(false); // Stop showing the animation since we have a different one to use
        fetchData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setActionBarTitle(R.string.file_title);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_WRITE_PERMISSION :
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDownload(tempFilename);
                    tempFilename = "";
                }
                break;
            default: break;
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
     * Start the download of a file from the server
     * @param filename Name of file to get
     */
    private void startDownload(String filename) {
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
            mainActivity.snack_message("Downloading " + filename);
        } catch (IllegalStateException e) {
            mainActivity.snack_message("Error downloading " + filename + " from server");
        }
    }

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
                        mainActivity.snack_message(R.string.error_opening_file);
                    }
                }
            }
        }
        cursor.close();
    }

    /**
     * Fetches the data from the server and parses the data before reloading activity view
     */
    private void fetchData() {
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
                    for(FileItem item : database) {
                        adapter.addItem(item);
                    }
                    loadingHeader.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(mainActivity != null)
                    mainActivity.snack_message(R.string.error_fetching_files_server);
                jsonMessage = "null";
                if(database.size() == 0) {
                    loadingHeader.setVisibility(View.GONE);
                    noFilesHeader.setVisibility(View.VISIBLE);
                } else {
                    loadingHeader.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        queue.add(jsonArrayRequest);
    }

    /**
     * Reparses the JSON message and builds new database
     */
    private void rebuildDataSet() {
        if(!jsonMessage.contentEquals("null")) { // Only try to parse the message if we actually got something
            JsonDatabaseParser databaseParser = new JsonDatabaseParser();
            try {
                List<FileItem> serverDatabase = databaseParser.genDatabaseArray(jsonMessage);
                database = new ArrayList<>();
                database.addAll(serverDatabase);
            } catch (IOException e) { // Here we swallow the exception without doing anything about it
                mainActivity.snack_message(R.string.error_parsing_json);
            }
        } else {
            database = new ArrayList<>();
        }
    }
}
