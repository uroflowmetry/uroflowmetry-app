#include "UroflowmetryNative.h"

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <iostream>
#include <vector>

//#define LOG_VIEW
//#define LOG_SAVE

#define METHOD_BIN

#include <android/log.h>

//#include <opencv2/core/core.hpp>
//#include <opencv2/highgui.hpp>
//#include <opencv2/imgproc.hpp>
//#include <opencv2/imgproc/imgproc_c.h>
#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"

using namespace std;
using namespace cv;

// Option for remove noise
int g_edgeThresh0 = 15;
int g_edgeThresh1 = 3 * g_edgeThresh0;

Size g_gausSize = Size(5, 5);
Size g_eleSize0 = Size(19, 19);
Point g_elePoint = Point(-1, -1);

#define  LOG_TAG    "UROFLOWMETRY_NATIVE"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

void ShowArrRectInMatImage(Mat matImgView, Rect *arrRt, int nNumArrRt)
{
    Mat matImgRe;
    Mat matImgRGB;
    if (matImgView.channels() != 3) {
        matImgRGB = Mat(matImgView.size(), CV_8UC3);
        cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
    }
    else
        matImgRGB = matImgView.clone();

    for(int i = 0; i < nNumArrRt; i++)
        rectangle(matImgRGB, arrRt[i], Scalar(0, 0, 255), 2, 8, 0);
#ifdef LOG_SAVE
    imwrite(string("/storage/emulated/0/test_marks.jpg"), matImgRGB);
#endif
}
void ShowVtRectInMatImage(Mat matImgView, vector<Rect> vtRt)
{
    if (vtRt.size() == 0) return;

    Mat matImgRe;
    Mat matImgRGB;
    if (matImgView.channels() != 3) {
        matImgRGB = Mat(matImgView.size(), CV_8UC3);
        cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
    }
    else
        matImgRGB = matImgView.clone();

    for(int i = 0; i < (int)vtRt.size(); i++)
        rectangle(matImgRGB, vtRt.at(i), Scalar(0, 0, 255), 2, 8, 0);
}

bool compareIntervalX(Rect rt1, Rect rt2)
{
	return (rt1.x < rt2.x);
}

bool compareIntervalY(Rect i1, Rect i2)
{
	return (i1.y < i2.y);
}
bool compareIntervalRight(Rect i1, Rect i2)
{
	return (i1.x + i1.width > i2.x + i2.width);
}
bool IsIncludeRect(Rect rt1, Rect rt2)
{
	if (rt1.x <= rt2.x && rt2.x + rt2.width <= rt1.x + rt1.width &&
		rt1.y <= rt2.y && rt2.y + rt2.height <= rt1.y + rt1.height)
		return true;
	else
		return false;
}

bool getFlowmetry(Mat matIn, Rect &rtBorder, int nMinW, int nMaxW) {
	// Find the biggest rectangle.
	vector < vector<Point> > contours;
	vector < Vec4i > hiera;
	Mat matContours = matIn.clone();
	findContours(matContours, contours, hiera,  RETR_LIST, CHAIN_APPROX_SIMPLE);//CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
	matContours.release();

	vector<Rect> vtRtContours;
	for (int i = 0; i < contours.size(); i++) {
		Rect rtBounding = boundingRect(contours[i]);
		double dArea = contourArea(contours[i], false);
		if( 3 * dArea < rtBounding.width * rtBounding.height )
            continue;
		if (rtBounding.x > 50 && rtBounding.y > 50 &&
			rtBounding.x + rtBounding.width < matIn.size().width - 50 && rtBounding.y + rtBounding.height < matIn.size().height - 50 &&
			//rtBounding.width > g_nMinBorderW && rtBounding.width < g_nMaxBorderW)
			rtBounding.width > nMinW && rtBounding.width < nMaxW )
			vtRtContours.push_back(rtBounding);
	}
#ifdef LOG_VIEW
	if(vtRtContours.size() > 0)
		ShowVtRectInMatImage(matIn, vtRtContours);
#endif

	if (vtRtContours.size() > 0) {
		int k = 0;
		if (vtRtContours.size() > 1) {

			for (int i = 0; i < (int)vtRtContours.size(); i++) {
				if (vtRtContours.at(k).width < vtRtContours.at(i).width)
					k = i;
			}
		}
		Rect rtBounding = vtRtContours.at(k);
		rtBorder.x = rtBounding.x;
		rtBorder.y = rtBounding.y;
		rtBorder.width = rtBounding.width;
		rtBorder.height = rtBounding.height;
		return true;
	}

	return false;
}

