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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * By Timothee de Butler on 24.06.2019
 * Extends RecyclerView.Adapter.
 * Corresponds to the ClusterListActivity.
 * Adapts list items to the list RecyclerView.
 * Each list item is composed of a face icon (sample of the cluster), and a title (by default, cluster no.)
 */
public class ListRecyclerViewAdapter extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "ListRecyclerViewAdapter";

    private ArrayList<String> listLabels;
    private ArrayList<Bitmap> samples;
    private Context context;

    ListRecyclerViewAdapter(Context context, ArrayList<String> listLabels, ArrayList<Bitmap> samples) {
        this.listLabels = listLabels;
        this.samples = samples;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        Log.d(TAG, "onCreateViewHolder: created. i = " + i);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_list_item,
                                        parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int clusterIndex) {
        Log.d(TAG, "onBindViewHolder: called. clusterIndex = " + clusterIndex);

        holder.sample.setImageBitmap(samples.get(clusterIndex));
        
        holder.clusterLabel.setText(listLabels.get(clusterIndex));

        holder.parentLayout.setOnClickListener(v -> {
            Log.d(TAG, "onClick: clicked on: " + listLabels.get(clusterIndex));
            Toast.makeText(context, listLabels.get(clusterIndex), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(context, ClusterGalleryActivity.class);
            intent.putExtra("cluster_index", clusterIndex);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: count is " + listLabels.size());
        return listLabels.size();  // how many items will be displayed
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        CircleImageView sample;
        TextView clusterLabel;
        RelativeLayout parentLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            sample = itemView.findViewById(R.id.sample);
            clusterLabel = itemView.findViewById(R.id.cluster_label);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
