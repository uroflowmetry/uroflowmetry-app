package com.uroflowmetry.library.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.uroflowmetry.R;
import com.uroflowmetry.bottledetect.tflite.Classifier;
import com.uroflowmetry.library.utils.ImageUtils;

import java.util.LinkedList;
import java.util.List;


public class DrawView extends View {

    Context mContext;

    private Paint mPaintMeasure;
    private RectF mRtMeasure;

    //int textX, textY;
    private int mOffBorder = 50;
    private Paint mPaintBorder;
    private RectF mRtBorder;

    //Bottle Setting
    private final List<TrackedRecognition> mTrackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint mPaintBottle = new Paint();
    //private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final RectF mRtBottle = new RectF();

    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    //private float textSizePx;
    //private int sensorOrientation;

    public DrawView(Context context) {
        super(context);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;

        mPaintBorder = new Paint();
        mPaintBorder.setColor(ContextCompat.getColor(mContext, R.color.red));
        mPaintBorder.setStrokeWidth(6);
        mPaintBorder.setStyle(Paint.Style.STROKE);

        mPaintMeasure = new Paint();
        mPaintMeasure.setColor(ContextCompat.getColor(mContext, R.color.green));
        mPaintMeasure.setStrokeWidth(6);
        mPaintMeasure.setStyle(Paint.Style.STROKE);

        mPaintBottle.setColor(Color.BLUE);
        mPaintBottle.setStyle(Paint.Style.STROKE);
        mPaintBottle.setPathEffect(new DashPathEffect(new float[]{10, 40}, 0));
        mPaintBottle.setStrokeWidth(10.0f);
        mPaintBottle.setStrokeCap(Paint.Cap.ROUND);
        mPaintBottle.setStrokeJoin(Paint.Join.ROUND);
        mPaintBottle.setStrokeMiter(100);

        //textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mRtBorder != null){
            float x0 = mRtBorder.left;
            float y0 = mRtBorder.top;
            float x1 = mRtBorder.right;
            float y1 = mRtBorder.bottom;
            canvas.drawLine(x0, y0,  x0 + mOffBorder, y0, mPaintBorder);
            canvas.drawLine(x0, y0,  x0, y0 + mOffBorder, mPaintBorder);

            canvas.drawLine(x1 - mOffBorder, y0,  x1, y0, mPaintBorder);
            canvas.drawLine(x1, y0,  x1, y0 + mOffBorder, mPaintBorder);

            canvas.drawLine(x0, y1,  x0 + mOffBorder, y1, mPaintBorder);
            canvas.drawLine(x0, y1,  x0, y1 - mOffBorder, mPaintBorder);

            canvas.drawLine(x1 - mOffBorder, y1,  x1, y1, mPaintBorder);
            canvas.drawLine(x1, y1,  x1, y1 - mOffBorder, mPaintBorder);
        }

        if(mRtMeasure != null && mRtMeasure.width() > 0 && mRtMeasure.height() > 0 ){
            //textX = getWidth() / 2;
            //textY = getHeight() / 2;

            canvas.drawRect(mRtMeasure.left, mRtMeasure.top, mRtMeasure.right, mRtMeasure.bottom, mPaintMeasure);
        }

        //Draw bottle area
        for (final TrackedRecognition recognition : mTrackedObjects) {
            mRtBottle.top = recognition.location.top;
            mRtBottle.left = recognition.location.left;
            mRtBottle.right = recognition.location.right;
            mRtBottle.bottom = recognition.location.bottom;

            getFrameToCanvasMatrix().mapRect(mRtBottle);
            canvas.drawRoundRect(mRtBottle, 0, 0, mPaintBottle);
        }
    }

    public synchronized void setFrameConfigurationForBottleDetect(final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        //this.sensorOrientation = sensorOrientation;

        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier = Math.min(getHeight() / (float) (rotated ? frameWidth : frameHeight), getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
                frameWidth,
                frameHeight,
                (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                sensorOrientation, false);
    }

    public void drawDesireArea(RectF rtMeasure){
        this.mRtMeasure = rtMeasure;

        invalidate();
    }

    public void drawFilledBorder(RectF rtBorder){
        this.mRtBorder = rtBorder;

        invalidate();
    }

    private long emptyBottleDetectedTime = -1;
    public void processDetectedBottleResults(final List<Classifier.Recognition> results) {
        final List<Pair<Float, Classifier.Recognition>> rectsToTrack = new LinkedList<Pair<Float, Classifier.Recognition>>();

        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        for (final Classifier.Recognition result : results) {
            if (result.getLocation() == null) {
                continue;
            }
            final RectF detectionFrameRect = new RectF(result.getLocation());

            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

            screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                //logger.w("Degenerate rectangle! " + detectionFrameRect);
                continue;
            }

            rectsToTrack.add(new Pair<Float, Classifier.Recognition>(result.getConfidence(), result));
        }

        final long currentTime = System.currentTimeMillis();
        if (rectsToTrack.isEmpty()) {
            //logger.v("Nothing to track, aborting.");

            if(emptyBottleDetectedTime > 0){
                if((currentTime - emptyBottleDetectedTime) > 1000){ // if it doesn't detect bottle for 3 seconds, clear all last rectangle  for bottle
                    mTrackedObjects.clear();
                    screenRects.clear();

                    new Handler(Looper.getMainLooper()).post(this::invalidate);
                }

            }else {
                emptyBottleDetectedTime = currentTime;
            }

            return;
        }else {
            emptyBottleDetectedTime = -1;
            mTrackedObjects.clear();
            screenRects.clear();
        }

        for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
            mTrackedObjects.add(trackedRecognition);
        }
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        String title;
    }
}
