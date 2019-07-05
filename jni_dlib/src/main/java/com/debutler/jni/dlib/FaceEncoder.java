package com.debutler.jni.dlib;

import android.graphics.Bitmap;
import android.util.Log;


/**
 * By Timothee de Butler on 03.06.2019
 * This class loads the necessary native libraries at startup.
 * This class contains the native methods employed by MainActivity to use the Dlib JNI library.
 */
public class FaceEncoder {

    // Loading JNI libraries
    public FaceEncoder() {
        try {
            System.loadLibrary("c++_shared");
            Log.d("jni", "libc++_shared.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"c++_shared\" not found; check that the correct native " +
                            "libraries are present in the APK.");
        }
        try {
            System.loadLibrary("dlib");
            Log.d("jni", "libdlib.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"dlib\" not found; check that the correct native libraries " +
                            "are present in the APK.");
        }
        String javaLibraryPath = System.getProperty("java.library.path");
        Log.i("JAVA LIBRARY PATH", "FaceEncoder: " + javaLibraryPath);
        try {
            System.loadLibrary("dlib_jni");
            Log.d("jni", "libdlib_jni.so is loaded");
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException(
                    "\"dlib_jni\" not found; check that the correct native " +
                            "libraries are present in the APK.");
        }
    }

    public native float[][] getEncodings(Bitmap bitmap, int[][] faceBoundingBoxes);
    public native void prepareFaceLandmarksDetector();
    public native void prepareFaceEncoderNetwork();

}
