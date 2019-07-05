//
// Created by Timothee de Butler on 28.05.2019.
//

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <dlib/image_processing.h>
#include <dlib/image_io.h>
#include <dlib/dnn.h>
#include <my/jni.h>
#include <my/profiler.h>
#include <iterator>
#include <iostream>
#include <fstream>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "dlib-jni:", __VA_ARGS__))

using namespace dlib;
using namespace std;


// ResNet NETWORK //////////////////////////////////////////////////////////////////////////////////

/// A residual neural network that will be used with the pre-trained dlib_face_recognition_resnet_v1
/// model to create 128-element encodings (vectors) of every face.
/// Input : a "face chip", an isolated and orientated rgb matrix representation of a face.
/// Output : a vector of 128 float elements

template <template <int,template<typename>class,int,typename> class block, int N, template<typename>class BN, typename SUBNET>
using residual = add_prev1<block<N,BN,1,tag1<SUBNET>>>;

template <template <int,template<typename>class,int,typename> class block, int N, template<typename>class BN, typename SUBNET>
using residual_down = add_prev2<avg_pool<2,2,2,2,skip1<tag2<block<N,BN,2,tag1<SUBNET>>>>>>;

template <int N, template <typename> class BN, int stride, typename SUBNET>
using block  = BN<con<N,3,3,1,1,relu<BN<con<N,3,3,stride,stride,SUBNET>>>>>;

template <int N, typename SUBNET> using ares      = relu<residual<block,N,affine,SUBNET>>;
template <int N, typename SUBNET> using ares_down = relu<residual_down<block,N,affine,SUBNET>>;

template <typename SUBNET> using alevel0 = ares_down<256,SUBNET>;
template <typename SUBNET> using alevel1 = ares<256,ares<256,ares_down<256,SUBNET>>>;
template <typename SUBNET> using alevel2 = ares<128,ares<128,ares_down<128,SUBNET>>>;
template <typename SUBNET> using alevel3 = ares<64,ares<64,ares<64,ares_down<64,SUBNET>>>>;
template <typename SUBNET> using alevel4 = ares<32,ares<32,ares<32,SUBNET>>>;

using anet_type = loss_metric<fc_no_bias<128,avg_pool_everything<
        alevel0<
                alevel1<
                        alevel2<
                                alevel3<
                                        alevel4<
                                                max_pool<3,3,2,2,relu<affine<con<32,7,7,2,2,
                                                        input_rgb_image_sized<150>
                                                >>>>>>>>>>>>;


// LOCAL ///////////////////////////////////////////////////////////////////////////////////////////

static shape_predictor sFaceLandmarksDetector;
static anet_type sFaceEncoderNet;

/**
 * Checks if a file exists or is accessible
 */
bool fexists(const char *filename)
{
    std::ifstream ifile(filename);
    return (bool)ifile;
}


/**
 * Converts a bitmap image into a 2D RGB pixels array
 */
void convertBitmapToArray2d(JNIEnv* env,
                            jobject bitmap,
                            array2d<rgb_pixel>& out) {
    LOGI("NATIVE FUNCTION RUNNING : convertBitmapToArray2d");

    // Profiler
    Profiler profiler;
    profiler.start();

    AndroidBitmapInfo bitmapInfo;
    void* pixels;
    int state;

    if (0 > (state = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo))) {
        LOGI("L%d: AndroidBitmap_getInfo() failed! error=%d", __LINE__, state);
        throwException(env, "AndroidBitmap_getInfo() failed!");
        return;
    } else if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGI("L%d: Bitmap format is not RGB_565!", __LINE__);
        throwException(env, "Bitmap format is not RGB_565!");
    }

    // Lock the bitmap for copying the pixels safely.
    if (0 > (state = AndroidBitmap_lockPixels(env, bitmap, &pixels))) {
        LOGI("L%d: AndroidBitmap_lockPixels() failed! error=%d", __LINE__, state);
        throwException(env, "AndroidBitmap_lockPixels() failed!");
        return;
    }

    LOGI("L%d: info.width=%d, info.height=%d", __LINE__, bitmapInfo.width, bitmapInfo.height);
    out.set_size((long) bitmapInfo.height, (long) bitmapInfo.width);

    char* line = (char*) pixels;
    for (int h = 0; h < bitmapInfo.height; ++h) {
        for (int w = 0; w < bitmapInfo.width; ++w) {
            auto * color = (uint32_t*) (line + 4 * w);

            out[h][w].red = (unsigned char) (0xFF & *color >> 24);
            out[h][w].green = (unsigned char) (0xFF & *color >> 16);
            out[h][w].blue = (unsigned char) (0xFF & *color >> 8);
        }

        line = line + bitmapInfo.stride;
    }
    // Unlock the bitmap.
    AndroidBitmap_unlockPixels(env, bitmap);

    double interval = profiler.stopAndGetInterval();

    LOGI("L%d: Bitmap has been converted (took %.3f ms)", __LINE__, interval);
    LOGI("LEAVING NATIVE FUNCTION : convertBitmapToArray2d");
}


