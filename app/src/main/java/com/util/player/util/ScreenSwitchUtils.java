package com.util.player.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class ScreenSwitchUtils {

    @SuppressLint("StaticFieldLeak")
    private static ScreenSwitchUtils mInstance;
    private static final int ORIENTATION_SUCCESS = 1;

    private static final Object lock = new Object();

    private OrientationSensorListener mOrientationSensorListener;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Activity mActivity;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ORIENTATION_SUCCESS) {
                int orientation = msg.arg1;
                if (null == mActivity)
                    return;
                int requestedOrientation = mActivity.getRequestedOrientation();
                if (orientation > 45 && orientation < 135) {
                    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else if (orientation > 225 && orientation < 315) {
                    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        }
    };


    private ScreenSwitchUtils(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (null != mSensorManager) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mOrientationSensorListener = new OrientationSensorListener(handler);
            mSensorManager.registerListener(mOrientationSensorListener, mSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public static ScreenSwitchUtils $(Context context) {
        if (mInstance == null) {
            synchronized (lock) {
                if (mInstance == null) {
                    mInstance = new ScreenSwitchUtils(context);
                }
            }
        }
        return mInstance;
    }


    public class OrientationSensorListener implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        static final int ORIENTATION_UNKNOWN = -1;

        private Handler rotateHandler;


        OrientationSensorListener(Handler handler) {
            rotateHandler = handler;
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {

        }

        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            if (magnitude * 4 >= Z * Z) {
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }

            if (rotateHandler != null) {
                rotateHandler.obtainMessage(ORIENTATION_SUCCESS, orientation, 0).sendToTarget();
            }
        }

    }

    /**
     * 开始监听
     */

    public void start(Context activity) {
        mActivity = (Activity) activity;
        if (null != mSensorManager && null != mOrientationSensorListener && null != mSensor)
            mSensorManager.registerListener(mOrientationSensorListener, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * 停止监听
     */
    public void stop() {
        if (null != mSensorManager && null != mOrientationSensorListener)
            mSensorManager.unregisterListener(mOrientationSensorListener);
    }
}