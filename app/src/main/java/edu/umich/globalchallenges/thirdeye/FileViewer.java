package edu.umich.globalchallenges.thirdeye;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.Snackbar;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter.Mode;
import eu.davidea.flexibleadapter.items.IFlexible;

public class FileViewer extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FlexibleAdapter<IFlexible> adapter;
    private LinearLayoutManager layoutManager;
    private RequestQueue queue;
    private String jsonMessage;
    private List<List<String>> databaseMessage;
    private List<IFlexible> database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    @Override
    public boolean onItemClick(View view, int position) {
        // Build needed strings
        String filename = adapter.getItem(position).toString();
        filename = filename.substring(filename.indexOf(" ") + 1, filename.length());
        String url = "http://stream.pi:5000/media/" + filename;
        // Set up download and queue it up
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
            } catch (IOException e) { // Here we swallow the exception without doing anything about it
                //snack_message(view, "Error parsing json message");
                return;
            }
            database = new ArrayList<>();
            for(int i = 0; i < databaseMessage.size(); i++) {
                database.add(new FileItem(databaseMessage.get(i).get(0), databaseMessage.get(i).get(1)));
            }
        }
    }
}
