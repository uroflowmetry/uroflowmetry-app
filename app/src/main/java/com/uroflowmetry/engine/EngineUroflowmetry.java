package com.uroflowmetry.engine;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;

public class EngineUroflowmetry {

    static{
        System.loadLibrary("EngineUroflowmetry");
    }

//    public static boolean getArea(byte[] data, int width, int height){//}, int[] position){
//
//        Log.d("Camera : ", " ============ width : ===========" + width );
//        Log.d("Camera : ", " ============ height : ===========" + height );
//        Log.d("Camera : ", " ============ data : ===========" + data[300] );
//
//        int[] position = new int[4];
//        boolean bRet = ProcFrame(data, width, height, position);
//        return bRet;
//    }

    public static Bitmap renderCroppedGreyscaleBitmap(byte[] yuvData, int width, int height) {
        int[] pixels = new int[width * height];
        int inputOffset = 0;

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuvData[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += width;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static byte[] GetByteImageData(Bitmap bmp, int nImgW, int nImgH){
        int nWideWidth = bmp.getRowBytes();
        int nSize = nWideWidth * nImgH;

        ByteBuffer byteImgData = ByteBuffer.allocate(nSize);
        bmp.copyPixelsToBuffer(byteImgData);

        byte[] arrayImgData = byteImgData.array();

        nSize = 3 * nImgW * nImgH;
        byte[] byRGB = new byte[nSize];

        int iy1, iy2, jx1, jx2;
        for (int i = 0; i < nImgH; i++) {
            iy1 = i * 3 * nImgW;
            iy2 = i * nWideWidth;
            for (int j = 0; j < nImgW; j++) {
                jx1 = j * 3;
                jx2 = j * 4;
                byRGB[iy1 + jx1] = arrayImgData[iy2 + jx2 + 2];
                byRGB[iy1 + jx1 + 1] = arrayImgData[iy2 + jx2 + 1];
                byRGB[iy1 + jx1 + 2] = arrayImgData[iy2 + jx2];
            }
        }

        return byRGB;
    }

    //If fail, empty string.
    //public static boolean doRunData(byte[] data, int width, int height, int rot, int crop_l , int crop_t, int crop_r, int crop_b, int[] position){
    public static boolean doRunData(byte[] data, int width, int height, int rot, int crop_l , int crop_t, int crop_r, int crop_b, int[] position){
        boolean bRet = false;
//        long startTime = System.currentTimeMillis();
//        //int ret = doRecogYuv420p(data, width, height, facepick,rot,intData, faceBitmap,unknown);
//        Bitmap bmpFrame = renderCroppedGreyscaleBitmap(data, width, height);//getColorOrgBitmap(data, width, height);
//        final Bitmap bmpFinal = bmpFrame.getWidth() > bmpFrame.getHeight()? rotateBitmap(bmpFrame, 90) : bmpFrame;

//        Bitmap croppedBmp = Bitmap.createBitmap(bmpFinal, bmpFinal.getWidth() / 2, 0, bmpFinal.getWidth() / 2 - 10, bmpFinal.getHeight() / 2 - 10);
//        mStrQRcode = ProcessImage.scanQRImage(croppedBmp);
//        if( mStrQRcode.equals("") == false ) {
//            Utils.bitmapToMat(bmpFinal, ProcessImage.subAnswer);
//            result = true;
//            return result;
//        }
//        else {
//            result = false;
//            return result;
//        }

//        int w = bmpFinal.getWidth();
//        int h = bmpFinal.getHeight();
//        byte[] byImgData = Util.GetByteImageData(bmpFinal, w, h);
//
//        Log.d("Camera : ", " ============ data : ===========" + data[300] );

        Log.d("Camera : ", " ============ width : ===========" + width );
        Log.d("Camera : ", " ============ height : ===========" + height );
        Log.d("Crop : ", " ============ crop_l : ===========" + crop_l );
        Log.d("Crop : ", " ============ crop_t : ===========" + crop_t );
        Log.d("Crop : ", " ============ crop_r : ===========" + crop_r );
        Log.d("Crop : ", " ============ crop_b : ===========" + crop_b );
        //int[] position = new int[4];
        bRet = ProcFrame(data, width, height, position, crop_l, crop_t, crop_r, crop_b);

        Log.d("Result : ", " ============ left : ===========" + position[0] );
        Log.d("Result : ", " ============ right : ===========" + position[1] );
        Log.d("Result : ", " ============ top : ===========" + position[2] );
        Log.d("Result : ", " ============ bottom : ===========" + position[3] );
        return bRet;
    }

    //If fail, empty string.
    //public static boolean doRunDataRGB(byte[] data, int width, int height, int[] position, int crop_l , int crop_t, int crop_r, int crop_b){
    public static boolean doRunDataRGB(Bitmap bmpFrame, int[] position, int cl , int ct, int cr, int cb){
        boolean bRet = false;
        int frameW = bmpFrame.getWidth();
        int frameH = bmpFrame.getHeight();
        final Bitmap bmpFinal = frameW > frameH? rotateBitmap(bmpFrame, 90) : bmpFrame;
        byte[] data = GetByteImageData(bmpFinal, bmpFinal.getWidth(), bmpFinal.getHeight());

        Log.d("Camera : ", " ============ width : ===========" + bmpFinal.getWidth() );
        Log.d("Camera : ", " ============ height : ===========" + bmpFinal.getHeight() );
        Log.d("Crop : ", " ============ crop_l : ===========" + cl );
        Log.d("Crop : ", " ============ crop_t : ===========" + ct );
        Log.d("Crop : ", " ============ crop_r : ===========" + cr );
        Log.d("Crop : ", " ============ crop_b : ===========" + cb );
        //int[] position = new int[4];
        bRet = ProcFrameRGB(data, bmpFinal.getWidth(), bmpFinal.getHeight(), position, cl, ct, cr, cb);

        Log.d("Result : ", " ============ left : ===========" + position[0] );
        Log.d("Result : ", " ============ right : ===========" + position[1] );
        Log.d("Result : ", " ============ top : ===========" + position[2] );
        Log.d("Result : ", " ============ bottom : ===========" + position[3] );
        return bRet;
    }
    // byImgData : rgb data, w : image width, h : image hight
    // Return : position[0]-left, postion[1]-top, position[2]-right, position[3]-bottom
    public static native boolean ProcFrame(byte[] byImgData, int w, int h, int[] position, int crop_l, int crop_t, int crop_r, int crop_b);

    public static native boolean ProcFrameRGB(byte[] byImgData, int w, int h, int[] position, int crop_l, int crop_t, int crop_r, int crop_b);
}
