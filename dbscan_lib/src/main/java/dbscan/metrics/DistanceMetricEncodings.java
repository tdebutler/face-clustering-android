package dbscan.metrics;

import dbscan.DBSCANClusteringException;
import dbscan.DistanceMetric;

/**
 * By Timothee de Butler on 16.05.2019
 * This is an implementation of the DistanceMetric interface, used to perform the DBSCAN clustering
 * algorithm on face encodings, which are 128-element-long float arrays.
 */
public class DistanceMetricEncodings implements DistanceMetric<float[]> {

    @Override
    public double calculateDistance(float[] array1, float[] array2) throws DBSCANClusteringException {

        if (array1.length != 128 || array2.length !=128) {
            throw new DBSCANClusteringException("Encodings should have a length of 128");
        }

        return Math.sqrt(this.dotProduct(array1, array1)
                - 2 * this.dotProduct(array1, array2)
                + this.dotProduct(array2, array2));
    }

    /**
     * Simple dot product between two arrays
     */
    private double dotProduct(float[] array1, float[] array2) {
        double result = 0.0;
        for (int i = 0; i < array1.length; i++) {
            result += array1[i]*array2[i];
        }
        return result;
    }
}
