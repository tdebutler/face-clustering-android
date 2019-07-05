package com.debutler.clustering_dbscan_dlib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.apache.commons.math3.distribution.LogisticDistribution;

import java.util.ArrayList;


/**
 * By Timothee de Butler on 23.06.2019
 * This activity displays a list of the clusters.
 * Each cluster is displayed as a round icon of the first face of the cluster and a name
 * The display is made through a RecyclerView, managed by the ListRecyclerViewAdapter class.
 */
public class ClustersListActivity extends AppCompatActivity {

    private static final String TAG = "CLUSTER LIST ACTIVITY";

    // variables
    private ArrayList<ArrayList<FaceData>> faceDataClusters = MainActivity.faceDataClusters;
    private ArrayList<Bitmap> bitmapSamples = new ArrayList<>();
    static ArrayList<String> clustersNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clusters_view);

        getIncomingIntent();
        initBitmapSamples();
    }


    /**
     * Initializes the samples array, taking the first face of every cluster.
     * Initializes also the encodingsClusters names array.
     */
    private void initBitmapSamples(){
        Log.d(TAG, "initBitmapSamples: preparing samples.");

        if (clustersNames.isEmpty()) {
            int i = 0;
            for (ArrayList<FaceData> cluster : faceDataClusters) {
                FaceData sampleData = cluster.get(0);
                String samplePath = sampleData.getImagePath();
                int[] sampleBbx = sampleData.getBoundingBoxCoordinates();

                //To speed up loading of image
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;

                Bitmap img = BitmapFactory.decodeFile(samplePath, options);
                Bitmap sample = Bitmap.createBitmap(img,
                        sampleBbx[0], sampleBbx[1], sampleBbx[2] - sampleBbx[0], sampleBbx[3] - sampleBbx[1]);

                bitmapSamples.add(i, sample);
                clustersNames.add(i, "CLUSTER no. " + (i + 1));
                i++;
            }
        } else {
            int i = 0;
            for (ArrayList<FaceData> cluster : faceDataClusters) {
                Log.d(TAG, "initBitmapSamples: clusterName = " + clustersNames.get(i));
                FaceData sampleData = cluster.get(0);
                String samplePath = sampleData.getImagePath();
                int[] sampleBbx = sampleData.getBoundingBoxCoordinates();

                //To speed up loading of image
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;

                Bitmap img = BitmapFactory.decodeFile(samplePath, options);
                Bitmap sample = Bitmap.createBitmap(img,
                        sampleBbx[0], sampleBbx[1], sampleBbx[2] - sampleBbx[0], sampleBbx[3] - sampleBbx[1]);

                bitmapSamples.add(i, sample);
                i++;
            }
        }
        initRecyclerView();
    }

    /**
     * Prepares the RecyclerView to display the list of clusters
     */
    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: initializing RecyclerView");
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        ListRecyclerViewAdapter adapter = new ListRecyclerViewAdapter(this, clustersNames, bitmapSamples);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getIncomingIntent(){
        Log.d(TAG, "getIncomingIntent: checking for incoming intents");
        if (getIntent().hasExtra("nb_of_clusters")){
            Log.d(TAG, "getIncomingIntent: found intent extra.");
            int NB_OF_CLUSTERS = getIntent().getIntExtra("nb_of_clusters", 0);
        }
    }
}
