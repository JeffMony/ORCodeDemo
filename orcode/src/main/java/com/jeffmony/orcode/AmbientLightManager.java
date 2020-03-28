package com.jeffmony.orcode;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

import com.jeffmony.orcode.camera.CameraManager;
import com.jeffmony.orcode.camera.FrontLightMode;

/**
 * Detects ambient light and switches on the front light when very dark, and off again when sufficiently light.
 *
 * @author Sean Owen
 * @author Nikolaus Huber
 */
final class AmbientLightManager implements SensorEventListener {

    protected static final float TOO_DARK_LUX = 45.0f;
    protected static final float BRIGHT_ENOUGH_LUX = 100.0f;

    /**
     * 光线太暗时，默认：照度45 lux
     */
    private float mTooDarkLux = TOO_DARK_LUX;
    /**
     * 光线足够亮时，默认：照度450 lux
     */
    private float mBrightEnoughLux = BRIGHT_ENOUGH_LUX;

    private final Context mContext;
    private CameraManager mCameraManager;
    private Sensor mLightSensor;

    AmbientLightManager(Context context) {
        this.mContext = context;
    }

    void start(CameraManager cameraManager) {
        this.mCameraManager = cameraManager;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (FrontLightMode.readPref(sharedPrefs) == FrontLightMode.AUTO) {
            SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            mLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (mLightSensor != null) {
                sensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    void stop() {
        if (mLightSensor != null) {
            SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
            mCameraManager = null;
            mLightSensor = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float ambientLightLux = sensorEvent.values[0];
        if (mCameraManager != null) {
            if (ambientLightLux <= mTooDarkLux) {
                mCameraManager.sensorChanged(true,ambientLightLux);
            } else if (ambientLightLux >= mBrightEnoughLux) {
                mCameraManager.sensorChanged(false,ambientLightLux);
            }
        }
    }

    public void setTooDarkLux(float tooDarkLux){
        this.mTooDarkLux = tooDarkLux;
    }

    public void setBrightEnoughLux(float brightEnoughLux){
        this.mBrightEnoughLux = brightEnoughLux;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

}