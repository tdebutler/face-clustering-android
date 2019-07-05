package com.debutler.clustering_dbscan_dlib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import com.debutler.clustering_dbscan_dlib.ClustersListActivity;

/**
 * By Timothee de Butler on 25.06.2019
 * This activity displays a grid gallery of every face in the chosen cluster.
 * The display is made through a RecyclerView, managed by the GridRecyclerViewAdapter class.
 */
public class ClusterGalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";
    private static final int NUM_COLUMNS = 3;

    private ArrayList<ArrayList<FaceData>> faceDataClusters = MainActivity.faceDataClusters;
    private ArrayList<FaceData> currentCluster = new ArrayList<>();
    private ArrayList<Bitmap> faces = new ArrayList<>();
    private ArrayList<String> paths = new ArrayList<>();
    private EditText textInputClusterName;
    private ArrayList<String> clusterNames = ClustersListActivity.clustersNames;
    private int CLUSTER_INDEX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_gallery);
        Log.d(TAG, "onCreate: started.");

        textInputClusterName = findViewById(R.id.rename_cluster);

        getIncomingIntent();
        initBitmapFaces();
    }

    @Override
    public void onBackPressed() {
        refreshActivity();
        super.onBackPressed();
    }


    private void getIncomingIntent(){
        Log.d(TAG, "getIncomingIntent: checking for incoming intents");
        if (getIntent().hasExtra("cluster_index")){
            Log.d(TAG, "getIncomingIntent: found intent extra.");

            CLUSTER_INDEX = getIntent().getIntExtra("cluster_index", 0);
            currentCluster = faceDataClusters.get(CLUSTER_INDEX);
        }
    }

    private void initBitmapFaces(){
        Log.d(TAG, "initBitmapFaces: preparing faces.");
        for (FaceData faceData : currentCluster){
            String path = faceData.getImagePath();
            path =  path.replaceAll("%20"," ");
            int[] bbx = faceData.getBoundingBoxCoordinates();

            //To speed up loading of image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;

            Bitmap image = BitmapFactory.decodeFile(path, options);
            Bitmap faceToSet = Bitmap.createBitmap(image, bbx[0], bbx[1], bbx[2]-bbx[0], bbx[3]-bbx[1]);
            faces.add(faceToSet);
            paths.add(path);
        }
        initRecyclerView();
    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: initializing grid recyclerView.");

        RecyclerView gridRecyclerView = findViewById(R.id.gallery_recycler_view);
        GridRecyclerViewAdapter gridRecyclerViewAdapter =
                new GridRecyclerViewAdapter(this, faces, paths);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(NUM_COLUMNS, LinearLayoutManager.VERTICAL);
        gridRecyclerView.setLayoutManager(staggeredGridLayoutManager);
        gridRecyclerView.setAdapter(gridRecyclerViewAdapter);
    }


    public void refreshActivity() {
        Intent i = new Intent(this, ClustersListActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }


    private boolean validateClusterName(){
        String nameInput = textInputClusterName.getText().toString().trim();
        if (nameInput.isEmpty()){
            textInputClusterName.setError("Field cannot be empty");
            return false;
        } else {
            textInputClusterName.setError(null);
            return true;
        }
    }

    public void renameCluster(View view) {
        Log.d(TAG, "renameCluster: called.");
        if (!validateClusterName()) {
            return;
        }
        hideKeyboard(this);
        String input = textInputClusterName.getText().toString();
        Toast.makeText(this, input, Toast.LENGTH_SHORT).show();

        clusterNames.set(CLUSTER_INDEX, input);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) { view = new View(activity); }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }
}
