package com.jeffmony.orcode;

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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;
import com.jeffmony.orcode.camera.CameraManager;
import com.jeffmony.orcode.utils.LogUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

    private final Context mContext;
    private final CameraManager mCameraManager;
    private final Map<DecodeHintType, Object> mHints;
    private Handler mHandler;
    private CaptureHandler mCaptureHandler;
    private final CountDownLatch mHandlerInitLatch;

    DecodeThread(Context context, CameraManager cameraManager,
                 CaptureHandler captureHandler,
                 Collection<BarcodeFormat> decodeFormats,
                 Map<DecodeHintType, Object> baseHints,
                 String characterSet,
                 ResultPointCallback resultPointCallback) {
        this.mContext = context;
        this.mCameraManager = cameraManager;
        this.mCaptureHandler = captureHandler;
        mHandlerInitLatch = new CountDownLatch(1);

        mHints = new EnumMap<>(DecodeHintType.class);
        if (baseHints != null) {
            mHints.putAll(baseHints);
        }

        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_1D_PRODUCT, true)) {
                decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
            }
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_1D_INDUSTRIAL, true)) {
                decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            }
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_QR, true)) {
                decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            }
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_DATA_MATRIX, true)) {
                decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            }
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_AZTEC, false)) {
                decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
            }
            if (prefs.getBoolean(PreferenceKeys.KEY_DECODE_PDF417, false)) {
                decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
            }
        }
        mHints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            mHints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }
        mHints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
        LogUtils.i("DecodeThread", "Hints: " + mHints);
    }

    Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return mHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new DecodeHandler(mContext, mCameraManager, mCaptureHandler, mHints);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

}