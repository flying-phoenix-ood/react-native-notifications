package com.wix.reactnativenotifications.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class BackgroundEventService extends HeadlessJsTaskService {

    public static final String EXTRA_EVENT_NAME = "com.wix.reactnativenotifications.core.event.name";
    public static final String EXTRA_EVENT_DATA = "com.wix.reactnativenotifications.core.event.data";

    private static final String TASK_KEY = "com.wix.reactnativenotifications.core.background.event";

    private static final String EVENT_NAME = "name";
    private static final String EVENT_DATA = "data";

    @Override
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(final Intent intent) {
        final Bundle extras = intent.getExtras();

        if (extras != null) {
            final Object data = extras.get(EXTRA_EVENT_DATA);

            final WritableNativeMap writableMap = new WritableNativeMap();
            writableMap.putString(EVENT_NAME, extras.getString(EXTRA_EVENT_NAME));

            if (data instanceof Bundle) {
                writableMap.putMap(EVENT_DATA, Arguments.fromBundle((Bundle) data));
            } else if (data instanceof String) {
                writableMap.putString(EVENT_DATA, (String) data);
            }

            return new HeadlessJsTaskConfig(
                TASK_KEY,
                writableMap,
                15000,
                false
            );
        }

        return null;
    }
}
