package com.wix.reactnativenotifications.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.LinkedList;
import java.util.List;

public class JsIOHelper {

    private static final LinkedList<Event> sBackgroundQueue = new LinkedList<Event>();
    private static boolean sJsIsReady = false;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public JsIOHelper(Context context) {
        mContext = context;
    }

    public void sendEventToJS(String name, Bundle bundle) {
        final Event event = new Event(name, bundle);
        postEvent(event);
    }

    public void sendEventToJS(String name, String string) {
        final Event event = new Event(name, string);
        postEvent(event);
    }

    public void consumeBackgroundQueue() {
        synchronized (JsIOHelper.class) {
            sJsIsReady = true;

            while (!sBackgroundQueue.isEmpty()) {
                emitEvent(sBackgroundQueue.pop());
            }
        }
    }

    private void postEvent(final Event event) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            emitEvent(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    emitEvent(event);
                }
            });
        }
    }

    private void emitEvent(Event event) {
        final ReactInstanceManager reactInstanceManager = ((ReactApplication) mContext.getApplicationContext()).getReactNativeHost().getReactInstanceManager();
        final ReactContext reactContext = reactInstanceManager.getCurrentReactContext();

        synchronized (JsIOHelper.class) {
            if (sJsIsReady && reactContext != null && reactContext.hasActiveCatalystInstance()) {
                if (isAppBackgrounded()) {
                    sendBackgroundEvent(event);
                } else {
                    final Object data = event.object instanceof Bundle ? Arguments.fromBundle((Bundle) event.object) : event.object;
                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(event.name, data);
                }
            } else {
                sBackgroundQueue.push(event);

                if (reactContext == null && !reactInstanceManager.hasStartedCreatingInitialContext()) {
                    reactInstanceManager.createReactContextInBackground();
                }
            }
        }
    }

    private void sendBackgroundEvent(Event event) {
        final Intent backgroundEventIntent = new Intent(mContext, BackgroundEventService.class);
        backgroundEventIntent.putExtra(BackgroundEventService.EXTRA_EVENT_NAME, event.name);

        if (event.object instanceof String) {
            backgroundEventIntent.putExtra(BackgroundEventService.EXTRA_EVENT_DATA, (String) event.object);
        } else if (event.object instanceof Bundle) {
            backgroundEventIntent.putExtra(BackgroundEventService.EXTRA_EVENT_DATA, (Bundle) event.object);
        }

        mContext.startService(backgroundEventIntent);
        HeadlessJsTaskService.acquireWakeLockNow(mContext);
    }

    private boolean isAppBackgrounded() {
        final ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager != null ? activityManager.getRunningAppProcesses() : null;

        if (appProcesses != null) {
            final String packageName = mContext.getPackageName();

            for (final ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static final class Event {
        final String name;
        final Object object;

        Event(String name, Object object) {
            this.name = name;
            this.object = object;
        }
    }
}
