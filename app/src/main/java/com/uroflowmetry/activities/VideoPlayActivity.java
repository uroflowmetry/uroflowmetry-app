package com.uroflowmetry.activities;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import com.uroflowmetry.R;
import com.uroflowmetry.engine.EngineUroflowmetry;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class VideoPlayActivity extends MeasureActivity {

    private String filePath;
    private TextureView videoView;

    private MediaPlayer mMediaPlayer;

    private Bitmap _bmpFrame = null;


    @Override
    public int getResID() {
        return R.layout.activity_video;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePath = getIntent().getStringExtra("filePath");

        videoView = findViewById(R.id.videoView);
        videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

                Surface surface = new Surface(surfaceTexture);

                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setDataSource(VideoPlayActivity.this, Uri.parse(filePath));
                    mMediaPlayer.setSurface(surface);
                    mMediaPlayer.setLooping(false);

                    // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
                    // creating MediaPlayer
                    mMediaPlayer.prepareAsync();
                    // Play video when the media source is ready for playback.
                    mMediaPlayer.setOnPreparedListener(mediaPlayer -> {
                        mediaPlayer.start();

                        Bitmap bmFrame = videoView.getBitmap();

                        int frameW = bmFrame.getWidth();
                        int frameH = bmFrame.getHeight();

                        double dScaleX = (double) videoView.getWidth() / (double)frameW;
                        double dScaleY = (double) videoView.getHeight() / (double)frameH;

                        double[] ratio = new double[2];
                        ratio[0] = dScaleX;
                        ratio[1] = dScaleY;

                        preInitialize(videoView.getWidth(), videoView.getHeight(), frameW, frameH, ratio, 0);
                        startWork();

                        new Thread(){
                            @Override
                            public void run() {
                                while (isAvailableToWork){
                                    detectBottle();
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }.start();
                    });

                    mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                        endWork();
                        finish();
                    });

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    @Override
    public Bitmap getBitmapSource() {
        _bmpFrame = videoView.getBitmap();
        return _bmpFrame;
    }
    @Override
    public Bitmap getBitmapFrame() {
        _bmpFrame = videoView.getBitmap();
        return _bmpFrame;
    }
}
