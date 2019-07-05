package com.debutler.clustering_dbscan_dlib;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * By Timothee de Butler on 26.06.2019
 * Extends RecyclerView.Adapter.
 * Corresponds to the ClusterGalleryActivity.
 * Adapts grid items to the Grid RecyclerView.
 * The grid is composed of every cropped face belonging to the cluster
 */
public class GridRecyclerViewAdapter extends RecyclerView.Adapter<GridRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "GridRecyclerViewAdapter";

    private ArrayList<Bitmap> faces;
    private ArrayList<String> paths;
    private Context context;

    public GridRecyclerViewAdapter(Context context, ArrayList<Bitmap> faces, ArrayList<String> paths) {
        this.faces = faces;
        this.paths = paths;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.layout_grid_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int faceIndex) {
        Log.d(TAG, "onBindViewHolder: called.");

        holder.faceImage.setImageBitmap(faces.get(faceIndex));

        holder.faceImage.setOnClickListener(v -> {
            Log.d(TAG, "onClick: clicked on: " + faces.get(faceIndex).toString());
            Toast.makeText(context, "image path is " + paths.get(faceIndex), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(context, ImageDisplayActivity.class);
            intent.putExtra("face_image_path", paths.get(faceIndex));
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return faces.size();    // how many items will be displayed
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView faceImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.faceImage = itemView.findViewById(R.id.grid_image);
        }
    }
}
