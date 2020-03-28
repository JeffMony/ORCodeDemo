package com.jeffmony.orcode;

public interface CaptureLifecycle {

    void onCreate();

    void onResume();

    void onPause();

    void onDestroy();
}
