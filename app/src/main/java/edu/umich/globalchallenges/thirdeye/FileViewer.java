package edu.umich.globalchallenges.thirdeye;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.DividerItemDecoration;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class FileViewer extends AppCompatActivity {

    private RecyclerView recyclerView;
    //private RecyclerView.Adapter adapter;
    private FlexibleAdapter<IFlexible> adapter;
    private LinearLayoutManager layoutManager;

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
        RequestQueue queue = Volley.newRequestQueue(this);
        /*
        adapter = new JsonAdapter(queue);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                layoutManager.getOrientation()));
        */
        adapter = new FlexibleAdapter<>(database);
        recyclerView.setAdapter(adapter);
    }
}