#define   SCREEN_WIDTH			800

void rgb2gray(unsigned char* pbyImgRGB, int nW, int nH, unsigned char* pbyImgGray)
{
    int i, j, ii, jj, hh;
    for( i = 0; i < nH; i++ )
    {
        hh = i*nW;
        ii = 3*hh;
        for(j = 0; j < nW ; j++)
        {
            jj = ii + 3*j;
            pbyImgGray[hh + j] = ( ( (int)pbyImgRGB[jj] * 117 + (int)pbyImgRGB[jj+1]*601 + (int)pbyImgRGB[jj+2]*306 ) >> 10 ) ;
        }
    }
}

void getGrayData(unsigned char* pbyImgData, int nImgW, int nImgH, int nWideWidth, unsigned char* pbyGrayData){

    unsigned char r, g, b;
    int iy1, iy2, jx1, jx2;
    for (int i = 0; i < nImgH; i++) {
        iy1 = i * nImgW;//3 * nImgW;
        iy2 = i * nWideWidth;
        for (int j = 0; j < nImgW; j++) {
//            jx1 = j * 3;
            jx2 = j * 4;
//            byRGB[iy1 + jx1] = pbyImgData[iy2 + jx2 + 2];
//            byRGB[iy1 + jx1 + 1] = arrayImgData[iy2 + jx2 + 1];
//            byRGB[iy1 + jx1 + 2] = arrayImgData[iy2 + jx2];
            r = pbyImgData[iy2 + jx2 + 2];
            g = pbyImgData[iy2 + jx2 + 1];
            b = pbyImgData[iy2 + jx2];
            pbyGrayData[iy1 + j] = ( ( (int)r * 117 + (int)g*601 + (int)b*306 ) >> 10 ) ;
        }
    }
}
void rotate90(unsigned char* byDataIn, int& width, int& height, unsigned char* byDataOut){
    int inputOffset = 0;
    for (int y = 0; y < height; y++) {
        //int outputOffset = y * width;
        for (int x = 0; x < width; x++) {
            unsigned char grey = byDataIn[inputOffset + x];
            int outputOffset = x * height;
            byDataOut[outputOffset + height - y - 1] = grey;
        }
        inputOffset += width;
    }
    int temp = height;
    height = width;
    width = temp;
}

void renderCroppedGreyscaleBitmap(unsigned char* yuvData, int& width, int& height, unsigned char* pixels) {
	int inputOffset = 0;

	if( width > height){
        for (int y = 0; y < height; y++) {
            //int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                unsigned char grey = yuvData[inputOffset + x] & 0xff;
                int outputOffset = x * height;
                pixels[outputOffset + height - y - 1] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += width;
        }
        int temp = height;
        height = width;
        width = temp;
	}
    else {
        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                unsigned char grey = yuvData[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += width;
        }
    }
}

