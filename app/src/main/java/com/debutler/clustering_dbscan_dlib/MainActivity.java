package com.debutler.clustering_dbscan_dlib;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.debutler.jni.dlib.FaceEncoder;

import org.apache.commons.math3.distribution.LogisticDistribution;

import dbscan.DBSCANClusterer;
import dbscan.DBSCANClusteringException;
import dbscan.DistanceMetric;
import dbscan.metrics.DistanceMetricEncodings;

/**
 * By Timothee de Butler on 28.05.2019
 * After getting a text file shared by the Face Detector App, containing bounding boxes coordinates
 * of faces and the corresponding image paths, this Activity is used to :
 * 1- store the obtained data in a SparseArray through ImageData objects
 * 2- convert each face into a 128-element encoding using the JNI library Dlib and pre-trained Dlib models
 * 3- run a clustering algorithm on the encodings (using the DBSCAN lightweight library)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 2;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private FaceEncoder mFaceEncoder = new FaceEncoder();
    private DBSCANClusterer<float[]> mDBSCANClusterer;

    static DistanceMetric<float[]> mMetric = new DistanceMetricEncodings();
    static ArrayList<ArrayList<FaceData>> faceDataClusters = new ArrayList<>();

    private TextView showBoundingBoxes;
    private TextView showProgressInfo;
    private String bbxText = "";
    private SparseArray<ImageData> bbxMap = new SparseArray<>();
    private ArrayList<float[]> encodingsToCluster = new ArrayList<>();
    private ArrayList<ArrayList<float[]>> encodingsClusters = new ArrayList<>();

    private boolean isDataAvailable = false, isDataMapped = false, areEncodingsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showBoundingBoxes = findViewById (R.id.bbx_info_textView);
        showBoundingBoxes.setMovementMethod(new ScrollingMovementMethod());

        showProgressInfo = findViewById(R.id.progress_info);

        Button viewClusterBtn = findViewById(R.id.view_clusters_btn);
        viewClusterBtn.setOnClickListener(view -> {
            Intent viewClustersIntent = new Intent(MainActivity.this, ClustersListActivity.class);
            viewClustersIntent.putExtra("nb_of_clusters",faceDataClusters.size());
            startActivity(viewClustersIntent);
        });

        verifyPermissions();    // verifying if access to external storage is granted

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // handle text being sent
            }
        }
    }


    public native void stringFromJNI();


    /** Handles the bounding boxes info shared by the Face Detection App */
    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            showBoundingBoxes.setText(sharedText);
            bbxText = sharedText;
            isDataAvailable = true;
        }
    }

    /**
     * For every image, creates an ImageData object with image path and bbx coordinates
     * of every face in the image (faces are detected by the Face Detection App).
     * Then stores this object into a SparseArray
     */
    @SuppressLint("SetTextI18n")
    public void mapBoundingBoxesText(View v) {
        Log.d(TAG, "mapBoundingBoxesText: started.");
        if (isDataAvailable) {
            String[] lineSplit = bbxText.split("\\n");

            // Initialization (i=1 because first element is empty)
            String[] spaceSplitInit = lineSplit[1].split("\\s"); // first element is empty
            int[] bbxInit = new int[4];
            for (int j = 1; j < spaceSplitInit.length; j++) { // first element is path
                bbxInit[j - 1] = Integer.parseInt(spaceSplitInit[j]);
            }
            ImageData imageDataInit = new ImageData();              // image data object
            String imagePathInit =  spaceSplitInit[0];              // retrieving the image path
            imagePathInit = imagePathInit.replaceAll("%20"," ");  // manage whitespaces
            imageDataInit.setImagePath(imagePathInit);              // adding img path to the data
            ArrayList<int[]> faceListInit = new ArrayList<>();      // list of different faces bbx
            faceListInit.add(bbxInit);                              // adding bbx coord. of 1st face
            imageDataInit.setBoundingBoxCoordinates(faceListInit);  // adding the bbx list to the data
            bbxMap.put(0, imageDataInit);                           // putting data in the SparseArray
            int lastMapKey = 0;

            // looping over the detected faces
            for (int i = 2; i < lineSplit.length; i++) {

                String[] spaceSplit = lineSplit[i].split("\\s");
                String imagePath = spaceSplit[0];
                imagePath =  imagePath.replaceAll("%20"," ");
                int[] bbx = new int[4];
                for (int j = 1; j < spaceSplit.length; j++) {         // first element is path
                    bbx[j - 1] = Integer.parseInt(spaceSplit[j]);     // adding bbx coord. of new face
                }

                // checking if this face is in the same image as the previous one
                if (imagePath.equals(bbxMap.get(lastMapKey).getImagePath())) {
                    ImageData imageData = bbxMap.get(lastMapKey);     // getting the already existing data
                    ArrayList<int[]> faceList = imageData.getBoundingBoxCoordinates();
                    faceList.add(bbx);                              // adding new face bbx coord.
                    imageData.setBoundingBoxCoordinates(faceList);
                    bbxMap.put(lastMapKey, imageData);               // updating data in the SparseArray
                } else { // new image
                    ImageData imageData = new ImageData();           // creating data object for new img
                    imageData.setImagePath(imagePath);
                    ArrayList<int[]> faceList = new ArrayList<>();
                    faceList.add(bbx);                               // bbx coord. of 1st face in new img
                    imageData.setBoundingBoxCoordinates(faceList);
                    bbxMap.put(lastMapKey + 1, imageData);           // adding new data in the map
                    lastMapKey++;
                }
            }
            Log.d(TAG, "mapBoundingBoxesText: length of SparseArray is " + bbxMap.size());
            showProgressInfo.setText("Data mapped.");
            isDataMapped = true;
        } else {
            Toast.makeText(getBaseContext(), "No data available. Use Face Detection App first", Toast.LENGTH_LONG).show();
        }
    }


    /**
     *  Loads the face landmarks detector model and the Dlib face recognition residual network model.
     *  For every face in every image, uses the native Dlib library via JNI to create
     *  a 128-float-element representation (encoding, or vector) of the face and its landmarks
     */
    @SuppressLint("SetTextI18n")
    public void prepareEncodings() {

        Log.d(TAG, "prepareEncodings: started.");

        stringFromJNI(); // for debugging purposes

        if (isDataMapped) {
            long startTime = System.nanoTime(); // profiling purposes

            // Loading pre-trained Dlib models
            runOnUiThread(() -> showProgressInfo.setText("Loading landmarks detector ..."));
            mFaceEncoder.prepareFaceLandmarksDetector();
            long landmarksDetectorLoadingTime = System.nanoTime() - startTime;
            long interTime = System.nanoTime();
            runOnUiThread(() -> showProgressInfo.setText("Loading encoder network ..."));
            mFaceEncoder.prepareFaceEncoderNetwork();
            long encoderNetLoadingTime = System.nanoTime() - interTime;

            // looping over every image
            for (int i = 0; i < bbxMap.size(); i++) {
                ImageData imgData = bbxMap.get(i);
                String imgPath = imgData.getImagePath();  // path to the image in the device.
                File imgFile = new File(imgPath);
                int[][] boundingBoxes = convertIntArrayList(imgData.getBoundingBoxCoordinates()); // bounding boxes list to pass to JNI.
                ArrayList<String> toStrings = new ArrayList<>();
                if (imgFile.exists()) {
                    Bitmap mBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    Log.d(TAG, "prepareEncodings: Bitmap reference is " + mBitmap.toString());
                    Log.d(TAG, "prepareEncodings: BBX Array is " + Arrays.deepToString(boundingBoxes)
                            + " of length " + boundingBoxes.length);
                    float[][] encodings = mFaceEncoder.getEncodings(mBitmap, boundingBoxes);

                    // Adding every encoding to the list, and every toString reference to the Image Data
                    for (float[] encoding : encodings) {
                        Log.d(TAG, "prepareEncodings: encoding is " + Arrays.toString(encoding));
                        toStrings.add(Arrays.toString(encoding));
                        encodingsToCluster.add(encoding);
                    }
                    imgData.setEncodingsToString(toStrings);
                    bbxMap.put(i, imgData); // putting updated data with toStrings.
                    Log.i(TAG, "nb of faces encoded :" + encodingsToCluster.size());
                    int finalI = i;
                    runOnUiThread(() -> showProgressInfo.setText("Images processed : "+(finalI +1)+" out of "+bbxMap.size()));
                } else {
                    Log.e(TAG,
                            "IMAGE NOT FOUND - path is: " + imgPath + ", index is: " + i);
                }
            }
            int totalEncodingsNb = encodingsToCluster.size();
            long elapsedTime = System.nanoTime() - startTime;
            Log.d(TAG, "prepareEncodings: encodings performed.");
            String timeInfo = "Encoding performed. " +
                    "\nLoading landmarks detector took " + landmarksDetectorLoadingTime / 1000000 + " ms. " +
                    "\nLoading encoder network took " + encoderNetLoadingTime / 1000000 + " ms. " +
                    "\nGetting the encoding of one face took on average " + (elapsedTime - encoderNetLoadingTime) / (1000000 * totalEncodingsNb) + " ms.";
            Log.i(TAG, "prepareEncodings: " + timeInfo);
            runOnUiThread(() -> showProgressInfo.setText(timeInfo));
            areEncodingsReady = true;
            runOnUiThread(() -> Toast.makeText(getBaseContext(), "Encodings performed", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(getBaseContext(), "Data has not been mapped.", Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Clusters the face encodings using the DBSCAN clustering algorithm
     * An encoding is a 128-float-element representation of a face's landmarks
     */
    @SuppressLint("SetTextI18n")
    public void performClustering(View v) {

        Log.d(TAG, "performClustering: started.");

        if (areEncodingsReady) {
            long startTime = System.nanoTime(); // profiling purposes
            try {
                double maxDistance = 0.43; // Maximum distance of elements to consider clustered
                int minNumElements = 2; // Minimum number of elements to constitute cluster
                // DBSCAN Clusterer Instance from DBSCAN Library
                mDBSCANClusterer = new DBSCANClusterer<>(encodingsToCluster, minNumElements, maxDistance, mMetric);
                encodingsToCluster.clear();
                Log.d(TAG, "performClustering: Clusterer loaded.");
            } catch (DBSCANClusteringException e) {
                e.printStackTrace();
            }
            try {
                // Creating encodingsClusters with DBSCAN algorithm
                encodingsClusters = mDBSCANClusterer.performClustering();
            } catch (DBSCANClusteringException e) {
                e.printStackTrace();
            }

            fillFaceDataClusters(); // now we have the encodings clusters, we create clusters of FaceData objects.

            long elapsedTime = System.nanoTime() - startTime;
            Log.d(TAG, "performClustering: took " + elapsedTime / 1000000 + " ms.");
            showProgressInfo.setText(encodingsClusters.size() + " clusters generated.");
            //Toast.makeText(getBaseContext(), encodingsClusters.size() + " clusters generated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getBaseContext(), "Faces have not been encoded yet.", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Reads the encodings clusters ArrayList, finds the corresponding face data for each encoding,
     * and adds the Face Data objects to the new FaceData clusters ArrayList.
     */
    public void fillFaceDataClusters(){

        int i = 0;
        // looping over every encodings cluster.
        for (ArrayList<float[]> cluster : encodingsClusters) {
            i++;
            Log.d(TAG, "fillFaceDataClusters: cluster no." + i + " of length " + cluster.size());
            ArrayList<FaceData> clusterOfFaceData = new ArrayList<>();

            // looping over every encoding in the cluster.
            for (float[] encoding : cluster) {
                clusterOfFaceData.add(findCorrespondingFace(encoding));
            }
            faceDataClusters.add(clusterOfFaceData);
        }
    }


    /**
     * Given an already clustered face encoding, looks over every ImageData object until it finds
     * the corresponding face, using the toString identifier of the encoding.
     * Returns its bounding box coordinates as well as the image path.
     * @param encoding : a 128-float-element representations of a face's landmarks
     * @return a FaceData object containing the bounding box coordinates of the face in the image
     * as well as the image path
     */
    public FaceData findCorrespondingFace(float[] encoding){

        String strIdentifier = Arrays.toString(encoding);
        FaceData out = new FaceData();

        // looping over every ImageData object
        for (int i=0; i<bbxMap.size(); i++) {
            ImageData imgData = bbxMap.get(i);
            String path = imgData.getImagePath();
            ArrayList<int[]> boundingBoxes = imgData.getBoundingBoxCoordinates();

            // looping over every face in this image
            for (int j=0; j<boundingBoxes.size(); j++){
                int[] bbx = boundingBoxes.get(j);
                String id = imgData.getEncodingsToString().get(j);
                if (strIdentifier.equals(id)) {
                    out.setBoundingBoxCoordinates(bbx);
                    out.setImagePath(path);
                    return out;
                }
            }
        }
        Log.e(TAG, "findCorrespondingFace: no corresponding face found");
        return null;
    }


    /** checks if the app has access to external storage at startup. */
    private void verifyPermissions(){
        Log.d(TAG, "verifyPermissions: asking user for permissions");
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "verifyPermissions: permission already granted.");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        verifyPermissions();
    }


    /**
     * Simply converts an ArrayList of int[] into a int[][]
     * @param arrayList the arrayList to convert
     * @return out the converted int[][]
     */
    public int[][] convertIntArrayList(ArrayList<int[]> arrayList){
        int[][] out = new int[arrayList.size()][];
        for(int i=0; i<arrayList.size(); i++){
            out[i]=arrayList.get(i);
        }
        return out;
    }


    /**
     * launches the prepareEncodings function as an Async Task in order to dynamically display
     * profiling info on the UI.
     */
    public void launchEncodingAsyncTask(View view) {
        new PrepareEncodingsAsync().execute();
    }
    private class PrepareEncodingsAsync extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            prepareEncodings();
            return null;
        }
    }
}