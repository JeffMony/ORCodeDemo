package com.jeffmony.orcode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.jeffmony.orcode.camera.CameraManager;

import java.util.Collection;
import java.util.Map;

public class CaptureHandler extends Handler implements ResultPointCallback {

    private static final String TAG = CaptureHandler.class.getSimpleName();

    private final OnCaptureListener mOnCaptureListener;
    private final DecodeThread mDecodeThread;
    private State mState;
    private final CameraManager mCameraManager;
    private final Activity mActivity;
    private final ViewfinderView mViewfinderView;
    /**
     * 是否支持垂直的条形码
     */
    private boolean mIsSupportVerticalCode;

    /**
     * 是否返回扫码原图
     */
    private boolean mIsReturnBitmap;

    /**
     * 是否支持自动缩放
     */
    private boolean mIsSupportAutoZoom;

    private boolean mIsSupportLuminanceInvert;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    CaptureHandler(Activity activity, ViewfinderView viewfinderView, OnCaptureListener onCaptureListener,
                   Collection<BarcodeFormat> decodeFormats,
                   Map<DecodeHintType, Object> baseHints,
                   String characterSet,
                   CameraManager cameraManager) {
        this.mActivity = activity;
        this.mViewfinderView = viewfinderView;
        this.mOnCaptureListener = onCaptureListener;
        mDecodeThread = new DecodeThread(activity,cameraManager,this, decodeFormats, baseHints, characterSet, this);
        mDecodeThread.start();
        mState = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.mCameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.restart_preview) {
            restartPreviewAndDecode();

        } else if (message.what == R.id.decode_succeeded) {
            mState = State.SUCCESS;
            Bundle bundle = message.getData();
            Bitmap barcode = null;
            float scaleFactor = 1.0f;
            if (bundle != null) {
                byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                if (compressedBitmap != null) {
                    barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                    // Mutable copy:
                    barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                }
                scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
            }
            mOnCaptureListener.onHandleDecode((Result) message.obj, barcode, scaleFactor);


        } else if (message.what == R.id.decode_failed) {// We're decoding as fast as possible, so when one decode fails, start another.
            mState = State.PREVIEW;
            mCameraManager.requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);

        }
    }

    public void quitSynchronously() {
        mState = State.DONE;
        mCameraManager.stopPreview();
        Message quit = Message.obtain(mDecodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            mDecodeThread.join(100L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    public void restartPreviewAndDecode() {
        if (mState == State.SUCCESS) {
            mState = State.PREVIEW;
            mCameraManager.requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
            mViewfinderView.drawViewfinder();
        }
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        if(mViewfinderView!=null){
            ResultPoint resultPoint = transform(point);
            mViewfinderView.addPossibleResultPoint(resultPoint);
        }
    }

    private boolean isScreenPortrait(Context context){
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point screenResolution = new Point();
        display.getSize(screenResolution);
        return screenResolution.x < screenResolution.y;
    }

    /**
     *
     * @return
     */
    private ResultPoint transform(ResultPoint originPoint) {
        Point screenPoint = mCameraManager.getScreenResolution();
        Point cameraPoint = mCameraManager.getCameraResolution();

        float scaleX;
        float scaleY;
        float x;
        float y;

        if(screenPoint.x < screenPoint.y){
            scaleX = 1.0f * screenPoint.x / cameraPoint.y;
            scaleY = 1.0f * screenPoint.y / cameraPoint.x;

            x = originPoint.getX() * scaleX - Math.max(screenPoint.x,cameraPoint.y)/2;
            y = originPoint.getY() * scaleY - Math.min(screenPoint.y,cameraPoint.x)/2;
        }else{
            scaleX = 1.0f * screenPoint.x / cameraPoint.x;
            scaleY = 1.0f * screenPoint.y / cameraPoint.y;

            x = originPoint.getX() * scaleX - Math.min(screenPoint.y,cameraPoint.y)/2;
            y = originPoint.getY() * scaleY - Math.max(screenPoint.x,cameraPoint.x)/2;
        }


        return new ResultPoint(x,y);
    }

    public boolean isSupportVerticalCode() {
        return mIsSupportVerticalCode;
    }

    public void setSupportVerticalCode(boolean supportVerticalCode) {
        mIsSupportVerticalCode = supportVerticalCode;
    }

    public boolean isReturnBitmap() {
        return mIsReturnBitmap;
    }

    public void setReturnBitmap(boolean returnBitmap) {
        mIsReturnBitmap = returnBitmap;
    }

    public boolean isSupportAutoZoom() {
        return mIsSupportAutoZoom;
    }

    public void setSupportAutoZoom(boolean supportAutoZoom) {
        mIsSupportAutoZoom = supportAutoZoom;
    }

    public boolean isSupportLuminanceInvert() {
        return mIsSupportLuminanceInvert;
    }

    public void setSupportLuminanceInvert(boolean supportLuminanceInvert) {
        mIsSupportLuminanceInvert = supportLuminanceInvert;
    }
}
