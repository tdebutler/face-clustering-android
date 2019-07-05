package com.debutler.clustering_dbscan_dlib;

import java.util.ArrayList;

/**
 * By Timothee de Butler on 05.06.2019
 * A ImageData object is created for every image.
 * It contains the path of the image, as well as an ArrayList of bounding boxes.
 * Each bounding box represents a frame around a face contained in the image.
 */
class ImageData {

    private String imagePath;
    private ArrayList<int[]> boundingBoxCoordinates;
    /*
    i = 0 : top-left x (left)
    i = 1 : top-left y (top)
    i = 2 : bottom-right x (right)
    i = 3 : bottom-right y (bottom)
     */
    private ArrayList<String> encodingsToString; // each element is the toString of the face corresponding to the same index in the bounding boxes ArrayList.
    // length of boundingBoxCoordinates == length of encodingsToString

    String getImagePath() {
        return imagePath;
    }

    void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    ArrayList<int[]> getBoundingBoxCoordinates() {
        return boundingBoxCoordinates;
    }

    void setBoundingBoxCoordinates(ArrayList<int[]> boundingBoxCoordinates) {
        this.boundingBoxCoordinates = boundingBoxCoordinates;
    }

    public ArrayList<String> getEncodingsToString() {
        return encodingsToString;
    }

    public void setEncodingsToString(ArrayList<String> encodingsToString) {
        this.encodingsToString = encodingsToString;
    }
}