bool ProcMatFrame(Mat orgGray, int crop_l, int crop_t, int crop_r, int crop_b, int *pPosData) {
    bool bRet = false;
    Mat cropGray, imGray, imEdge, imErode;
    if( crop_r == 0 || crop_b == 0 ||
        crop_l == crop_r || crop_t == crop_b )
    {
        cropGray = orgGray.clone();
    }
    else {
        Rect rtCrop;
        rtCrop.x = crop_l;
        rtCrop.y = crop_t;
        rtCrop.width = crop_r - crop_l;
        rtCrop.height = crop_b - crop_t;

        cropGray = orgGray(rtCrop);
    }

    LOGD("Engine : =============  resize() start ==================");
    Size imgSize = cropGray.size();
    double dRatio = 1.0;
    int	nReW = imgSize.width;
    int	nReH = imgSize.height;
    if (imgSize.width != SCREEN_WIDTH) {
        nReW = SCREEN_WIDTH;
        dRatio = (double)nReW / (double)imgSize.width;
        nReH = (int)(imgSize.height * dRatio);
        resize(cropGray, imGray, Size(nReW, nReH), 1.0, 1.0, INTER_CUBIC);
    }

#ifdef METHOD_BIN
    threshold(imGray, imEdge, 80, 255, THRESH_BINARY);

    LOGD("Engine : =============  dilate(), erode() start ==================");
    Mat ele0 = getStructuringElement(MORPH_RECT, g_eleSize0, g_elePoint);
    dilate(imEdge, imErode, ele0);
#else
    LOGD("Engine : =============  Canny() start ==================");
	GaussianBlur(imGray, imEdge, g_gausSize, 0);
	Canny(imEdge, imEdge, g_edgeThresh1, g_edgeThresh0);
	//imEdge = 255 - imEdge;

    LOGD("Engine : =============  dilate(), erode() start ==================");
	Mat ele0 = getStructuringElement(MORPH_RECT, g_eleSize0, g_elePoint);
	dilate(imEdge, imErode, ele0);

#endif
#ifdef LOG_VIEW
    //ShowMatImage(imEdge);
	ShowMatResize(imEdge);
#endif

#ifdef LOG_VIEW
    //ShowMatImage(imErode);
	ShowMatResize(imErode);
#endif
    Mat ele2 = getStructuringElement(MORPH_RECT, g_eleSize0, g_elePoint);
    erode(imErode, imErode, ele2);
#ifdef LOG_VIEW
    //ShowMatImage(imErode);
	ShowMatResize(imErode);
#endif


    LOGD("Engine : =============  getFlowmetry() start ==================");
    int nMinW = imErode.size().width / 3;
    int nMaxW = imErode.size().width - 10;
    cv::Rect rtBorder;
    bRet = getFlowmetry(imErode, rtBorder, nMinW, nMaxW);
    if (bRet == true) {
        pPosData[0] = crop_l + (int)(rtBorder.x / dRatio);
        pPosData[1] = crop_t + (int)(rtBorder.y / dRatio);
        pPosData[2] = crop_l + (int)(rtBorder.br().x / dRatio);
        pPosData[3] = crop_t + (int)(rtBorder.br().y / dRatio);
#ifdef LOG_SAVE
        Mat orgRGB;
        orgRGB = Mat(cropGray.size(), CV_8UC3);
        cv::cvtColor(cropGray, orgRGB, COLOR_GRAY2RGB);
        cv::imwrite(string("/storage/emulated/0/frame_org.png"), orgRGB);

        Mat matImgRGB;
        if (imErode.channels() != 3) {
            matImgRGB = Mat(imErode.size(), CV_8UC3);
            cv::cvtColor(imErode, matImgRGB, COLOR_GRAY2RGB);
        }
        else
            matImgRGB = imErode.clone();
        rectangle(matImgRGB, rtBorder, Scalar(0, 0, 255), 2, 8, 0);
        cv::imwrite(string("/storage/emulated/0/frame_result.png"), matImgRGB);
#endif
    }

    imErode.release();
    imEdge.release();
    imEdge.release();
    imGray.release();
    cropGray.release();

    return bRet;
}

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrame(JNIEnv *env,
								jobject thiz, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b){
    jboolean bRet = false;
    jbyte* pbyImgData = 0;
    jint pRectData[4];

    memset(pRectData, 0, 4 * sizeof(jint));
    pbyImgData = env->GetByteArrayElements(byArrImgData, 0);

	unsigned char *pPixels = new unsigned char[w * h];
	renderCroppedGreyscaleBitmap((unsigned char *)pbyImgData, w, h, pPixels);
	
	Mat orgGray = Mat(h, w, CV_8UC1);
	memcpy(orgGray.data, pPixels, w * h);

    bRet = ProcMatFrame(orgGray, crop_l, crop_t, crop_r, crop_b, (int*)pRectData);

    orgGray.release();

	if( bRet == true )
        env->SetIntArrayRegion(nArrPosition, 0, 4, pRectData);

	env->ReleaseByteArrayElements(byArrImgData, pbyImgData, JNI_ABORT);

    delete[] pPixels; pPixels = nullptr;
	return bRet;
}

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameRGB(JNIEnv *env,
                              jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b){
    jboolean bRet = false;
    jbyte* pbyImgData = 0;
    jint pRectData[4];

    memset(pRectData, 0, 4 * sizeof(jint));
    pbyImgData = env->GetByteArrayElements(byArrImgData, 0);

    LOGD("Engine : =============  EngineUroflowmetry_ProcFrame() start ==================");
    Mat imFrameOrigin = Mat(h, w, CV_8UC3);
    memcpy(imFrameOrigin.data, pbyImgData, 3 * w * h);

    LOGD("Engine : =============  cvtColor() start ==================");
    Mat orgGray;
    cv::cvtColor(imFrameOrigin, orgGray, COLOR_BGR2GRAY);
#ifdef LOG_SAVE
    //cv::imwrite(string("sdcard/Pictures/frame_org.png"), orgGray);
#endif

    LOGD("Engine : ============== Image        width : %d,  height : %d", w, h);
    LOGD("Engine : ============== Image crop   left : %d, top : %d, right : %d, bottom : %d", crop_l, crop_t, crop_r, crop_b);
    bRet = ProcMatFrame(orgGray, crop_l, crop_t, crop_r, crop_b, pRectData);

    orgGray.release();
    imFrameOrigin.release();

    if( bRet == true ) {
        LOGD("Engine : ============== Result left : %d, top : %d, right : %d, bottom : %d", pRectData[0], pRectData[1], pRectData[2], pRectData[3]);

        env->SetIntArrayRegion(nArrPosition, 0, 4, pRectData);
    }

    env->ReleaseByteArrayElements(byArrImgData, pbyImgData, JNI_ABORT);

    return bRet;
}


JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameData(JNIEnv *env, jobject,
        jbyteArray byFrameData, jint w, jint h, jint wide_w, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b){
    jboolean bRet = false;
    jbyte* pbyImgData = 0;
    jint pRectData[4];

    memset(pRectData, 0, 4 * sizeof(jint));
    pbyImgData = env->GetByteArrayElements(byFrameData, 0);

    int size = w * h;
    unsigned char* pbyGrayData = new unsigned char[size];
    memset(pbyGrayData, 0, size);
    getGrayData((unsigned char*)pbyImgData, w, h, wide_w, pbyGrayData);

    LOGD("Engine : =============  EngineUroflowmetry_ProcFrame() start ==================");
    Mat orgGray = Mat(h, w, CV_8UC1);
    memcpy(orgGray.data, pbyImgData, size);

#ifdef LOG_SAVE
    //cv::imwrite(string("sdcard/Pictures/frame_org.png"), orgGray);
#endif

    LOGD("Engine : ============== Image        width : %d,  height : %d", w, h);
    LOGD("Engine : ============== Image crop   left : %d, top : %d, right : %d, bottom : %d", crop_l, crop_t, crop_r, crop_b);
    bRet = ProcMatFrame(orgGray, crop_l, crop_t, crop_r, crop_b, pRectData);

    orgGray.release();
    delete pbyGrayData; pbyGrayData = nullptr;

    if( bRet == true ) {
        LOGD("Engine : ============== Result left : %d, top : %d, right : %d, bottom : %d", pRectData[0], pRectData[1], pRectData[2], pRectData[3]);

        env->SetIntArrayRegion(nArrPosition, 0, 4, pRectData);
    }

    env->ReleaseByteArrayElements(byFrameData, pbyImgData, JNI_ABORT);

    return bRet;
}


