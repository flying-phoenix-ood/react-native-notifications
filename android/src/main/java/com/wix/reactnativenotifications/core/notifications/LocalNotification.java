package com.wix.reactnativenotifications.core.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import android.support.v4.app.NotificationCompat;

import com.facebook.react.bridge.ReactContext;
import com.wix.reactnativenotifications.core.AppLaunchHelper;
import com.wix.reactnativenotifications.core.AppLifecycleFacade;
import com.wix.reactnativenotifications.core.AppLifecycleFacade.AppVisibilityListener;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.BitmapLoader;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.JsIOHelper;
import com.wix.reactnativenotifications.core.LocalNotificationService;
import com.wix.reactnativenotifications.core.notifications.actions.Action;
import com.wix.reactnativenotifications.core.notifications.channels.ChannelManager;
import com.wix.reactnativenotifications.core.notifications.styles.ILocalNotificationStyle;
import com.wix.reactnativenotifications.core.notifications.styles.LocalNotificationStyleManager;

import java.util.ArrayList;

import static com.wix.reactnativenotifications.Defs.LOGTAG;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_OPENED_EVENT_NAME;

public class LocalNotification implements ILocalNotification {

    private final Context mContext;
    private final NotificationProps mNotificationProps;
    private final AppLifecycleFacade mAppLifecycleFacade;
    private final AppLaunchHelper mAppLaunchHelper;
    private final JsIOHelper mJsIOHelper;
    private final BitmapLoader mBitmapLoader;
    private final AppVisibilityListener mAppVisibilityListener = new AppVisibilityListener() {

        @Override
        public void onAppVisible() {
            mAppLifecycleFacade.removeVisibilityListener(this);
            dispatchImmediately();
        }

        @Override
        public void onAppNotVisible() {
        }
    };

    private Bitmap mLargeIconBitmap;

    public static ILocalNotification get(Context context, NotificationProps localNotificationProps) {
        final AppLifecycleFacade appLifecycleFacade = AppLifecycleFacadeHolder.get();
        final AppLaunchHelper appLaunchHelper = new AppLaunchHelper();
        final Context appContext = context.getApplicationContext();

        if (appContext instanceof INotificationsApplication) {
            return ((INotificationsApplication) appContext).getLocalNotification(context, localNotificationProps, AppLifecycleFacadeHolder.get(), new AppLaunchHelper());
        }

        return new LocalNotification(context, localNotificationProps, appLifecycleFacade, appLaunchHelper);
    }

    protected LocalNotification(Context context, NotificationProps localNotificationProps, AppLifecycleFacade appLifecycleFacade, AppLaunchHelper appLaunchHelper, JsIOHelper jsIOHelper, BitmapLoader bitmapLoader) {
        mContext = context;
        mNotificationProps = localNotificationProps;
        mAppLifecycleFacade = appLifecycleFacade;
        mAppLaunchHelper = appLaunchHelper;
        mJsIOHelper = jsIOHelper;
        mBitmapLoader = bitmapLoader;
    }

    protected LocalNotification(Context context, NotificationProps localNotificationProps, AppLifecycleFacade appLifecycleFacade, AppLaunchHelper appLaunchHelper) {
        this(context, localNotificationProps, appLifecycleFacade, appLaunchHelper, new JsIOHelper(context), new BitmapLoader(context));
    }

    @Override
    public int post(Integer notificationId, String channelId) {
        final int id = notificationId != null ? notificationId : createNotificationId();
        final PendingIntent pendingIntent = createOnOpenedIntent(id);
        applyStylingThenPostNotification(id, getNotificationBuilder(pendingIntent, id, channelId));
        return id;
    }

    @Override
    public void onOpened() {
        digestNotification();
    }

    protected void digestNotification() {
        if (!mAppLifecycleFacade.isReactInitialized()) {
            setAsInitialNotification();
            launchOrResumeApp();
            return;
        }

        final ReactContext reactContext = mAppLifecycleFacade.getRunningReactContext();
        if (reactContext.getCurrentActivity() == null) {
            setAsInitialNotification();
        }

        if (mAppLifecycleFacade.isAppVisible()) {
            dispatchImmediately();
        } else {
            dispatchUponVisibility();
        }
    }

    protected void setAsInitialNotification() {
        InitialNotificationHolder.getInstance().set(mNotificationProps);
    }

    protected void dispatchImmediately() {
        sendOpenedEvent();
    }

    protected void dispatchUponVisibility() {
        mAppLifecycleFacade.addVisibilityListener(getIntermediateAppVisibilityListener());

        // Make the app visible so that we'll dispatch the notification opening when visibility changes to 'true' (see
        // above listener registration).
        launchOrResumeApp();
    }

