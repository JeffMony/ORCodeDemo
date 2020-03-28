package com.jeffmony.orcode.camera;
/*
 * Copyright (C) 2012 ZXing authors
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
import android.hardware.Camera;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.jeffmony.orcode.PreferenceKeys;
import com.jeffmony.orcode.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

@SuppressWarnings("deprecation") // camera APIs
final class AutoFocusManager implements Camera.AutoFocusCallback {

    private static final String TAG = AutoFocusManager.class.getSimpleName();

    private static final long AUTO_FOCUS_INTERVAL_MS = 1200L;
    private static final Collection<String> FOCUS_MODES_CALLING_AF;
    static {
        FOCUS_MODES_CALLING_AF = new ArrayList<>(2);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
    }

    private boolean mStopped;
    private boolean mFocusing;
    private final boolean mUseAutoFocus;
    private final Camera mCamera;
    private AsyncTask<?,?,?> mOutstandingTask;

    AutoFocusManager(Context context, Camera camera) {
        this.mCamera = camera;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentFocusMode = camera.getParameters().getFocusMode();
        mUseAutoFocus =
                sharedPrefs.getBoolean(PreferenceKeys.KEY_AUTO_FOCUS, true) &&
                        FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
        LogUtils.i(TAG, "Current focus mode '" + currentFocusMode + "'; use auto focus? " + mUseAutoFocus);
        start();
    }

    @Override
    public synchronized void onAutoFocus(boolean success, Camera theCamera) {
        mFocusing = false;
        autoFocusAgainLater();
    }

    private synchronized void autoFocusAgainLater() {
        if (!mStopped && mOutstandingTask == null) {
            AutoFocusTask newTask = new AutoFocusTask(this);
            try {
                newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mOutstandingTask = newTask;
            } catch (RejectedExecutionException ree) {
                LogUtils.w(TAG, "Could not request auto focus", ree);
            }
        }
    }

    synchronized void start() {
        if (mUseAutoFocus) {
            mOutstandingTask = null;
            if (!mStopped && !mFocusing) {
                try {
                    mCamera.autoFocus(this);
                    mFocusing = true;
                } catch (RuntimeException re) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    LogUtils.w(TAG, "Unexpected exception while focusing", re);
                    // Try again later to keep cycle going
                    autoFocusAgainLater();
                }
            }
        }
    }

    private synchronized void cancelOutstandingTask() {
        if (mOutstandingTask != null) {
            if (mOutstandingTask.getStatus() != AsyncTask.Status.FINISHED) {
                mOutstandingTask.cancel(true);
            }
            mOutstandingTask = null;
        }
    }

    synchronized void stop() {
        mStopped = true;
        if (mUseAutoFocus) {
            cancelOutstandingTask();
            // Doesn't hurt to call this even if not focusing
            try {
                mCamera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                LogUtils.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }
    }

    private static class AutoFocusTask extends AsyncTask<Object, Object, Object> {

        private WeakReference<AutoFocusManager> weakReference;

        public AutoFocusTask(AutoFocusManager manager){
            weakReference = new WeakReference<>(manager);
        }

        @Override
        protected Object doInBackground(Object... voids) {
            try {
                Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
            } catch (InterruptedException e) {
                // continue
            }
            AutoFocusManager manager = weakReference.get();
            if(manager!=null){
                manager.start();
            }
            return null;
        }
    }

}