package com.wix.reactnativenotifications.core;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wix.reactnativenotifications.core.notifications.ILocalNotification;
import com.wix.reactnativenotifications.core.notifications.LocalNotification;
import com.wix.reactnativenotifications.core.notifications.NotificationProps;
import com.wix.reactnativenotifications.core.notifications.actions.Action;

public class LocalNotificationService extends IntentService {

    public static final String EXTRA_NOTIFICATION = "com.wix.reactnativenotifications.core.NOTIFICATION";

    public static final String EXTRA_NOTIFICATION_ID = "com.wix.reactnativenotifications.core.NOTIFICATION_ID";
    public static final String EXTRA_ACTION = "com.wix.reactnativenotifications.core.ACTION";

    private static final String TAG = LocalNotificationService.class.getSimpleName();

    public LocalNotificationService() {
        super(LocalNotificationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "New intent: " + intent);

        final Bundle notificationBundle = intent != null ? intent.getBundleExtra(EXTRA_NOTIFICATION) : null;
        final Bundle actionBundle = intent != null ? intent.getBundleExtra(EXTRA_ACTION) : null;

        if (actionBundle != null) {
            final Action action = new Action(actionBundle);
            action.onFired(this, intent);
        } else if (notificationBundle != null) {
            final NotificationProps notificationProps = NotificationProps.fromBundle(this, notificationBundle);
            final ILocalNotification localNotification = LocalNotification.get(this, notificationProps);
            localNotification.onOpened();
        }
    }
}
