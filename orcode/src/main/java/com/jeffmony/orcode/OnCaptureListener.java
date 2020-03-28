package com.jeffmony.orcode;

import android.graphics.Bitmap;

import com.google.zxing.Result;

public interface OnCaptureListener {
    void onHandleDecode(Result result, Bitmap barcode, float scaleFactor);
}
