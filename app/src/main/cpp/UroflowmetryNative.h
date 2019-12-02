//#define MFC_VERSION

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrame(JNIEnv *env,
														jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b);

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameRGB(JNIEnv *env,
                                                        jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b);

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameData(JNIEnv *env, jobject,
                                                                                         jbyteArray byFrameData, jint w, jint h, jint wide_w, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b);

#ifdef __cplusplus
}
#endif //__cplusplus
