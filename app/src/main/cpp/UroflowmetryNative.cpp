#include "UroflowmetryNative.h"

//#define LOG_VIEW
//#define LOG_SAVE

#define METHOD_BIN

#ifndef MFC_VERSION
#include <android/log.h>
#else

#include <Windows.h>
#ifdef _DEBUG
#pragma comment(lib, "opencv_core410d.lib")
#pragma comment(lib, "opencv_highgui410d.lib")
#pragma comment(lib, "opencv_imgcodecs410d.lib")
#pragma comment(lib, "opencv_imgproc410d.lib")
#pragma comment (lib, "opencv_ml410d.lib")
#pragma comment (lib, "opencv_objdetect410d.lib")
#pragma comment (lib, "opencv_video410d.lib")
#else
#pragma comment(lib, "opencv_core410.lib")
#pragma comment(lib, "opencv_highgui410.lib")
#pragma comment(lib, "opencv_imgcodecs410.lib")
#pragma comment(lib, "opencv_imgproc410.lib")
#pragma comment (lib, "opencv_ml410.lib")
#pragma comment (lib, "opencv_objdetect410.lib")
#pragma comment (lib, "opencv_video410.lib")
#endif //_DEBUT
//#pragma comment (lib, "opencv_world410.lib")
#endif

//#include <opencv/cv.h>
//#include <opencv/cxcore.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>


using namespace std;
using namespace cv;

typedef unsigned char OMR_BYTE;

/** Options */
//bool DEBUG = true;
//bool DRAW_GRID = false; // Draw grid on sub answers or not.

int minRectW = 400;	// Min rectangle Width
int minRectH = 700;	// Min rectangle Height
int maxRectW = 3100;	// Max rectangle Width
int maxRectH = 3100;	// Max rectangle Height

#define ERROR_FIND_AN			-5
#define ERROR_FIND_MARK			-6

#define MAX_NUM_CONTOURS		1500
#define MAX_NUM_AN_GROUP		5
#define MAX_NUM_LINE_GROUP		50
#define MAX_NUM_LINE_MARKS		10

int g_sourceWidth = 1280;
int g_boderWidth = 1024;

int g_nMinBorderW = 300;
int g_nMaxBorderW = 600;

int g_nMinBoderH = 700;

int g_nMinWidthAN = 185;
int g_nMaxWidthAN = 270;
int g_nMinHeightAN = 15;
int g_nMaxHeightAN = 110;

int g_nMinWidthMark = 10;
int g_nMaxWidthMark = 45;
int g_nMinHeightMark = 10;
int g_nMaxHeightMark = 45;
int g_nMarkRadius = 15;

// Option for remove noise
int g_edgeThresh0 = 15;
int g_edgeThresh1 = 3 * g_edgeThresh0;

Size g_gausSize = Size(5, 5);
Size g_eleSize0 = Size(19, 19);
Size g_eleSize1 = Size(11, 11);
Size g_eleSize2 = Size(9, 9);
Point g_elePoint = Point(-1, -1);

Scalar g_okSlr = Scalar(0, 0, 255);
Scalar g_errorSlr = Scalar(255, 0, 0);

int g_nMarkThreshold = 150;
//int g_minThreshold = 50;// 110; // Threshold 80 to 105 is Ok
//int g_maxThreshold = 255; // Always 255

int g_nNumRow = 25;

//bool g_bPreMarks[150][5];
bool g_bPreMarks[600];
static int g_nCntCompare = 0;

bool g_bSecurity = true;

#define OMR_MAX(a,b)            (((a) > (b)) ? (a) : (b))
#define OMR_MIN(a,b)            (((a) < (b)) ? (a) : (b))

typedef struct tagOMR_RECT
{
	int left;
	int top;
	int right;
	int bottom;
}OMR_RECT;
typedef struct tagLineMarksInfo
{
	int				nNumMarks;							//the number of the marks in one line
	Rect			rtMarks[MAX_NUM_LINE_MARKS];		//the rectangle of the marks in one line
	bool			bSelMarks[MAX_NUM_LINE_MARKS];		//if the marks in one line is filled, 1 or 0

	Rect			rtLine;								//the rectangle of one line
	Rect			rtMarksMinH;
}LineMarksInfo;

/** For log */
#define  LOG_TAG    "VGRADE NATIVE"

