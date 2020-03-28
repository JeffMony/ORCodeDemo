package com.jeffmony.orcode.utils;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "ZXing-Code";

    private static final int LOG_VERBOSE = 1;
    private static final int LOG_DEBUG = 2;
    private static final int LOG_INFO = 3;
    private static final int LOG_WARN = 4;
    private static final int LOG_ERROR = 5;
    private static int sLogLevel = LOG_DEBUG;

    public static void v(String tag, String msg) {
        if (sLogLevel >= LOG_VERBOSE) {
            Log.v(tag, msg);
        }
    }

    public static void v(String msg) {
        v(TAG, msg);
    }

    public static void d(String tag, String msg) {
        if (sLogLevel >= LOG_DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void i(String tag, String msg) {
        if (sLogLevel >= LOG_INFO) {
            Log.i(tag, msg);
        }
    }

    public static void i(String msg) {
        i(TAG, msg);
    }


    public static void w(String tag, String msg, Throwable e) {
        if (sLogLevel >= LOG_WARN) {
            Log.w(tag, msg, e);
        }
    }

    public static void w(String tag, String msg) {
        if (sLogLevel >= LOG_WARN) {
            Log.w(tag, msg);
        }
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (sLogLevel >= LOG_ERROR) {
            Log.e(tag, msg);
        }
    }

    public static void e(String msg) {
        d(TAG, msg);
    }
}