    protected AppVisibilityListener getIntermediateAppVisibilityListener() {
        return mAppVisibilityListener;
    }

    protected PendingIntent createOnOpenedIntent(int id) {
        final Intent serviceIntent = new Intent(mContext, LocalNotificationService.class);
        serviceIntent.putExtra(LocalNotificationService.EXTRA_NOTIFICATION, mNotificationProps.asBundle());
        return PendingIntent.getService(mContext, id, serviceIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    protected String getChannelId(final String channelId) {
        if (channelId != null) {
            return channelId;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String defaultChannelId = ChannelManager.getDefaultChannelId(mContext);

            if (defaultChannelId != null) {
                Log.i(LOGTAG, "No channel ID provided, defaulting to channel " + defaultChannelId);
            }

            return defaultChannelId;
        } else {
            return null;
        }
    }

    protected NotificationCompat.Builder getNotificationBuilder(PendingIntent intent, int notificationId, String channelId) {
        final Integer icon = mNotificationProps.getIcon();
        final Boolean groupSummary = mNotificationProps.getGroupSummary();

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, getChannelId(channelId))
            .setContentTitle(mNotificationProps.getTitle())
            .setContentText(mNotificationProps.getBody())
            .setGroup(mNotificationProps.getGroup())
            .setGroupSummary(groupSummary != null ? groupSummary : false)
            .setSmallIcon(icon != null ? icon : mContext.getApplicationContext().getApplicationInfo().icon)
            .setSound(mNotificationProps.getSound())
            .setContentIntent(intent)
            .setAutoCancel(true);

        int defaults = NotificationCompat.DEFAULT_VIBRATE;

        if (mNotificationProps.getSound() == null) {
            defaults |= NotificationCompat.DEFAULT_SOUND;
        }

        final Integer color = mNotificationProps.getColor();

        if (color != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(color);
        }

        final Integer lightsColor = mNotificationProps.getLightsColor();
        final Integer lightsOnMs = mNotificationProps.getLightsOnMs();
        final Integer lightsOffMs = mNotificationProps.getLightsOffMs();

        if (lightsColor != null && lightsOnMs != null && lightsOffMs != null) {
            builder.setLights(lightsColor, lightsOnMs, lightsOffMs);
        } else {
            defaults |= NotificationCompat.DEFAULT_LIGHTS;
        }

        builder.setDefaults(defaults);

        final ArrayList<Bundle> actions = mNotificationProps.getActions();

        if (actions != null) {
            for (final Bundle actionProperties : actions) {
                final NotificationCompat.Action action = new Action(actionProperties).build(mContext, notificationId, mNotificationProps.asBundle());

                if (action != null) {
                    builder.addAction(action);
                }
            }
        }

        return builder;
    }

    protected void applyStylingThenPostNotification(final int notificationId, final NotificationCompat.Builder notificationBuilder) {
        final String icon = mNotificationProps.getLargeIcon();

        mBitmapLoader.loadImage(icon, new BitmapLoader.OnBitmapLoadedCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap) {
                if (bitmap != null) {
                    mLargeIconBitmap = bitmap.copy(bitmap.getConfig(), false);
                    notificationBuilder.setLargeIcon(mLargeIconBitmap);
                } else if (icon != null) {
                    Log.e(LOGTAG, icon + " does not correspond to a loadable bitmap");
                }

                LocalNotificationStyleManager.getInstance().applyStyle(mContext, notificationBuilder, mNotificationProps.getStyle(), new ILocalNotificationStyle.OnStylingCompleteCallback() {
                    @Override
                    public void onStylingComplete(Context context, NotificationCompat.Builder builder, Bundle properties, boolean applied) {
                        postNotification(notificationId, notificationBuilder.build());
                    }
                });
            }
        });
    }

    protected void postNotification(int id, Notification notification) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(mNotificationProps.getTag(), id, notification);

        if (mLargeIconBitmap != null) {
            mLargeIconBitmap.recycle();
            mLargeIconBitmap = null;
        }
    }

    protected int createNotificationId() {
        return mNotificationProps.getTag() != null ? 0 : (int) System.nanoTime();
    }

    protected void launchOrResumeApp() {
        final Intent intent = mAppLaunchHelper.getLaunchIntent(mContext);
        mContext.startActivity(intent);
    }

    private void sendOpenedEvent() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_OPENED_EVENT_NAME, mNotificationProps.asBundle());
    }
}