#ifndef MFC_VERSION
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

bool getTimeLimit(JNIEnv *env) {
	jclass date = env->FindClass("java/util/Date");
	if (env->ExceptionCheck()) {
		return true;
	}
	jmethodID dateTypeConstructor = env->GetMethodID(date, "<init>", "()V");
	if (dateTypeConstructor == NULL) {
		return true;
	}
	jobject dateObjectStart = env->NewObject(date, dateTypeConstructor);
	if (dateObjectStart == NULL) {
		return true;
	}
	jmethodID getTime = env->GetMethodID(date, "getTime", "()J");
	if (getTime == NULL) {
		return true;
	}

	jlong lCurTime = env->CallLongMethod(dateObjectStart, getTime);
	//std::cout << a << std::endl;

	jlong lTimeLimit = 60 * 60 * 24 * 60;//2019.10.20
	if ((lCurTime - 1566274161571) / 1000 > lTimeLimit) //2019.08.20
		return true;

	return false;
}

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

#else
void log_print(char* szFormat, ...)
{
	char buffer[1000];
	memset(buffer, 0, 1000);

	va_list argptr;
	va_start(argptr, szFormat);
	wvsprintf(buffer, szFormat, argptr);
	va_end(argptr);

	OutputDebugString(buffer);
}

#define  LOGE(...)  log_print(LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  log_print(LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  log_print(LOG_TAG,__VA_ARGS__)

void ShowMatImage(Mat matImgView)
{
	namedWindow("Result", 1);
	imshow("Result", matImgView);

	waitKey(0);
	destroyWindow("Result");
}

void ShowMatResize(Mat matImgView)
{
	Mat matImgRe;

	resize(matImgView, matImgRe, Size(matImgView.cols * 800 / matImgView.rows, 800),
			1.0, 1.0, INTER_CUBIC);

	//Mat matImgRGB;

	//if( matImgView.channels() != 3 )
	//{
	//	matImgRGB = Mat(matImgView.size(), CV_8UC3);
	//	cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
	//	namedWindow("Result", 1);
	//	imshow("Result", matImgRGB);
	//}
	//else

	ShowMatImage(matImgRe);
}
void ShowRectInMatImage(Mat matImgView, Rect rt)
{
	Mat matImgRe;
	Mat matImgRGB;
	if (matImgView.channels() != 3) {
		matImgRGB = Mat(matImgView.size(), CV_8UC3);
		cv::cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
	}
	else
		matImgRGB = matImgView.clone();

	rectangle(matImgRGB, rt, Scalar(0, 0, 255), 2, 8, 0);

	ShowMatResize(matImgRGB);
}
void ShowArrRectInMatImage(Mat matImgView, Rect *arrRt, int nNumArrRt)
{
	Mat matImgRe;
	Mat matImgRGB;
	if (matImgView.channels() != 3) {
		matImgRGB = Mat(matImgView.size(), CV_8UC3);
		cv::cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
	}
	else
		matImgRGB = matImgView.clone();

	for(int i = 0; i < nNumArrRt; i++)
		rectangle(matImgRGB, arrRt[i], Scalar(0, 0, 255), 2, 8, 0);

	ShowMatResize(matImgRGB);
}
void ShowVtRectInMatImage(Mat matImgView, vector<Rect> vtRt)
{
	if (vtRt.size() == 0) return;

	Mat matImgRe;
	Mat matImgRGB;
	if (matImgView.channels() != 3) {
		matImgRGB = Mat(matImgView.size(), CV_8UC3);
		cv::cvtColor(matImgView, matImgRGB, COLOR_GRAY2RGB);
	}
	else
		matImgRGB = matImgView.clone();

	for(int i = 0; i < (int)vtRt.size(); i++)
		rectangle(matImgRGB, vtRt.at(i), Scalar(0, 0, 255), 2, 8, 0);

	ShowMatImage(matImgRGB);
	//ShowMatResize(matImgRGB);
}
#endif //MFC_VERSION

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

