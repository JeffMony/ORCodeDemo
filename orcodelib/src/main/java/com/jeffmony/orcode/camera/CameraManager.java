package com.jeffmony.orcode.camera;

/*
 * Copyright (C) 2008 ZXing authors
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

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;

import androidx.annotation.FloatRange;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.jeffmony.orcode.utils.LogUtils;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private final Context mContext;
    private final CameraConfigurationManager mConfigManager;
    private OpenCamera mCamera;
    private AutoFocusManager mAutoFocusManager;
    private Rect mFramingRect;
    private Rect mFramingRectInPreview;
    private boolean mInitialized;
    private boolean mPreviewing;
    private int mRequestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private int mRequestedFramingRectWidth;
    private int mRequestedFramingRectHeight;
    private boolean mIsFullScreenScan;

    private float mFramingRectRatio;
    private int mFramingRectVerticalOffset;
    private int mFramingRectHorizontalOffset;

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback mPreviewCallback;

    private OnTorchListener mOnTorchListener;
    private OnSensorListener mOnSensorListener;

    private boolean mIsTorch;

    public CameraManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mConfigManager = new CameraConfigurationManager(context);
        mPreviewCallback = new PreviewCallback(mConfigManager);
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = mCamera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(mRequestedCameraId);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            mCamera = theCamera;
        }

        if (!mInitialized) {
            mInitialized = true;
            mConfigManager.initFromCameraParameters(theCamera);
            if (mRequestedFramingRectWidth > 0 && mRequestedFramingRectHeight > 0) {
                setManualFramingRect(mRequestedFramingRectWidth, mRequestedFramingRectHeight);
                mRequestedFramingRectWidth = 0;
                mRequestedFramingRectHeight = 0;
            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            mConfigManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            LogUtils.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            LogUtils.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    mConfigManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    LogUtils.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);

    }

    public synchronized boolean isOpen() {
        return mCamera != null;
    }

    public OpenCamera getOpenCamera() {
        return mCamera;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public void closeDriver() {
        if (mCamera != null) {
            mCamera.getCamera().release();
            mCamera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            mFramingRect = null;
            mFramingRectInPreview = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public void startPreview() {
        OpenCamera theCamera = mCamera;
        if (theCamera != null && !mPreviewing) {
            theCamera.getCamera().startPreview();
            mPreviewing = true;
            mAutoFocusManager = new AutoFocusManager(mContext, theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public void stopPreview() {
        if (mAutoFocusManager != null) {
            mAutoFocusManager.stop();
            mAutoFocusManager = null;
        }
        if (mCamera != null && mPreviewing) {
            mCamera.getCamera().stopPreview();
            mPreviewCallback.setHandler(null, 0);
            mPreviewing = false;
        }
    }

    /**
     * Convenience method for {@link com.jeffmony.orcode.CaptureActivity}
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        OpenCamera theCamera = mCamera;
        if (theCamera != null && newSetting != mConfigManager.getTorchState(theCamera.getCamera())) {
            boolean wasAutoFocusManager = mAutoFocusManager != null;
            if (wasAutoFocusManager) {
                mAutoFocusManager.stop();
                mAutoFocusManager = null;
            }
            this.mIsTorch = newSetting;
            mConfigManager.setTorch(theCamera.getCamera(), newSetting);
            if (wasAutoFocusManager) {
                mAutoFocusManager = new AutoFocusManager(mContext, theCamera.getCamera());
                mAutoFocusManager.start();
            }

            if(mOnTorchListener!=null){
                mOnTorchListener.onTorchChanged(newSetting);
            }

        }

    }


    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = mCamera;
        if (theCamera != null && mPreviewing) {
            mPreviewCallback.setHandler(handler, message);
            theCamera.getCamera().setOneShotPreviewCallback(mPreviewCallback);
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (mFramingRect == null) {
            if (mCamera == null) {
                return null;
            }
            Point point = mConfigManager.getCameraResolution();
            if (point == null) {
                // Called early, before init even finished
                return null;
            }

            int width = point.x;
            int height = point.y;

            if(mIsFullScreenScan){
                mFramingRect = new Rect(0,0,width,height);
            }else{
                int size = (int)(Math.min(width,height) * mFramingRectRatio);

                int leftOffset = (width - size) / 2 + mFramingRectHorizontalOffset;
                int topOffset = (height - size) / 2 + mFramingRectVerticalOffset;
                mFramingRect = new Rect(leftOffset, topOffset, leftOffset + size, topOffset + size);
            }

        }
        return mFramingRect;
    }


    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     */
    public synchronized Rect getFramingRectInPreview() {
        if (mFramingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = mConfigManager.getCameraResolution();
            Point screenResolution = mConfigManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;

            mFramingRectInPreview = rect;
        }

        return mFramingRectInPreview;
    }

    public void setFullScreenScan(boolean fullScreenScan) {
        mIsFullScreenScan = fullScreenScan;
    }

    public void setFramingRectRatio(@FloatRange(from = 0.0f ,to = 1.0f) float framingRectRatio) {
        this.mFramingRectRatio = framingRectRatio;
    }

    public void setFramingRectVerticalOffset(int framingRectVerticalOffset) {
        this.mFramingRectVerticalOffset = framingRectVerticalOffset;
    }

    public void setFramingRectHorizontalOffset(int framingRectHorizontalOffset) {
        this.mFramingRectHorizontalOffset = framingRectHorizontalOffset;
    }

    public Point getCameraResolution() {
        return mConfigManager.getCameraResolution();
    }

    public Point getScreenResolution() {
        return mConfigManager.getScreenResolution();
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        mRequestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (mInitialized) {
            Point screenResolution = mConfigManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            LogUtils.d(TAG, "Calculated manual framing rect: " + mFramingRect);
            mFramingRectInPreview = null;
        } else {
            mRequestedFramingRectWidth = width;
            mRequestedFramingRectHeight = height;
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }

        if(mIsFullScreenScan){
            return new PlanarYUVLuminanceSource(data,width,height,0,0,width,height,false);
        }
        int size = (int)(Math.min(width,height) * mFramingRectRatio);
        int left = (width-size)/2 + mFramingRectHorizontalOffset;
        int top = (height-size)/2 + mFramingRectVerticalOffset;
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, left, top,
                size, size, false);
    }

    /**
     * 提供闪光灯监听
     * @param listener
     */
    public void setOnTorchListener(OnTorchListener listener){
        this.mOnTorchListener = listener;
    }

    /**
     * 传感器光线照度监听
     * @param listener
     */
    public void setOnSensorListener(OnSensorListener listener){
        this.mOnSensorListener = listener;
    }

    public void sensorChanged(boolean tooDark,float ambientLightLux){
        if(mOnSensorListener!=null){
            mOnSensorListener.onSensorChanged(mIsTorch,tooDark,ambientLightLux);
        }
    }

    public interface OnTorchListener{
        /**
         * 当闪光灯状态改变时触发
         * @param torch true表示开启、false表示关闭
         */
        void onTorchChanged(boolean torch);
    }


    /**
     * 传感器灯光亮度监听
     */
    public interface OnSensorListener{
        /**
         *
         * @param torch 闪光灯是否开启
         * @param tooDark  传感器检测到的光线亮度，是否太暗
         * @param ambientLightLux 光线照度
         */
        void onSensorChanged(boolean torch, boolean tooDark, float ambientLightLux);
    }


}