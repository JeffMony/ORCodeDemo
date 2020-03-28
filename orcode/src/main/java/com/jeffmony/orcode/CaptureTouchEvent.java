package com.jeffmony.orcode;

import android.view.MotionEvent;

public interface CaptureTouchEvent {

    /**
     * {@link android.app.Activity#onTouchEvent(MotionEvent)}
     */
    boolean onTouchEvent(MotionEvent event);
}
