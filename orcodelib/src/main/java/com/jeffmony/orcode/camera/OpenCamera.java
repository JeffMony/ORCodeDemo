package com.jeffmony.orcode.camera;

/*
 * Copyright (C) 2015 ZXing authors
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

import android.hardware.Camera;

/**
* Represents an open {@link Camera} and its metadata, like facing direction and orientation.
*/
@SuppressWarnings("deprecation") // camera APIs
public final class OpenCamera {

    private final int mIndex;
    private final Camera mCamera;
    private final CameraFacing mCameraFacing;
    private final int mOrientation;

    public OpenCamera(int index, Camera camera, CameraFacing facing, int orientation) {
        this.mIndex = index;
        this.mCamera = camera;
        this.mCameraFacing = facing;
        this.mOrientation = orientation;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public CameraFacing getFacing() {
        return mCameraFacing;
    }

    public int getOrientation() {
        return mOrientation;
    }

    @Override
    public String toString() {
        return "Camera #" + mIndex + " : " + mCameraFacing + ',' + mOrientation;
    }

}