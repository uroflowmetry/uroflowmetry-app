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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CameraProfile;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.uroflowmetry.R;
import com.uroflowmetry.base.BaseActivity;
import com.uroflowmetry.camera.CameraHolder;
import com.uroflowmetry.camera.CameraUtil;
import com.uroflowmetry.camera.FocusManager;
import com.uroflowmetry.camera.FocusManager.Listener;
import com.uroflowmetry.engine.EngineUroflowmetry;
import com.uroflowmetry.library.widget.DrawView;

import java.io.IOException;
import java.util.Collections;
import java.util.List;



@SuppressWarnings("ALL")
public class MeasureVolumeActivity extends BaseActivity implements
		SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ShutterCallback,
		Camera.PictureCallback, Listener, OnTouchListener
{

	public static void showActivity(Activity parent)
	{
		Intent intent = new Intent(parent, MeasureVolumeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		parent.startActivity(intent);
	}

	private static int m_requestCode;
	public static void showActivity(Activity parent, int requestCode)
	{
		m_requestCode = requestCode;
		Intent intent = new Intent(parent, MeasureVolumeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		parent.startActivityForResult(intent, requestCode);
	}

	public DrawView drawView;

	protected Camera mCameraDevice;
	// The first rear facing camera
	Parameters 	mParameters;
	private Parameters mInitialParams;
	SurfaceHolder mSurfaceHolder;

	private static final int PREVIEW_STOPPED = 0;
	protected static final int IDLE = 1; // preview is active
	// Focus is in progress. The exact focus state is in Focus.java.
	private static final int FOCUSING = 2;
	private static final int SNAPSHOT_IN_PROGRESS = 3;
	private static final int SELFTIMER_COUNTING = 4;
	protected static final int SAVING_PICTURES = 5;
	private int mCameraState = PREVIEW_STOPPED;

	private static boolean LOGV = true;
	private static final String TAG = "MeasureActivity";

	private int mCameraId;
	private boolean mOpenCameraFail = false;
	private boolean mCameraDisabled = false;
	private boolean mOnResumePending;
	private boolean mPausing;
	private boolean mFirstTimeInitialized;
	private int mSurfaceW = 0;
	private int mSurfaceH = 0;

	private static final int mPreviewWidth = 1280;//640;
	private static final int mPreviewHeight = 960;//480;

	private static final int FIRST_TIME_INIT = 2;
	private static final int CLEAR_SCREEN_DELAY = 3;
	private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;

	// number clear
	private static final int TRIGER_RESTART_RECOG = 5;
	private static final int TRIGER_RESTART_RECOG_DELAY = 30; // ms

	// The subset of parameters we need to update in setCameraParameters().
	private static final int UPDATE_PARAM_INITIALIZE = 1;
	private static final int UPDATE_PARAM_PREFERENCE = 4;
	private static final int UPDATE_PARAM_ALL = -1;

	// When setCameraParametersWhenIdle() is called, we accumulate the subsets
	// needed to be updated in mUpdateSet.
	private int mUpdateSet;

	// This handles everything about focus.
	FocusManager mFocusManager;

	ImageView uiFlashButton;
	private View mPreviewFrame; // Preview frame area for SurfaceView.
	private TextView mModeView, mPreviewSizeView, mPictureSizeView;

	private MediaPlayer mPlayer = null;
	private boolean mbVibrate;
	private static final long VIBRATE_DURATION = 200L;

	public boolean mbProcEngine = false;
	private int     mnCountEngine = 0;

	// The display rotation in degrees. This is only valid when mCameraState is
	// not PREVIEW_STOPPED.
	private int mDisplayRotation;
	// The value for android.hardware.Camera.setDisplayOrientation.
	private int mDisplayOrientation;

	private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
	final Handler mHandler = new MainHandler();

	private int _cropL, _cropT, _cropR, _cropB;

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
	{
        if (LOGV) Log.v(TAG, "onWindowFocusChanged.hasFocus=" + hasFocus
                + ".mOnResumePending=" + mOnResumePending);
        if (hasFocus && mOnResumePending)
        {
            doOnResume();
            mOnResumePending = false;
        }
    }
	@Override
	protected void onResume()
	{
		super.onResume();

        mbProcEngine = false;
		mbVibrate = true;
        if (LOGV) Log.v(TAG, "onResume. hasWindowFocus()=" + hasWindowFocus());
        if (mCameraDevice == null)
        {// && isKeyguardLocked()) {
            if (LOGV) Log.v(TAG, "onResume. mOnResumePending=true");
            mOnResumePending = true;
        }
        else
		{
            if (LOGV) Log.v(TAG, "onResume. mOnResumePending=false");
			int currentSDKVersion = Build.VERSION.SDK_INT;

			doOnResume();
            mOnResumePending = false;
        }
	}
	protected void doOnResume()
	{
		if (mOpenCameraFail || mCameraDisabled)
			return;

		// if (mRecogService != null && mRecogService.isProcessing())
		// showProgress(null);

		mPausing = false;

		// Start the preview if it is not started.
		if (mCameraState == PREVIEW_STOPPED) {
			try {
				mCameraDevice = CameraUtil.openCamera(this, mCameraId);
				initializeCapabilities();

				refreshCamera(mCameraDevice);
				startPreview();
			}
			catch (Exception e) {
                CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
				return;
			}
		}

		if (mSurfaceHolder != null) {
			// If first time initialization is not finished, put it in the
			// message queue.
			if (!mFirstTimeInitialized) {
				mHandler.sendEmptyMessage(FIRST_TIME_INIT);
			} else {
				initializeSecondTime();
			}
		}

		keepScreenOnAwhile();
		Log.i(TAG, "doOnresume end");
	}

	Thread mCameraOpenThread = new Thread(new Runnable() {
		public void run() {
			try {
				mCameraDevice = CameraUtil.openCamera(MeasureVolumeActivity.this, mCameraId);
			}
			catch (Exception e) {
				mOpenCameraFail = true;
				mCameraDisabled = true;
			}
		}
	});

	Thread mCameraPreviewThread = new Thread(new Runnable() {
		public void run() {
			initializeCapabilities();
			startPreview();
		}
	});

	// Snapshots can only be taken after this is called. It should be called
	// once only. We could have done these things in onCreate() but we want to
	// make preview screen appear as soon as possible.
	private void initializeFirstTime() {
		if (mFirstTimeInitialized)
			return;

//		mOrientationListener = new MyOrientationEventListener(this);
//		mOrientationListener.enable();

		mCameraId = CameraHolder.instance().getBackCameraId();

        CameraUtil.initializeScreenBrightness(getWindow(), getContentResolver());
		mFirstTimeInitialized = true;
	}

	// If the activity is paused and resumed, this method will be called in
	// onResume.
	private void initializeSecondTime() {
		//mOrientationListener.enable();
	}

	@Override
	public int getResID() {
		return R.layout.activity_camera;
	}

	private class MainHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CLEAR_SCREEN_DELAY: {
				getWindow().clearFlags(
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			}
			case FIRST_TIME_INIT: {
				initializeFirstTime();
				break;
			}

			case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
				setCameraParametersWhenIdle(0);
				break;
			}

			case TRIGER_RESTART_RECOG:
				if (!mPausing)
					mCameraDevice.setOneShotPreviewCallback(MeasureVolumeActivity.this);
				// clearNumberAreaAndResult();
				break;
			}
		}
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		keepScreenOnAwhile();
	}

	private void resetScreenOn() {
		mHandler.removeMessages(CLEAR_SCREEN_DELAY);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void keepScreenOnAwhile() {
		mHandler.removeMessages(CLEAR_SCREEN_DELAY);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);

		mCameraId = CameraHolder.instance().getBackCameraId();
		//String str = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
		String[] defaultFocusModes = {   "continuous-video", "auto", "continuous-picture"};
		mFocusManager = new FocusManager(defaultFocusModes);

		/*
		 * To reduce startup time, we start the camera open and preview threads.
		 * We make sure the preview is started at the end of onCreate.
		 */
		mCameraOpenThread.start();

		// Hide the window title.


		drawView = findViewById(R.id.drawView);

		//mPreviewFrame = findViewById(R.id.camera_preview);
		mPreviewFrame.setOnTouchListener(this);

		mModeView = (TextView) findViewById(R.id.mode);
		mPreviewSizeView = (TextView) findViewById(R.id.preview_size);
		mPictureSizeView = (TextView) findViewById(R.id.picture_size);
		uiFlashButton = (ImageView)findViewById(R.id.button_flash);
		uiFlashButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				turnOnFlashligh(true);
			}
		});
		requestCameraPermission();

		mPlayer = new MediaPlayer();
		AssetFileDescriptor descriptor;
		try {
			descriptor = getAssets().openFd("beep.ogg");//"beep.wav");
			mPlayer.setDataSource(descriptor.getFileDescriptor(),
					descriptor.getStartOffset(), descriptor.getLength());
			mPlayer.prepare();
			descriptor.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Make sure camera device is opened.
		try {
			mCameraOpenThread.join();
			mCameraOpenThread = null;
			if (mOpenCameraFail) {
                CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
				return;
			} else if (mCameraDisabled) {
                CameraUtil.showErrorAndFinish(this, R.string.camera_disabled);
				return;
			}
		} catch (InterruptedException ex) {
			// ignore
		}

		mCameraPreviewThread.start();

		// do init
		// initializeZoomMax(mInitialParams);

		// Make sure preview is started.
		try {
			mCameraPreviewThread.join();
		} catch (InterruptedException ex) {
			// ignore
		}
		mCameraPreviewThread = null;

		SurfaceHolder holder = ((SurfaceView)mPreviewFrame).getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}

	@Override
	public void onDestroy() {

		// finalize the scan engine.
		super.onDestroy();
		// unregister receiver.
	}

	public void requestCameraPermission()
	{
		int currentapiVersion = Build.VERSION.SDK_INT;
		if (currentapiVersion >= Build.VERSION_CODES.M){
			if (ContextCompat.checkSelfPermission(this,
					Manifest.permission.CAMERA)
					!= PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this,
						Manifest.permission.CAMERA)) {

					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.CAMERA},
							1);

				} else {
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.CAMERA},
							1);
				}
			}
			else
			{

			}

		} else{

			// do something for phones running an SDK before lollipop
		}
	}
	private void initializeCapabilities() {

		mInitialParams = mCameraDevice.getParameters();
		mInitialParams.getFocusMode();
		mFocusManager.initializeParameters(mInitialParams);

		if (mCameraDevice != null)
			mParameters = mCameraDevice.getParameters();
	}

	private void startPreview() {

		if (mPausing || isFinishing())
			return;

		mCameraDevice.setErrorCallback(mErrorCallback);

		// If we're previewing already, stop the preview first (this will blank
		// the screen).
		if (mCameraState != PREVIEW_STOPPED)
			stopPreview();

		setPreviewDisplay(mSurfaceHolder);
		setDisplayOrientation();

		mCameraDevice.setOneShotPreviewCallback(MeasureVolumeActivity.this);
		setCameraParameters(UPDATE_PARAM_ALL);

		double ratio[] = CameraUtil.getPreviewRatio(mPreviewFrame, mParameters);


		// Inform the mainthread to go on the UI initialization.
		if (mCameraPreviewThread != null)
		{
			synchronized (mCameraPreviewThread)
			{
				mCameraPreviewThread.notify();
			}
		}

		try {
			Log.v(TAG, "startPreview");
			mCameraDevice.startPreview();
		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("startPreview failed", ex);
		}

		setCameraState(IDLE);

		// notify again to make sure main thread is wake-up.
		if (mCameraPreviewThread != null)
		{
			synchronized (mCameraPreviewThread)
			{
				mCameraPreviewThread.notify();
			}
		}

		boolean auto_flash = false;
		if(auto_flash)//GlobalProfile.auto_flash
		{
			Handler handler = new Handler(Looper.getMainLooper())
			{
				@Override
				public void handleMessage(Message msg)
				{
					mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
					mCameraDevice.setParameters(mParameters);
					uiFlashButton.setImageResource(R.drawable.flash_off);
				}
			};
			handler.sendEmptyMessageAtTime(0, 500);

		}
		else
		{
			uiFlashButton.setImageResource(R.drawable.flash_on);
		}

		autoFocus();
	}

	private void setPreviewDisplay(SurfaceHolder holder) {
		try {
			mCameraDevice.setPreviewDisplay(holder);
		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("setPreviewDisplay failed", ex);
		}
	}

	private void setDisplayOrientation() {
		mDisplayRotation = CameraUtil.getDisplayRotation(this);
		mDisplayOrientation = CameraUtil.getDisplayOrientation(mDisplayRotation, mCameraId);
		mCameraDevice.setDisplayOrientation(mDisplayOrientation);
	}

	private void stopPreview() {
		if (mCameraDevice == null)
			return;
		mCameraDevice.stopPreview();
		// mCameraDevice.setPreviewCallback(null);
		setCameraState(PREVIEW_STOPPED);
	}

	private void setCameraState(int state) {
		mCameraState = state;
	}

	private void closeCamera() {
		if (mCameraDevice != null) {
			CameraHolder.instance().release();
			mCameraDevice.setErrorCallback(null);
			mCameraDevice = null;
			setCameraState(PREVIEW_STOPPED);
			mFocusManager.onCameraReleased();
		}
	}

	@Override
	protected void onPause() {
		Log.e(TAG, "onPause");

        mOnResumePending = false;
		mPausing = true;

		mIsAutoFocusCallback = false;

		stopPreview();

		// Close the camera now because other activities may need to use it.
		closeCamera();
		resetScreenOn();

		// Remove the messages in the event queue.
		mHandler.removeMessages(FIRST_TIME_INIT);
		mHandler.removeMessages(TRIGER_RESTART_RECOG);

//		if (mFirstTimeInitialized)
//			mOrientationListener.disable();

		super.onPause();
	}

	public void refreshCamera(Camera camera) {
		if (mSurfaceHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}
		// stop preview before making changes
		try {
			mCameraDevice.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings
		mCameraDevice = camera;
		mParameters = mCameraDevice.getParameters();

		Camera.Size original = mParameters.getPreviewSize();
		int camOri = CameraHolder.instance().getCameraInfo()[mCameraId].orientation;

		// Set a preview size that is closest to the viewfinder height and has the right aspect ratio.
		List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
		Camera.Size optimalSize;
		double dSurfaceW2H = (double)mSurfaceH / (double)mSurfaceW;
		double dPreDiff = dSurfaceW2H;
		Log.d(TAG, "================       surface ratio for h and w   =========================");
		Log.d(TAG, "dSurfaceW2H = " + dSurfaceW2H);

		optimalSize = sizes.get(0);
		for (Camera.Size size : sizes) {
			double dCamW2H =  (double)size.width / (double)size.height;
			Log.d(TAG, "================       camera sizes    =========================");
			Log.d(TAG, " Camera = [" + size.width + "x" + size.height + "]");
			Log.d(TAG, "dCamW2H = " + dCamW2H);

			double dDiff = Math.abs(dCamW2H - dSurfaceW2H);
			if (dDiff < dPreDiff) {
				optimalSize = size;
				dPreDiff = dDiff;
			}
		}

		Log.i(TAG, " Sensor[" + mCameraId + "]'s orientation is " + camOri);
		if (!original.equals(optimalSize)) {
			Log.d(TAG, "================       optimalSize sizes    =========================");
			Log.d(TAG, " Camera = [" + optimalSize.width + "x" + optimalSize.height + "]");
			if (camOri == 0 || camOri == 180) {
				mParameters.setPreviewSize(optimalSize.height, optimalSize.width);
			} else {
				mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
			}

			// Zoom related settings will be changed for different preview
			// sizes, so set and read the parameters to get lastest values
			mCameraDevice.setParameters(mParameters);
		}

		try {
			mCameraDevice.setPreviewDisplay(mSurfaceHolder);

			mParameters = mCameraDevice.getParameters();
			Camera.Size size = mParameters.getPictureSize();

			String previewSize = "";
			previewSize = " Camera = [" + size.width + "x" + size.height + "]" + "\n" + " Surface = [" + mSurfaceW + "x" + mSurfaceH + "]";
			mPreviewSizeView.setText(previewSize);

			startPreview();
		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Make sure we have a surface in the holder before proceeding.
		if (holder.getSurface() == null) {
			Log.d(TAG, "holder.getSurface() == null");
			return;
		}

		mSurfaceW = width;
		mSurfaceH = height;
		Log.d("Surface : ", " ============ width : ===========" + mSurfaceW );
		Log.d("Surface : ", " ============ height : ===========" + mSurfaceH );

//		String previewSize = "";
//		previewSize = " Camera = [" + mCamW + "x" + mCamH + "]" + "\n" + " Surface = [" + mSurfaceW + "x" + mSurfaceH + "]";
//		mPreviewSizeView.setText(previewSize);

//		if( mCamW != 0 && mCamH != 0 ){
//
//			width = mCamW * height / mCamH;
//
//			holder.setFixedSize(width, height);
//		}

		// We need to save the holder for later use, even when the mCameraDevice
		// is null. This could happen if onResume() is invoked after this
		// function.
		mSurfaceHolder = holder;

		// The mCameraDevice will be null if it fails to connect to the camera
		// hardware. In this case we will show a dialog and then finish the
		// activity, so it's OK to ignore it.
		if (mCameraDevice == null)
			return;

		// Sometimes surfaceChanged is called after onPause or before onResume.
		// Ignore it.
		if (mPausing || isFinishing())
			return;

		Parameters parameters = mCameraDevice.getParameters();
		Camera.Size size = parameters.getPictureSize();
		if(size.width != 0){
			double dRatioCame = (double)size.height / (double)size.width;
			double dRatioSurface = (double)mSurfaceW / (double)mSurfaceH;

			if( dRatioCame != dRatioSurface )
				refreshCamera(mCameraDevice);
		}

		RectF rtBorder = new RectF();
		_cropL = width / 6;
		_cropT = height / 6;
		_cropR = 5 * width / 6;
		_cropB = 5 * height / 6;
		rtBorder.left = _cropL;
		rtBorder.top = _cropT;
		rtBorder.right = _cropR;
		rtBorder.bottom = _cropB;
		drawView.drawFilledBorder(rtBorder);

		// Set preview display if the surface is being created. Preview was
		// already started. Also restart the preview if display rotation has
		// changed. Sometimes this happens when the device is held in portrait
		// and camera app is opened. Rotation animation takes some time and
		// display rotation in onCreate may not be what we want.
		if (mCameraState == PREVIEW_STOPPED) {
			startPreview();
		} else {
			if (CameraUtil.getDisplayRotation(this) != mDisplayRotation) {
				setDisplayOrientation();
			}
			if (holder.isCreating()) {
				// Set preview display if the surface is being created and
				// preview
				// was already started. That means preview display was set to
				// null
				// and we need to set it now.
				setPreviewDisplay(holder);
			}
		}

		// If first time initialization is not finished, send a message to do
		// it later. We want to finish surfaceChanged as soon as possible to let
		// user see preview first.
		if (!mFirstTimeInitialized) {
			mHandler.sendEmptyMessage(FIRST_TIME_INIT);
		} else {
			initializeSecondTime();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		stopPreview();
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onShutter() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPreviewFrame(final byte[] data, Camera camera) {
		// TODO Auto-generated method stub
//		Log.e(TAG, "onPreviewFrame mPausing=" + mPausing + ", mCameraState=" + mCameraState);

		if (mPausing)
			return;

		if (mCameraState != IDLE)
		{
			mCameraDevice.setOneShotPreviewCallback(MeasureVolumeActivity.this);
			return;
		}
		// generate jpeg image.
		final int width = camera.getParameters().getPreviewSize().width;
		final int height = camera.getParameters().getPreviewSize().height;

		Thread recogThread = new Thread(new Runnable()
		{
			boolean ret =false;
			@Override
			public void run() {

			    if( mbProcEngine == false ) {
                    mbProcEngine = true;
                    double ratio[] = CameraUtil.getPreviewRatio(mPreviewFrame, mParameters);
					int crop_l , crop_t, crop_r, crop_b;
					crop_l = (int)(_cropL / ratio[0]);
					crop_t = (int)(_cropT / ratio[1]);
					crop_r = (int)(_cropR / ratio[0]);
					crop_b = (int)(_cropB / ratio[1]);

                    int[] position = new int[4];
                    ret = EngineUroflowmetry.doRunData(data, width, height, mDisplayRotation, crop_l, crop_t, crop_r, crop_b, position);

                    MeasureVolumeActivity.this.runOnUiThread(() -> {
						mbProcEngine = false;
						if (ret == true )
						{
							RectF rtModel = new RectF();

							int left = (int)(position[0] * ratio[0]);
							int top = (int)(position[1] * ratio[1]);
							int right = (int)(position[2] * ratio[0]);
							int bottom = (int)(position[3] * ratio[1]);
							rtModel.left = left;
							rtModel.top = top;
							rtModel.right = right;
							rtModel.bottom = bottom;

							drawView.drawDesireArea(rtModel);

							//showResultActivity();
							//return;
						}
						//else
						{
							mHandler.sendMessageDelayed(mHandler.obtainMessage(TRIGER_RESTART_RECOG),TRIGER_RESTART_RECOG_DELAY);
							//mCameraDevice.setOneShotPreviewCallback(CameraActivity.this);
						}
					});
                }
			}
		});
		recogThread.start();
	}

	private static boolean isSupported(String value, List<String> supported) {
		return supported == null ? false : supported.indexOf(value) >= 0;
	}

	private void updateCameraParametersInitialize() {
		// Reset preview frame rate to the maximum because it may be lowered by
		// video camera application.
		List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
		if (frameRates != null) {
			Integer max = Collections.max(frameRates);
			mParameters.setPreviewFrameRate(max);
		}

		//mParameters.setRecordingHint(false);

		// Disable video stabilization. Convenience methods not available in API
		// level <= 14
		String vstabSupported = mParameters.get("video-stabilization-supported");
		if ("true".equals(vstabSupported)) {
			mParameters.set("video-stabilization", "false");
		}
	}

	private void updateCameraParametersPreference() {

		mParameters = mCameraDevice.getParameters();

		// Set JPEG quality.
		int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(mCameraId, CameraProfile.QUALITY_HIGH);
		mParameters.setJpegQuality(jpegQuality);

		// For the following settings, we need to check if the settings are
		// still supported by latest driver, if not, ignore the settings.

		//if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode))
		{

			// Set white balance parameter.
			String whiteBalance = "auto";
			if (isSupported(whiteBalance,
					mParameters.getSupportedWhiteBalance())) {
				mParameters.setWhiteBalance(whiteBalance);
			}

			String focusMode = mFocusManager.getFocusMode();
			mParameters.setFocusMode(focusMode);

			// Set exposure compensation
			int value = 0;
			int max = mParameters.getMaxExposureCompensation();
			int min = mParameters.getMinExposureCompensation();
			if (value >= min && value <= max) {
				mParameters.setExposureCompensation(value);
			} else {
				Log.w(TAG, "invalid exposure range: " + value);
			}
		}

		// Set flash mode.
		String flashMode = "off";
		List<String> supportedFlash = mParameters.getSupportedFlashModes();
		if (isSupported(flashMode, supportedFlash)) {
			mParameters.setFlashMode(flashMode);
		}

		Log.e(TAG, "focusMode=" + mParameters.getFocusMode());

	}
	// We separate the parameters into several subsets, so we can update only
	// the subsets actually need updating. The PREFERENCE set needs extra
	// locking because the preference can be changed from GLThread as well.
	private void setCameraParameters(int updateSet) {
		mParameters = mCameraDevice.getParameters();

		if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
			updateCameraParametersInitialize();
		}

		if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
			updateCameraParametersPreference();
			mIsAutoFocusCallback = false;
		}

		mCameraDevice.setParameters(mParameters);
	}

	private boolean mIsAutoFocusCallback = false;
	private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();

	private final class AutoFocusCallback implements
			Camera.AutoFocusCallback {
		public void onAutoFocus(boolean focused, Camera camera) {
			if (mPausing)
				return;

			if (mCameraState == FOCUSING) {
				setCameraState(IDLE);
			}
			mFocusManager.onAutoFocus(focused);
			mIsAutoFocusCallback = true;

			String focusMode = mFocusManager.getFocusMode();
			mParameters.setFocusMode(focusMode);
			mCameraDevice.setParameters(mParameters);
		}
	}

	@Override
	public void autoFocus() {
		// TODO Auto-generated method stub

		/*
		if(FocusManager.isSupported(Parameters.FOCUS_MODE_AUTO, mParameters.getSupportedFocusModes()))
		{
			mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);

			mCameraDevice.setParameters(mParameters);
			mCameraDevice.autoFocus(mAutoFocusCallback);
			setCameraState(FOCUSING);
		}
		*/
	}

	@Override
	public void cancelAutoFocus() {
		// TODO Auto-generated method stub
		mCameraDevice.cancelAutoFocus();
		if (mCameraState != SELFTIMER_COUNTING
				&& mCameraState != SNAPSHOT_IN_PROGRESS) {
			setCameraState(IDLE);
		}
		setCameraParameters(UPDATE_PARAM_PREFERENCE);
	}

	@Override
	public boolean capture() {
		// If we are already in the middle of taking a snapshot then ignore.
		if (mCameraState == SNAPSHOT_IN_PROGRESS || mCameraDevice == null) {
			return false;
		}
		setCameraState(SNAPSHOT_IN_PROGRESS);

		return true;
	}

	@Override
	public void setFocusParameters() {
		// TODO Auto-generated method stub
		setCameraParameters(UPDATE_PARAM_PREFERENCE);
	}

	@Override
	public void playSound(int soundId) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		 if (mPausing || mCameraDevice == null || !mFirstTimeInitialized
		 || mCameraState == SNAPSHOT_IN_PROGRESS
		 || mCameraState == PREVIEW_STOPPED
		 || mCameraState == SAVING_PICTURES)
		 {
		 	return false;
		 }

		 String focusMode = mParameters.getFocusMode();
		 if (focusMode == null || Parameters.FOCUS_MODE_INFINITY.equals(focusMode))
		 {
			 return false;
		 }

		 if(e.getAction() == MotionEvent.ACTION_UP)
		 {
			 autoFocus();
		 }

		//
		//return mFocusManager.onTouch(e);

		return true;
	}

	// If the Camera is idle, update the parameters immediately, otherwise
	// accumulate them in mUpdateSet and update later.
	private void setCameraParametersWhenIdle(int additionalUpdateSet) {
		mUpdateSet |= additionalUpdateSet;
		if (mCameraDevice == null) {
			// We will update all the parameters when we open the device, so
			// we don't need to do anything now.
			mUpdateSet = 0;
			return;
		} else if (isCameraIdle()) {
			setCameraParameters(mUpdateSet);
			mUpdateSet = 0;
		} else {
			if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
				mHandler.sendEmptyMessageDelayed(
						SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
			}
		}
	}

	private boolean isCameraIdle() {
		return (mCameraState == IDLE || mFocusManager.isFocusCompleted());
	}

	public class CameraErrorCallback  implements Camera.ErrorCallback {
		private static final String TAG = "CameraErrorCallback";

		public void onError(int error, Camera camera) {
			Log.e(TAG, "Got camera error callback. error=" + error);
			if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
				// We are not sure about the current state of the app (in preview or
				// snapshot or recording). Closing the app is better than creating a
				// new Camera object.
				throw new RuntimeException("Media server died.");
			}
		}
	}

	public void showResultActivity()
	{
//		Intent intent = new Intent(this, EvaluatorActivity.class);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		this.startActivityForResult(intent, m_requestCode);

		//Intent returnIntent = new Intent();
		//returnIntent.putExtra("processMode", processMode);
		//setResult(Activity.RESULT_OK, returnIntent);
		//finishActivity(MainActivity);
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == m_requestCode)
		{
			if(resultCode == Activity.RESULT_OK)
			{
				setResult(Activity.RESULT_OK);
			}

			finish();
		}
	}

	public void turnOnFlashligh(boolean bOn)
	{
		if(mCameraDevice == null) return;

		//Camera.Parameters cameraParams = mCameraDevice.getParameters();

		if(mParameters.getFlashMode().equals(Parameters.FLASH_MODE_OFF))
		//if(bOn)
		{
			mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
			uiFlashButton.setImageResource(R.drawable.flash_off);
		}
		else
		{
			mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			uiFlashButton.setImageResource(R.drawable.flash_on);
		}

		mCameraDevice.setParameters(mParameters);
	}

	@Override
	public void onBackPressed() {
		restartActivity(MainActivity.class);
	}
}