/**
 * With a Dlib full_object_detection object, detects the main components (landmarks) of each face
 * and returns a vector of rgb_pixel_matrix representing isolated and well orientated faces.
 * dlib::full_object_detection :
 *          - This object represents the location of face in an image along with the positions
 *            of each of its constituent parts.
 *          - It is generated by the shape_predictor model, which detects and annotates the position
 *            of 68 landmarks on each face.
 * dlib::get_face_chip_details :
 *          - This function assumes the input contains a human face detection with face parts
 *            annotated with a shape_predictor.  Given these assumptions, it creates a
 *            chip_details object that will extract a copy of the face that has been
 *            rotated upright, centered, and scaled to a standard size when given to
 *            extract_image_chip().
 *          - This function is specifically calibrated to work with
 *            the shape_predictor_68_face_landmarks.dat model.
 *          - The extracted chips will have size rows and columns in them.
 * dlib::extract_image_chips :
 *          - This function extracts "chips" from an image.  That is, it takes a list of
 *            rectangular sub-windows (i.e. chips) within an image and extracts those
 *            sub-windows, storing each into its own image.  It also scales and rotates the
 *            image chips according to the instructions inside each chip_details object.
 *            It uses the interpolation method supplied as a parameter.
 *          - Any pixels in an image chip that go outside img are set to 0 (i.e. black).
 *
 * @param bitmap : given image potentially containing faces, to be converted into a 2D pixels array
 * @param faceLocations : vector of rectangles demarcating each face
 * @return faceChips : a vector of RGB pixel matrix representations of each face
 */
std::vector<matrix<rgb_pixel>> detectLandmarksFromFaces(JNIEnv *env,
                              jobject bitmap,
                              std::vector<rectangle> faceLocations) {
    LOGI("NATIVE FUNCTION RUNNING : detectLandmarksFromFace");

    // Profiler
    Profiler profiler;

    // Convert bitmap to dlib::array2d.
    array2d<rgb_pixel> img;
    convertBitmapToArray2d(env, bitmap, img);

    unsigned long nb_faces = faceLocations.size();

    std::vector<matrix<rgb_pixel>> faceChips;
    for (int i = 0; i < nb_faces; ++i) {
        profiler.start();
        LOGI("Processing face no.%d of %ld", i+1, nb_faces);
        rectangle faceLocation = faceLocations[i];

        // Detect landmarks : shape represents the location of the face along with the positions
        // of each of its constituent parts
        full_object_detection shape = sFaceLandmarksDetector(img, faceLocation);

        // store cropped face
        matrix<rgb_pixel> faceChip;
        extract_image_chip(img, get_face_chip_details(shape, 150, 0.25), faceChip);
        faceChips.push_back(move(faceChip));

        double interval = profiler.stopAndGetInterval();
        LOGI("L%d: Face landmarks detected and face chip created (took %.3f ms)", __LINE__, interval);
    }
    LOGI("Number of face chips created : %d", faceChips.size());
    LOGI("LEAVING NATIVE FUNCTION: detectLandmarksFromFace");
    return faceChips;
}


/**
 * Encodes each face in the given image into a 128-long float array with the Dlib ResNet model.
 * The more similar the faces, the smaller the Euclidean distance between the corresponding encodings
 *
 * @param bitmap : given image potentially containing faces
 * @param faceBoundingBoxes : list of float arrays of 4 points, each one representing the rectangle bounding box demarcating a face
 * @return faceDescriptors : a vector of 128-long float arrays, each one being an encoding of a face
 */
