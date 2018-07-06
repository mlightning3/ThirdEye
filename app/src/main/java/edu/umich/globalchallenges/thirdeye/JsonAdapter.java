package edu.umich.globalchallenges.thirdeye;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonAdapter extends RecyclerView.Adapter<JsonAdapter.ViewHolder> {
    private List<List<String>> database;
    public String jsonMessage;

    /**
     * Class for handling the view of the data we want in our list
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ViewHolder(TextView view) {
            super(view);
            textView = view;
        }
    }

    /**
     * Constructs a JsonAdapter. This does the job of making the request to the server to get the
     * database listing as a json message
     * @param queue A RequestQueue from the activity using this that we can add our request to
     */
    public JsonAdapter(RequestQueue queue) {
        jsonMessage = "null";
        database = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        temp.add("Nothing");
        temp.add("here");
        database.add(temp);
        fetchData(queue);
        updateDataSet();
    }

    /**
     * Creates new views, as invoked by the layout manager
     * @param parent
     * @param type
     * @return An empty ViewHolder
     */
    @Override
    public JsonAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    /**
     * Replaces the contents of a view, as invoked by the layout manager
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String message = database.get(position).get(0) + " " + database.get(position).get(1);
        holder.textView.setText(message);
    }

    /**
     * Gives the size of the dataset for the layout manager
     * @return number of items to be placed into view
     */
    @Override
    public int getItemCount() {
        return database.size();
    }

    /**
     * Reparses the JSON message and builds new database
     */
    public void updateDataSet() {
        if(!jsonMessage.contentEquals("null")) { // Only try to parse the message if we actually got something
            JsonDatabaseParser databaseParser = new JsonDatabaseParser();
            try {
                database = databaseParser.genDatabaseArray(jsonMessage);
            } catch (IOException e) { // Here we swallow the exception without doing anything about it
                //database = null;
            }
        }
    }

    /**
     * Fetches the data from the server and parses the data before reloading activity view
     * @param queue Request queue from activity that we can add to
     */
    public void fetchData(RequestQueue queue) {
        String url = "http://stream.pi:5000/get_database";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                jsonMessage = response.toString();
                updateDataSet();
                notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                jsonMessage = "null";
            }
        });
        queue.add(jsonArrayRequest);
    }
}
