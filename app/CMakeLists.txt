# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

include_directories( ${CMAKE_CURRENT_SOURCE_DIR}/opencv/include/ )

add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_java4.so)


add_library( # Sets the name of the library.
        EngineUroflowmetry

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/UroflowmetryNative.cpp)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
find_library(jnigraphics-lib
        jnigraphics)



target_link_libraries( # Specifies the target library.
            EngineUroflowmetry
            lib_opencv

            # Links the target library to the log library
            # included in the NDK.
            ${log-lib}
            ${jnigraphics-lib}
            #${OpenCV_LIBS}
            )
