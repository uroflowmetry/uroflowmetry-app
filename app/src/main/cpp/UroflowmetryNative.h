//#define MFC_VERSION

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <iostream>
#include <cmath>
#include <vector>

#ifndef MFC_VERSION
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrame(JNIEnv *env,
														jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b);

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameRGB(JNIEnv *env,
                                                        jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b);

#ifdef __cplusplus
}
#endif //__cplusplus

#else

extern  bool UroflowmetryNative_ProcFrame(unsigned char* pbyImgData, int w, int h, int* pRectData, int crop_l, int crop_t, int crop_r, int crop_b);

#endif