void sortRect(vector<Rect> &pVtRt, int type){
	//type 0:X, 1:Y, 2:right
	bool bOver = false;
	vector<Rect> pVtRtNew;
	for (int i = 0; i < (int)pVtRt.size(); i++)
	{
		bOver = true;
		for (int j = 0; j < (int)pVtRt.size(); j++)
		{
			if (i == j)
				continue;
			if (IsIncludeRect(pVtRt.at(j), pVtRt.at(i)))
			{
				bOver = false;
				break;
			}
		}
		if (bOver == true)
			pVtRtNew.push_back(pVtRt.at(i));
	}

	if(type == 0)
		std::sort(pVtRtNew.begin(), pVtRtNew.end(), compareIntervalX);
	else if(type == 1)
		std::sort(pVtRtNew.begin(), pVtRtNew.end(), compareIntervalY);
	else if(type == 2)
		std::sort(pVtRtNew.begin(), pVtRtNew.end(), compareIntervalRight);

	pVtRt.clear();
	pVtRt = pVtRtNew;
}


bool getFlowmetry(Mat matIn, Rect &rtBorder, int nMinW, int nMaxW) {
	// Find the biggest rectangle.
	vector < vector<Point> > contours;
	vector < Vec4i > hiera;
	Mat matContours = matIn.clone();
	findContours(matContours, contours, hiera, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
	matContours.release();

	vector<Rect> vtRtContours;
	for (int i = 0; i < contours.size(); i++) {
		Rect rtBounding = boundingRect(contours[i]);
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

	//Rect rtBounding;
	//int i, nContours, indexLevel2 = 0;
	//nContours = (int)contours.size();
	//for (i = 0; i < nContours; i++) {
	//	indexLevel2 = (int)hiera[i][2];
	//	if (indexLevel2 >= i) {
	//		vector < Point > tempContour = contours[i];// indexLevel2];
	//		rtBounding = boundingRect(tempContour);
	//		if (rtBounding.x > 10 && rtBounding.y > 10 && rtBounding.width > g_nMinBorderW && rtBounding.width < g_nMaxBorderW) {
	//			//rectangle(origin, rtBounding.tl(), rtBounding.br(), colorGrid, 15);
	//			break;
	//		}
	//	}
	//}

	//if (indexLevel2 < 0){
	//	return false;
	//}

	//rtBorder.x = rtBounding.x;
	//rtBorder.y = rtBounding.y;
	//rtBorder.width = rtBounding.width;
	//rtBorder.height = rtBounding.height;


	//TL.x = rtBounding.x; TL.y = rtBounding.y;
	//TR.x = rtBounding.br().x; TR.y = rtBounding.y;
	//BL.x = rtBounding.x; BL.y = rtBounding.br().y;
	//BR.x = rtBounding.br().x; BR.y = rtBounding.br().y;
#ifdef LOG_VIE
	ShowRectInMatImage(matIn, rtBorder);
#endif

	return true;

	//vector < Point > lstPoint;
	//lstPoint = contours[i];

	//if (indexLevel2 >= 0) {
	//	rtBorder.x = rectBounding.x;
	//	rtBorder.y = rectBounding.y;
	//	rtBorder.width = rectBounding.width;
	//	rtBorder.height = rectBounding.height;

	//	ShowRectInMatImage(matOrigin, rtBorder);

	//	return true;
	//}


//	bool hit = false;
//	while (true) {
//		lstPoint = contours[indexLevel2];
//		Vec4i vl = hiera[indexLevel2];
//		//if (vl == NULL) {
//		//	break;
//		//}
//		// Get contour same level
//		indexLevel2 = (int)vl[0];
//		if (indexLevel2 == -1) {
//			indexLevel2 = (int)vl[2];
//		}
//
//		rtBounding = boundingRect(lstPoint);
//		//LOGI("Rect w: %d, h: %d", rectBounding.width, rectBounding.height);
//
//		if (rtBounding.width > g_nMinBoderW && rtBounding.width <= maxRectW
//			&& rtBounding.height > minRectH && rtBounding.height <= maxRectH) {
//
//#ifdef LOG_VIE
//				rectangle(matIn, rtBounding.tl(), rtBounding.br(), colorGrid, 5);
//#endif 
//			LOGI("Get list point HIT");
//			hit = true;
//			break;
//		}
//
//		if (indexLevel2 < 0) {
//			LOGI("Get list point not HIT");
//			break;
//		}
//
//	}
	//if (lstPoint.empty()) {
	//	return false;
	//}

	//int halfTop = rtBounding.y + rtBounding.height / 2;
	//int halfBot = (int)rtBounding.br().y - rtBounding.height / 2;
	//int halfLeft = rtBounding.x + rtBounding.width / 2;
	//int halfRight = (int)rtBounding.br().x - rtBounding.width / 2;

	//while (!lstPoint.empty()) {
	//	Point p = lstPoint.back();
	//	lstPoint.pop_back();

	//	if (p.x < rtBounding.x + 15) {
	//		if (p.y < halfTop) {
	//			if (TL.x == -1
	//				|| abs(p.y - rtBounding.y) + abs(p.x - rtBounding.x)
	//				< abs(TL.y - rtBounding.y)
	//				+ abs(TL.x - rtBounding.x)) {
	//				TL = p;
	//			}
	//		}
	//		else if (p.y > halfBot) {
	//			if (BL.x == -1
	//				|| abs(p.y - rtBounding.br().y)
	//				+ abs(p.x - rtBounding.x)
	//				< abs(BL.y - rtBounding.br().y)
	//				+ abs(BL.x - rtBounding.x)) {
	//				BL = p;
	//			}
	//		}
	//		continue;
	//	}
	//	if (p.x > rtBounding.br().x - 15) {
	//		if (p.y < halfTop) {
	//			if (TR.x == -1
	//				|| abs(p.y - rtBounding.y)
	//				+ abs(p.x - rtBounding.br().x)
	//				< abs(TR.y - rtBounding.y)
	//				+ abs(TR.x - rtBounding.br().x)) {
	//				TR = p;
	//			}
	//		}
	//		else if (p.x > halfRight) {
	//			if (BR.x == -1
	//				|| abs(p.y - rtBounding.br().y)
	//				+ abs(p.x - rtBounding.br().x)
	//				< abs(BR.y - rtBounding.br().y)
	//				+ abs(BR.x - rtBounding.br().x)) {
	//				BR = p;
	//			}
	//		}

	//		continue;
	//	}
	//	if (p.y < rtBounding.y + 15) {
	//		if (p.x < halfLeft) {
	//			if (TL.x == -1
	//				|| abs(p.y - rtBounding.y) + abs(p.x - rtBounding.x)
	//				< abs(TL.y - rtBounding.y)
	//				+ abs(TL.x - rtBounding.x)) {
	//				TL = p;
	//			}
	//		}
	//		else if (p.x > halfRight) {
	//			if (TR.x == -1
	//				|| abs(p.y - rtBounding.y)
	//				+ abs(p.x - rtBounding.br().x)
	//				< abs(TR.y - rtBounding.y)
	//				+ abs(TR.x - rtBounding.br().x)) {
	//				TR = p;
	//			}
	//		}
	//		continue;
	//	}

	//	if (p.y > rtBounding.br().y - 15) {
	//		if (p.x < halfLeft) {
	//			if (BL.x == -1
	//				|| abs(p.y - rtBounding.br().y)
	//				+ abs(p.x - rtBounding.x)
	//				< abs(BL.y - rtBounding.br().y)
	//				+ abs(BL.x - rtBounding.x)) {
	//				BL = p;
	//			}
	//		}
	//		else if (p.x > halfRight) {
	//			if (BR.x == -1
	//				|| abs(p.y - rtBounding.br().y)
	//				+ abs(p.x - rtBounding.br().x)
	//				< abs(BR.y - rtBounding.br().y)
	//				+ abs(BR.x - rtBounding.br().x)) {
	//				BR = p;
	//			}
	//		}
	//		continue;
	//	}
	//}

	////	if (DEBUG) {
	////		if (TL.x != -1)
	////			circle(origin, TL, 10, colorGrid, 5);
	////		if (TR.x != -1)
	////			circle(origin, TR, 10, colorGrid, 5);
	////		if (BL.x != -1l)
	////			circle(origin, BL, 10, colorGrid, 5);
	////		if (BR.x != -1l)
	////			circle(origin, BR, 10, colorGrid, 5);
	////	}

	//if (TL.x == -1 || TR.x == -1 || BR.x == -1 || BL.x == -1) {
	//	
	//	return false;
	//}

	//return  true;
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
	//int outputOffset = 0;

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

//	Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//	bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//	return bitmap;
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
    //cv::imwrite(string("storage/emulated/0/frame_erode.png"), imErode);
    //imwrite("frame_erode.jpg", imErode);

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

#ifndef MFC_VERSION
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
	//cv::imwrite(string("/storage/emulated/0/frame_gray.png"), orgGray);

#else
bool UroflowmetryNative_ProcFrame(unsigned char* pbyImgData, int w, int h, int* pRectData, int crop_l, int crop_t, int crop_r, int crop_b) {

	bool bRet = false;
    memset(pRectData, 0, 4 * sizeof(int));

	LOGD("Engine : =============  EngineUroflowmetry_ProcFrame() start ==================");
	Mat imFrameOrigin = Mat(h, w, CV_8UC3);
	memcpy(imFrameOrigin.data, pbyImgData, 3 * w * h);
#ifdef LOG_SAVE
	cv::imwrite(string("storage/emulated/0/frame_org.png"), imFrameOrigin);
#endif
	
	LOGD("Engine : =============  cvtColor() start ==================");
	Mat orgGray;
	cv::cvtColor(imFrameOrigin, orgGray, CV_BGR2GRAY);
#endif

    bRet = ProcMatFrame(orgGray, crop_l, crop_t, crop_r, crop_b, (int*)pRectData);

    orgGray.release();

#ifndef MFC_VERSION
	delete[] pPixels; pPixels = nullptr;

	if( bRet == true )
        env->SetIntArrayRegion(nArrPosition, 0, 4, pRectData);

	env->ReleaseByteArrayElements(byArrImgData, pbyImgData, JNI_ABORT);
#endif
	return bRet;
}

JNIEXPORT jboolean JNICALL Java_com_uroflowmetry_engine_EngineUroflowmetry_ProcFrameRGB(JNIEnv *env,
                              jobject, jbyteArray byArrImgData, jint w, jint h, jintArray nArrPosition, jint crop_l, jint crop_t, jint crop_r, jint crop_b){
    jboolean bRet = false;
    jbyte* pbyImgData = 0;
    jint pRectData[4];

    memset(pRectData, 0, 4 * sizeof(jint));
    pbyImgData = env->GetByteArrayElements(byArrImgData, 0);

//    unsigned char* pbyImgGray = new unsigned char[w * h];
//    rgb2gray((unsigned char*)pbyImgData, w, h, pbyImgGray);
//    if( w > h ){
//        unsigned char* pbyImgRot = new unsigned char[w * h];
//        rotate90(pbyImgGray, w, h, pbyImgRot);
//        memcpy(pbyImgGray, pbyImgRot, w * h);
//        delete pbyImgRot; pbyImgRot = nullptr;
//    }

    LOGD("Engine : =============  EngineUroflowmetry_ProcFrame() start ==================");
    Mat imFrameOrigin = Mat(h, w, CV_8UC3);
    memcpy(imFrameOrigin.data, pbyImgData, 3 * w * h);

    LOGD("Engine : =============  cvtColor() start ==================");
    //Mat orgGray = Mat(h, w, CV_8UC1);
    //memcpy(orgGray.data, pbyImgGray, w * h);
    Mat orgGray;
    cv::cvtColor(imFrameOrigin, orgGray, CV_BGR2GRAY);
#ifdef LOG_SAVE
    //cv::imwrite(string("sdcard/Pictures/frame_org.png"), orgGray);
#endif

    LOGD("Engine : ============== Image        width : %d,  height : %d", w, h);
    LOGD("Engine : ============== Image crop   left : %d, top : %d, right : %d, bottom : %d", crop_l, crop_t, crop_r, crop_b);
    bRet = ProcMatFrame(orgGray, crop_l, crop_t, crop_r, crop_b, pRectData);

    orgGray.release();
    imFrameOrigin.release();
    //delete pbyImgGray; pbyImgGray = nullptr;

#ifndef MFC_VERSION
    if( bRet == true ) {
        LOGD("Engine : ============== Result left : %d, top : %d, right : %d, bottom : %d", pRectData[0], pRectData[1], pRectData[2], pRectData[3]);

        env->SetIntArrayRegion(nArrPosition, 0, 4, pRectData);
    }

    env->ReleaseByteArrayElements(byArrImgData, pbyImgData, JNI_ABORT);
#endif
    return bRet;
}

