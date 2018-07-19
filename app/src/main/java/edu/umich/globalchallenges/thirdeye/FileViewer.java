package edu.umich.globalchallenges.thirdeye;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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
import eu.davidea.flexibleadapter.SelectableAdapter.Mode;
import eu.davidea.flexibleadapter.items.IFlexible;

public class FileViewer extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    private RecyclerView recyclerView;
    private FlexibleAdapter<IFlexible> adapter;
    private LinearLayoutManager layoutManager;
    private RequestQueue queue;
    private String jsonMessage;
    private List<List<String>> databaseMessage;
    private List<IFlexible> database;
    private String toDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        toDelete = "";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);

        database = new ArrayList<>();
        database.add(new FileItem("Nothing here yet!", "Loading..."));

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);
        // Set the layout we want for the list
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Load in data
        queue = Volley.newRequestQueue(this);
        adapter = new FlexibleAdapter<>(database);
        adapter.addListener(this);
        adapter.setMode(Mode.SINGLE);
        recyclerView.setAdapter(adapter);
        fetchData();
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
        String url = "http://stream.pi:5000/media/" + filename;
        // Set up download and queue it up
        try {
            registerReceiver(downloadCompleteReceive, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Downloading " + filename);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            // Notify user
            snack_message(view, "Downloading " + filename);
            adapter.toggleSelection(position);
            return true;
        }
        catch (IllegalStateException e) {
            snack_message(view, "Error downloading " + filename + " from server");
            return true;
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
        Snackbar deletebar = Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), "Delete file on server?", Snackbar.LENGTH_LONG);
        deletebar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        TextView messagetext = (TextView) deletebar.getView().findViewById(android.support.design.R.id.snackbar_text);
        messagetext.setTextColor(Color.BLACK);
        deletebar.setAction("Yes", new deleteListener());
        deletebar.show();
    }

    /**
     * Listens for delete action on snackbar and calls remove action on server
     */
    public class deleteListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            String url = "http://stream.pi:5000/database/remove?filename=" + toDelete;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            snack_message(view, toDelete + " deleted");
                            fetchData();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            snack_message(view, "Error deleting file on server");
                        }
                    }
            );
            queue.add(stringRequest);
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
                        attachmentUri = FileProvider.getUriForFile(this, "edu.umich.globalchallenges.provider", file);
                    }

                    Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
                    openAttachmentIntent.setDataAndType(attachmentUri, downloadMimeType);
                    openAttachmentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        context.startActivity(openAttachmentIntent);
                    } catch (ActivityNotFoundException e) {
                        snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Unable to open file");
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
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                jsonMessage = response.toString();
                rebuildDataSet();
                adapter.updateDataSet(database);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                jsonMessage = "null";
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
                    database.add(new FileItem(databaseMessage.get(i).get(0), databaseMessage.get(i).get(1)));
                }
            } catch (IOException e) { // Here we swallow the exception without doing anything about it
                snack_message(getWindow().getDecorView().findViewById(android.R.id.content), "Error parsing json message");
            }
        }
    }
}
