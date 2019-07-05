package com.debutler.clustering_dbscan_dlib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * By Timothee de Butler on 25.06.2019
 * This activity displays the whole image containing the selected face in the Cluster Gallery
 */
public class ImageDisplayActivity extends AppCompatActivity {

    private static final String TAG = "Image Display Activity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);
        Log.d(TAG, "onCreate: started.");

        getIncomingIntent();
    }

    private void getIncomingIntent(){
        Log.d(TAG, "getIncomingIntent: checking for incoming intents");
        if (getIntent().hasExtra("face_image_path")){
            Log.d(TAG, "getIncomingIntent: found intent extra.");

            String faceImagePath = getIntent().getStringExtra("face_image_path");
            setImage(faceImagePath);
        }
    }

    private void setImage(String imgPath){
        Log.d(TAG, "setImage: setting image to display on screen");

        String txtToSet = "Path is : " + imgPath;

        imgPath =  imgPath.replaceAll("%20"," ");

        //To speed up loading of image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        Bitmap imgToSet = BitmapFactory.decodeFile(imgPath, options);

        TextView name = findViewById(R.id.img_description);
        name.setText(txtToSet);
        ImageView sampleDisplay = findViewById(R.id.image);
        sampleDisplay.setImageBitmap(imgToSet);
    }


}
