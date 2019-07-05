#include <jni.h>
#include <string>
#include <android/log.h>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "native-lib:", __VA_ARGS__))

#include <iostream>
#include <fstream>
#include <string>

#include<cstdio>

#include <iostream>
#include <fstream>
using namespace std;

int test () {
    LOGI("Method started");
    ifstream myfile;
    myfile.open("/storage/emulated/0/Download/test.txt");
    string line;
    if (myfile.is_open()) {
        LOGI("File is open !");
        while (getline(myfile, line)) {
            LOGI("%s", line.c_str());
        }
        myfile.close();
    } else LOGI("Unable to open file for reading");
    return 0;
}


extern "C" JNIEXPORT void JNICALL
Java_com_debutler_clustering_1dbscan_1dlib_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    LOGI("Using native-lib for test");
    test();
    LOGI("Leaving native-lib");
}