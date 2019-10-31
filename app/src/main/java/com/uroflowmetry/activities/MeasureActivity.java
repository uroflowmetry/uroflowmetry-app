/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uroflowmetry.activities;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.TypedValue;
import android.view.Surface;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.uroflowmetry.R;
import com.uroflowmetry.base.BaseActivity;
import com.uroflowmetry.bottledetect.env.BorderedText;
import com.uroflowmetry.bottledetect.tflite.Classifier;
import com.uroflowmetry.bottledetect.tflite.TFLiteObjectDetectionAPIModel;
import com.uroflowmetry.engine.EngineUroflowmetry;
import com.uroflowmetry.library.utils.DateTimeUtils;
import com.uroflowmetry.library.utils.ImageUtils;
import com.uroflowmetry.library.widget.DrawView;
import com.uroflowmetry.models.AppStorage;
import com.uroflowmetry.models.BottleModel;
import com.uroflowmetry.models.DataModel;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class MeasureActivity extends BaseActivity {

    private boolean computingDetection = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private Classifier detector;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    private BorderedText borderedText;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private static final boolean MAINTAIN_ASPECT = false;

    private Bitmap croppedBitmap = null;

    protected DrawView drawView;
    protected int _viewW, _viewH;
    protected int _frameW, _frameH;
    protected double[] _ratio = new double[2];

    protected int _cropL, _cropT, _cropR, _cropB;

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.4f;

    private RectF detectedBottleArea = null;

    private BottleModel bottleModel;
    private DataModel dataModel;
    private final int MEASURERING_NOT_STAETED = -1;
    private final int MEASURERING_STAETED = 1;
    private final int MEASURERING_ENDED = 2;
    private final int MEASURERING_STOPPED = 3;
    private final int MEASURERING_RESUMED = 4;
    private int isStartedMeasuring = MEASURERING_NOT_STAETED;  // -1: is not started, 1 : started, 2 : ended
    private int bottleHeight = 0;
    private int lastMeasuredHeight = 0;
    private int lastMeasuredWidth = 0;
    private int lastTimeInMilliSeconds = 0;
    private int currentMeasuredHeight = 0;
    private int currentMeasuredWidth = 0;

    private Handler mHandler;
    private TextView infoViewer;
    private final int speedUpdateDuration = 1; // seconds

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawView = findViewById(R.id.drawView);
        infoViewer = findViewById(R.id.bottleSpecViewer);

        bottleModel = AppStorage.Companion.build(this).getCurrentBottleModel();

        infoViewer.setText(bottleModel.getVolumeRateString());

        mHandler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(isStartedMeasuring == MEASURERING_STAETED){
                    int diffHeight = currentMeasuredHeight - lastMeasuredHeight;
                    float rateOfRealHeightAndPixel = (bottleModel.getVolume()* 1f / bottleHeight );
                    int voidedVolume = (int) (rateOfRealHeightAndPixel * currentMeasuredHeight);
                    float speed = (rateOfRealHeightAndPixel * diffHeight) / speedUpdateDuration;

                    if(speed < 0) speed = 0f;
                    dataModel.getValues().add(speed);
                    String infoMsg =
                            String.format("Bottle Volume: %dml\n", bottleModel.getVolume()) +
                                    String.format("Filled Volume: %dml\n", voidedVolume) +
                                    String.format("Flow Rate: %.2fml/s", speed);
                    infoViewer.setText(infoMsg);
                    lastMeasuredHeight = currentMeasuredHeight;
                    mHandler.sendEmptyMessageDelayed(0, speedUpdateDuration * 1000);
                }
            }
        };
    }

    @Override
    public synchronized void onResume() {
        //LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        //LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            //LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    protected void preInitialize(int viewW, int viewH, int frameW, int frameH, double[] ratio,  int rotation){
        _viewW = viewW;
        _viewH = viewH;

        _frameW = frameW;
        _frameH = frameH;

        _ratio = ratio;

        RectF rtBorder = new RectF();
        _cropL = this._viewW / 6;
        _cropT = this._viewH / 6;
        _cropR = 5 * this._viewW / 6;
        _cropB = 5 * this._viewH / 6;
        rtBorder.left = _cropL;
        rtBorder.top = _cropT;
        rtBorder.right = _cropR;
        rtBorder.bottom = _cropB;
        drawView.drawFilledBorder(rtBorder);

        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            //LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        sensorOrientation = rotation - getScreenOrientation();
        //LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        //LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        //rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(_frameW, _frameH, cropSize, cropSize, sensorOrientation, MAINTAIN_ASPECT);
        //frameToCropTransform = ImageUtils.getTransformationMatrix(_viewW, _viewH, cropSize, cropSize, sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        //drawView.setFrameConfigurationForBottleDetect(_frameW, _frameH, sensorOrientation);
        drawView.setFrameConfigurationForBottleDetect(_frameW, _frameH, sensorOrientation);
    }

    protected void detectBottle() {
        //drawView.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }

        computingDetection = true;

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(getBitmapSource(), frameToCropTransform, null);
        // For examining the actual TF input.

        readyForNextImage();

        runInBackground(() -> {
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

            final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API && result.getTitle().equals("bottle")) {

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);

                    //Check if including of marked area
                    //if(location.top > _cropT && location.left > _cropL && location.right < _cropR && location.bottom < _cropB){
                        mappedRecognitions.add(result);
                        detectedBottleArea = location;
                        break; // for getting only 1 bottle
                    //}
                }
            }

            drawView.processDetectedBottleResults(mappedRecognitions);
            drawView.postInvalidate();

            computingDetection = false;

        });
    }

    protected void readyForNextImage() {

    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected boolean isAvailableToWork = false;
    protected boolean _bProcEngine = false;
    private final int OFFSET_MARGINE_FOR_AREA = 50;

    protected void startWork(){

        isAvailableToWork = true;

        new Thread(){
            @Override
            public void run() {
                while (isAvailableToWork){

                    if( !_bProcEngine) {

                        _bProcEngine = true;
                        int crop_l , crop_t, crop_r, crop_b;

                        if(detectedBottleArea != null){

                            float offsetedPosition = detectedBottleArea.left - OFFSET_MARGINE_FOR_AREA;
                            if(offsetedPosition < 0){
                                offsetedPosition = 0;
                            }
                            crop_l = (int)(offsetedPosition);// / _ratio[0]);

                            offsetedPosition = detectedBottleArea.top - OFFSET_MARGINE_FOR_AREA;
                            if(offsetedPosition < 0){
                                offsetedPosition = 0;
                            }
                            crop_t = (int)( offsetedPosition);// / _ratio[1]);

                            offsetedPosition = detectedBottleArea.right + OFFSET_MARGINE_FOR_AREA;
                            if(offsetedPosition >= _frameW){
                                offsetedPosition = _frameW - 5;
                            }
                            crop_r = (int)(offsetedPosition);// / _ratio[0]);

                            offsetedPosition = detectedBottleArea.bottom + OFFSET_MARGINE_FOR_AREA;
                            if(offsetedPosition >= _frameH){
                                offsetedPosition = _frameH - 5;
                            }
                            crop_b = (int)(offsetedPosition);// / _ratio[1]);

                            Bitmap bmpFrame = getBitmapFrame();
                            int cl, ct, cr, cb;
                            if( _frameW > _frameH ){
                                cl = _frameH - crop_b;
                                ct = crop_l;
                                cr = _frameH - crop_t;
                                cb = crop_r;
                            }
                            else{
                                cl = crop_l;
                                ct = crop_t;
                                cr = crop_r;
                                cb = crop_b;
                            }

                            int[] position = new int[4];
                            boolean bRet = EngineUroflowmetry.doRunDataRGB(bmpFrame, position, cl, ct, cr, cb);

                            if (bRet)
                            {
                                RectF rtModel = new RectF();

                                int left = (int)(position[0] * _ratio[0]);
                                int top = (int)(position[1] * _ratio[1]);
                                int right = (int)(position[2] * _ratio[0]);
                                int bottom = (int)(position[3] * _ratio[1]);
                                rtModel.left = left;
                                rtModel.top = top;
                                rtModel.right = right;
                                rtModel.bottom = bottom;

                                runOnUiThread(() -> drawView.drawDesireArea(rtModel));

                                if(isStartedMeasuring == MEASURERING_NOT_STAETED){
                                    isStartedMeasuring = MEASURERING_STAETED;

                                    dataModel = new DataModel();
                                    dataModel.setMStartedTime(DateTimeUtils.getTimestamp());
                                    runOnUiThread(() -> Toast.makeText(MeasureActivity.this, "Measuring Start", Toast.LENGTH_SHORT).show());
                                    mHandler.sendEmptyMessage(0);
                                }

                                if(isStartedMeasuring == MEASURERING_STAETED){
                                    currentMeasuredHeight = bottom - top;
                                    currentMeasuredWidth = right - left;
                                    bottleHeight = (int) (currentMeasuredWidth / bottleModel.getRateOfWH());
                                    dataModel.setMEndedTime(DateTimeUtils.getTimestamp());
                                }
                            }
                        }
                        _bProcEngine = false;
                    }
                }
            }
        }.start();

    }

    protected void endWork(){

        isAvailableToWork = false;
        isStartedMeasuring = MEASURERING_ENDED;

        if(dataModel != null){
            float rateOfRealHeightAndPixel = (bottleModel.getVolume()* 1f / bottleHeight );
            int voidedVolume = (int) (rateOfRealHeightAndPixel * currentMeasuredHeight);
            dataModel.setVoidedVolume(voidedVolume);
            AppStorage.Companion.build(MeasureActivity.this).saveDataModel(dataModel);
        }
    }

    public abstract Bitmap getBitmapFrame();
    public abstract Bitmap getBitmapSource();
}