std::vector<matrix<float,0,1>> performEncodings(JNIEnv *env, jobject instance,
                                                jobject bitmap,
                                                jobjectArray faceBoundingBoxes) {

    LOGI("NATIVE FUNCTION RUNNING : performEncodings");

    Profiler profiler;

    ///////// Structure the data from JNI /////////
    // Loading the bounding boxes array
    int nb_faces = env->GetArrayLength(faceBoundingBoxes);
    auto dim = (jintArray) env->GetObjectArrayElement(faceBoundingBoxes, 0);
    int bbx_len = env->GetArrayLength(dim);
    int **localFaceBoundingBoxes;
    // allocate faces using nb_faces
    localFaceBoundingBoxes = new int *[nb_faces];
    for (int i = 0; i < nb_faces; ++i) {
        auto oneDim = (jintArray) env->GetObjectArrayElement(faceBoundingBoxes, i);
        jint *bbox = env->GetIntArrayElements(oneDim, nullptr);
        // allocate bbox using bbx_len
        localFaceBoundingBoxes[i] = new int[bbx_len];
        for (int j = 0; j < bbx_len; ++j) {
            localFaceBoundingBoxes[i][j] = bbox[j];
        }
    }
    // Convert bounding boxes to Dlib rectangles - store them in a std::vector
    std::vector<rectangle> faceLocations;
    for (int i = 0; i < nb_faces; ++i) {
        LOGI("Converting bounding box no. %d of %d to dlib rectangle", i+1, nb_faces);
        rectangle faceLocation = (rectangle((long) localFaceBoundingBoxes[i][0],
                                            (long) localFaceBoundingBoxes[i][1],
                                            (long) localFaceBoundingBoxes[i][2],
                                            (long) localFaceBoundingBoxes[i][3]));
        faceLocations.push_back(faceLocation);
    }

    ///////// Isolate and orientate each face with help of landmarks detection /////////
    std::vector<matrix<rgb_pixel>> faceChips = detectLandmarksFromFaces(env, bitmap, faceLocations);

    ///////// Encode each face as a 128-element long vector /////////
    LOGI("Encoding the faces ...");
    profiler.start();
    std::vector<matrix<float,0,1>> faceDescriptors = sFaceEncoderNet(faceChips);
    double interval = profiler.stopAndGetInterval();
    LOGI("L%d: Faces successfully encoded (took %.3f ms)", __LINE__, interval);

    LOGI("LEAVING NATIVE FUNCTION : performEncodings");
    return faceDescriptors;
}


// JNI /////////////////////////////////////////////////////////////////////////////////////////////

#define JNI_METHOD(NAME) \
    Java_com_debutler_jni_dlib_FaceEncoder_##NAME


/**
 * JNI METHOD - Loads the landmarks detector model file "shape_predictor_68_face_landmarks.dat"
 * and deserializes it to the sFaceLandmarksDetector shape_predictor object.
 */
extern "C" JNIEXPORT void JNICALL
JNI_METHOD(prepareFaceLandmarksDetector)(JNIEnv *env,
                                         jobject thiz) {
    LOGI("JNI FUNCTION RUNNING : prepareFaceLandmarksDetector");

    // Profiler
    Profiler profiler;
    profiler.start();

    /* const char *path = env->GetStringUTFChars(detectorPath, JNI_FALSE); */

    // We need a shape_predictor. This is the tool that will predict face
    // landmark positions given an image and face bounding box.  Here we are just
    // loading the model from the shape_predictor_68_face_landmarks.dat file you gave
    // as a command line argument.
    // Deserialize the shape detector.
    if (fexists("/storage/emulated/0/Download/models/shape_predictor_68_face_landmarks.dat")) {
        deserialize("/storage/emulated/0/Download/models/shape_predictor_68_face_landmarks.dat")>>sFaceLandmarksDetector;
        double interval = profiler.stopAndGetInterval();
        LOGI("L%d: sFaceLandmarksDetector is initialized (took %.3f ms)", __LINE__, interval);
    } else {
        double interval = profiler.stopAndGetInterval();
        LOGI("SHAPE DETECTOR MODEL FILE DOESN'T EXIST OR ACCESS IS NOT GRANTED (took %.3f ms)", interval);
    }

    int parts = static_cast<int>(sFaceLandmarksDetector.num_parts());
    LOGI("num parts : %d", parts);
    /*    env->ReleaseStringUTFChars(detectorPath, path); */

    if (sFaceLandmarksDetector.num_parts() != 68) {
        throwException(env, "It's not a 68 landmarks detector!");
    }
    LOGI("LEAVING JNI FUNCTION : prepareFaceLandmarksDetector");
}


