# Face Clustering Praktikumsprojekt

This is a simple face clustering app using the open-source Dlib native library.
It was developed as an internship project.

This app must be used hand in hand with the [Face Detection app](https://evcs.photoprintit.lan/maic/face-detection-praktikumsprojekt).

## Prerequisites

This project uses the Dlib library as a JNI shared library.
Dlib is a C++ library that needs to be cross-compiled as a shared library of the desired ABI.
A tutorial can be found [there](https://tech.pic-collage.com/face-landmarks-detection-in-your-android-app-part-2-ae049a4ac0d1).

All the functional native code is found in [jni_lib/src/main/cpp](https://evcs.photoprintit.lan/maic/face-clustering-praktikumsprojekt/tree/master/jni_dlib/src/main/cpp).
JNI libs are loaded and JNI functions are called in the [FaceEncoder](https://evcs.photoprintit.lan/maic/face-clustering-praktikumsprojekt/blob/master/jni_dlib/src/main/java/com/debutler/jni/dlib/FaceEncoder.java) class.

## Usage

This app works hand in hand with the [Face Detection app](https://evcs.photoprintit.lan/maic/face-detection-praktikumsprojekt).
After having detected the faces, the latter can share a text file containing bounding boxes info to the Face Clustering app.



