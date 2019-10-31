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
    private Paint paint;
    private RectF rectangleModel;

    int textX, textY;
    private int offBorder = 50;
    private Paint paintBorder;
    private RectF rtBorder;

    //Bottle Setting
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final RectF trackedPos = new RectF();

    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private float textSizePx;
    private int sensorOrientation;


    private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();
    /** Interface defining the callback for client classes. */
    public interface DrawCallback {
        public void drawCallback(final Canvas canvas);
    }

    public void addCallback(final DrawCallback callback) {
        callbacks.add(callback);
    }

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

        paintBorder = new Paint();
        paintBorder.setColor(ContextCompat.getColor(mContext, R.color.red));
        paintBorder.setStrokeWidth(6);
        paintBorder.setStyle(Paint.Style.STROKE);

        paint = new Paint();
        paint.setColor(ContextCompat.getColor(mContext, R.color.green));
        paint.setStrokeWidth(6);
        paint.setStyle(Paint.Style.STROKE);

        boxPaint.setColor(Color.BLUE);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setPathEffect(new DashPathEffect(new float[]{10, 40}, 0));
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(rtBorder != null){
            float x0 = rtBorder.left;
            float y0 = rtBorder.top;
            float x1 = rtBorder.right;
            float y1 = rtBorder.bottom;
            canvas.drawLine(x0, y0,  x0 + offBorder, y0, paintBorder);
            canvas.drawLine(x0, y0,  x0, y0 + offBorder, paintBorder);

            canvas.drawLine(x1 - offBorder, y0,  x1, y0, paintBorder);
            canvas.drawLine(x1, y0,  x1, y0 + offBorder, paintBorder);

            canvas.drawLine(x0, y1,  x0 + offBorder, y1, paintBorder);
            canvas.drawLine(x0, y1,  x0, y1 - offBorder, paintBorder);

            canvas.drawLine(x1 - offBorder, y1,  x1, y1, paintBorder);
            canvas.drawLine(x1, y1,  x1, y1 - offBorder, paintBorder);
        }

        if(rectangleModel != null){
            textX = getWidth() / 2;
            textY = getHeight() / 2;

            canvas.drawRect(rectangleModel.left, rectangleModel.top, rectangleModel.right, rectangleModel.bottom, paint);
        }

        for (final DrawCallback callback : callbacks) {
            callback.drawCallback(canvas);
        }

        //Draw bottle area
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier = Math.min(getHeight() / (float) (rotated ? frameWidth : frameHeight), getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
                frameWidth,
                frameHeight,
                (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                sensorOrientation,
                false);
        for (final TrackedRecognition recognition : trackedObjects) {
            trackedPos.top = recognition.location.top;
            trackedPos.left = recognition.location.left;
            trackedPos.right = recognition.location.right;
            trackedPos.bottom = recognition.location.bottom;

            getFrameToCanvasMatrix().mapRect(trackedPos);
            canvas.drawRoundRect(trackedPos, 0, 0, boxPaint);
        }
    }

    public synchronized void setFrameConfigurationForBottleDetect(
            final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public void drawDesireArea(RectF rectangleModel){
        this.rectangleModel = rectangleModel;

        invalidate();
    }

    public void drawFilledBorder(RectF rtBorder){
        this.rtBorder = rtBorder;

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
                if((currentTime - emptyBottleDetectedTime) > 3000){ // if it doesn't detect bottle for 3 seconds, clear all last rectangle  for bottle
                    trackedObjects.clear();
                    screenRects.clear();

                    new Handler(Looper.getMainLooper()).post(this::invalidate);
                }

            }else {
                emptyBottleDetectedTime = currentTime;
            }

            return;
        }else {
            emptyBottleDetectedTime = -1;
            trackedObjects.clear();
            screenRects.clear();
        }


        for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
            trackedObjects.add(trackedRecognition);
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
