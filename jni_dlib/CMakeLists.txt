cmake_minimum_required(VERSION 3.4.1)

# Configure build library name.
set(TARGET_NAME dlib_jni)

# Configure install folder for:
# 1) Finding the dependent libraries.
# 2) Copy out the generated files for others to use.
set(INSTALL_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs)

# Configure import libs.
set(LIB_DIR ${CMAKE_SOURCE_DIR}/src/main/cppLibs)

# Link to libdlib.so.
# Shared lib will also be tucked into APK and sent to target
# refer to app/build.gradle, jniLibs section for that purpose.
# ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# the most valuable thing of this sample, the rest are pretty much normal cmake
add_library(dlib SHARED IMPORTED)
set_target_properties(dlib PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/dlib/lib/${ANDROID_ABI}/libdlib.so)

# Build project shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -std=c++11")
add_library(${TARGET_NAME} SHARED
        src/main/cpp/jni.cpp
        src/main/cpp/profiler.cpp
        src/main/cpp/face-encoder.cpp
        )
target_include_directories(${TARGET_NAME} PRIVATE
        ${CMAKE_SOURCE_DIR}/src/main/cpp/include
        ${LIB_DIR}/dlib/include)
target_link_libraries(${TARGET_NAME}
        dlib
        android
        jnigraphics
        log)

# Make install directory.
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME})
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME}/include)
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME}/lib)

# Copy out lib header file.
file(COPY "${LIB_DIR}/dlib/include/"
        DESTINATION "${INSTALL_DIR}/${TARGET_NAME}/include/")

# Copy out the project binary.
add_custom_command(TARGET ${TARGET_NAME} POST_BUILD

        # Copy pre-built *.so files.
        COMMAND "${CMAKE_COMMAND}" -E
        copy_directory "${LIB_DIR}/dlib/lib/${ANDROID_ABI}"
        "${INSTALL_DIR}/${TARGET_NAME}/lib/${ANDROID_ABI}"

        # Copy generated *.so files.
        COMMAND "${CMAKE_COMMAND}" -E
        copy_directory "${CMAKE_SOURCE_DIR}/build/intermediates/cmake/${CMAKE_BUILD_TYPE}/obj/${ANDROID_ABI}"
        "${INSTALL_DIR}/${TARGET_NAME}/lib/${ANDROID_ABI}"

        COMMENT "Copying ${TARGET_NAME} shared library to output directory")