/**
 * JNI METHOD - loads the face encoder network model file "dlib_face_recognition_resnet_model_v1.dat"
 * and deserializes it to the sFaceEncoderNet anet_type object.
 */
extern "C" JNIEXPORT void JNICALL
JNI_METHOD(prepareFaceEncoderNetwork)(JNIEnv *env,
                                         jobject thiz) {
    LOGI("JNI FUNCTION RUNNING : prepareFaceEncoderNetwork");

    // Profiler
    Profiler profiler;
    profiler.start();

    //const char *path = env->GetStringUTFChars(modelPath, JNI_FALSE);
    // Deserialize the model.
    if (fexists("/storage/emulated/0/Download/models/dlib_face_recognition_resnet_model_v1.dat")) {
        deserialize("/storage/emulated/0/Download/models/dlib_face_recognition_resnet_model_v1.dat")>>sFaceEncoderNet;
        double interval = profiler.stopAndGetInterval();
        LOGI("L%d: sFaceEncoderNet is initialized (took %.3f ms)", __LINE__, interval);
    } else {
        double interval = profiler.stopAndGetInterval();
        LOGI("RESNET MODEL FILE DOESN'T EXIST OR ACCESS IS NOT GRANTED (took %.3f ms)", interval);
    }
    /*    env->ReleaseStringUTFChars(modelPath, path); */
    LOGI("LEAVING JNI FUNCTION : prepareFaceEncoderNetwork");
}


/**
 * JNI METHOD - passes 128-element encodings of every face found in the given image to the Java code
 * @param bitmap : the given image potentially containing faces
 * @param faceBoundingBoxes : JNI array of float arrays representing the rectangle bounding boxes demarcating each face
 * This function calls the local function performEncodings
 */
extern "C" JNIEXPORT jobjectArray JNICALL
JNI_METHOD(getEncodings)(JNIEnv *env, jobject instance,
                                      jobject bitmap,
                                      jobjectArray faceBoundingBoxes){

    LOGI("JNI FUNCTION RUNNING : getEncodings");
    //Profiler
    Profiler profiler;
    profiler.start();

    std::vector<matrix<float,0,1>> faceDescriptors = performEncodings(env,
                                                                      instance,
                                                                      bitmap,
                                                                      faceBoundingBoxes);

    auto jniEncoding = (jfloatArray) env->NewFloatArray((jsize)faceDescriptors[0].size());   // row of size = 128
    auto jniEncodings = (jobjectArray) env->NewObjectArray((jsize)faceDescriptors.size(), env->GetObjectClass(jniEncoding),
                                                           nullptr);  // initializing the JNI encodings array - length = nb of faces

    // allocate the JNI face encodings
    LOGI("nb of descriptors : %d", faceDescriptors.size());
    for (int i = 0; i < faceDescriptors.size(); ++i) {
        LOGI("Preparing JNI share for descriptor no. %d of %d", i+1, faceDescriptors.size());
        matrix<float,0,1> descriptor = faceDescriptors[i];
        jniEncoding = (jfloatArray) env->NewFloatArray(descriptor.size());
        // allocate every descriptor component for each encoding
        LOGI("descriptor size: %ld", descriptor.size());
        float encoding[descriptor.size()];
        for (int j = 0; j < descriptor.size(); ++j) {
            float el = descriptor(0, j);
            encoding[j] = el;
        }
        env->SetFloatArrayRegion((jfloatArray)jniEncoding, (jsize)0, 128, (jfloat *)encoding);
        env->SetObjectArrayElement(jniEncodings, i, jniEncoding);
        env->DeleteLocalRef(jniEncoding);
    }

    double interval = profiler.stopAndGetInterval();
    LOGI("L%d: getting encodings for this image took %.3f ms", __LINE__, interval);
    LOGI("LEAVING JNI FUNCTION : getEncodings");
    return jniEncodings;
}


//      ...
//      // Make the image larger so we can detect small faces.
//      pyramid_up(img);
//      LOGI("L%d: pyramid_up the input image (w=%lu, h=%lu).", __LINE__, img.nc(), img.nr());