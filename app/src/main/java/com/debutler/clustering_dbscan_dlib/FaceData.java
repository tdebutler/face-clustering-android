package com.debutler.clustering_dbscan_dlib;

/**
 * By Timothee de Butler on 19.06.2019
 * A simple object used to store the bounding box coordinates of a face
 * and the path of the image in which it has been found
 */
class FaceData {
    private String imagePath;
    private int[] boundingBoxCoordinates;

    String getImagePath() {
        return imagePath;
    }

    void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    int[] getBoundingBoxCoordinates() {
        return boundingBoxCoordinates;
    }

    void setBoundingBoxCoordinates(int[] boundingBoxCoordinates) {
        this.boundingBoxCoordinates = boundingBoxCoordinates;
    }
}
