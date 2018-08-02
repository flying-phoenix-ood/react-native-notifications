package com.wix.reactnativenotifications.core.notifications.channels;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.util.*;

@RequiresApi(Build.VERSION_CODES.O)
public final class ChannelManager {
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "rnn_default";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_NAME = "Default";

    public static void createChannel(final Context context, final String channelId, final ChannelProps channelProps) {
        final String name = channelProps.getName() != null ? channelProps.getName() : "Unnamed channel";
        final int importance = getImportance(channelProps);

        final NotificationChannel channel = new NotificationChannel(channelId, name, importance);

        if (channelProps.getDescription() != null) {
            channel.setDescription(channelProps.getDescription());
        }

        if (channelProps.getGroup() != null) {
            channel.setGroup(channelProps.getGroup());
        }

        if (channelProps.getLightsColor() != null) {
            channel.setLightColor(channelProps.getLightsColor());
            channel.enableLights(true);
        }

        if (channelProps.getShowBadge() != null) {
            channel.setShowBadge(channelProps.getShowBadge());
        }

        if (channelProps.getSound() != null) {
            channel.setSound(channelProps.getSound(), getAudioAttributes(channelProps));
        }

        if (channelProps.getVibrationPattern() != null) {
            channel.setVibrationPattern(channel.getVibrationPattern());
        }

        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    public static Map<String, ChannelProps> getAllChannelProps(final Context context) {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final List<NotificationChannel> channels = notificationManager != null ? notificationManager.getNotificationChannels() : null;
        final Map<String, ChannelProps> channelProps = new HashMap<String, ChannelProps>();

        if (channels != null) {
            for (final NotificationChannel channel : channels) {
                channelProps.put(channel.getId(), getChannelProps(context, channel));
            }
        }

        return channelProps;
    }

    @Nullable
    public static ChannelProps getChannelProps(final Context context, final String channelId) {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final NotificationChannel channel = notificationManager != null ? notificationManager.getNotificationChannel(channelId) : null;

        if (channel != null) {
            return getChannelProps(context, channel);
        }

        return null;
    }

    public static String getDefaultChannelId(final Context context) {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final List<NotificationChannel> channels = notificationManager != null ? notificationManager.getNotificationChannels() : null;

        if (channels != null && channels.size() > 0) {
            return channels.get(0).getId();
        } else if (notificationManager != null) {
            final NotificationChannel channel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL_ID, DEFAULT_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            return DEFAULT_NOTIFICATION_CHANNEL_ID;
        }

        return null;
    }

    private static int getImportance(final ChannelProps channelProps) {
        final Integer importance = channelProps.getImportance();

        if (importance != null && importance >= NotificationManager.IMPORTANCE_NONE && importance <= NotificationManager.IMPORTANCE_MAX) {
            return importance;
        }

        return NotificationManager.IMPORTANCE_DEFAULT;
    }

    private static AudioAttributes getAudioAttributes(final ChannelProps channelProps) {
        final AudioAttributes.Builder builder = new AudioAttributes.Builder();
        builder.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION);

        final Integer contentType = channelProps.getSoundContentType();
        final Integer usage = channelProps.getSoundUsage();

        if (contentType != null && contentType >= AudioAttributes.CONTENT_TYPE_UNKNOWN && contentType <= AudioAttributes.CONTENT_TYPE_SONIFICATION) {
            builder.setContentType(contentType);
        } else {
            builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
        }

        if (usage != null && usage >= AudioAttributes.USAGE_NOTIFICATION && usage <= AudioAttributes.USAGE_NOTIFICATION_EVENT) {
            builder.setUsage(usage);
        } else {
            builder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
        }

        return builder.build();
    }

    private static ChannelProps getChannelProps(final Context context, final NotificationChannel channel) {
        final Bundle properties = new Bundle();
        properties.putBoolean(ChannelProps.BYPASS_DO_NOT_DISTURB, channel.canBypassDnd());

        if (channel.getDescription() != null) {
            properties.putString(ChannelProps.DESCRIPTION, channel.getDescription());
        }

        if (channel.getGroup() != null) {
            properties.putString(ChannelProps.GROUP, channel.getGroup());
        }

        properties.putInt(ChannelProps.IMPORTANCE, channel.getImportance());

        if (channel.shouldShowLights()) {
            properties.putString(ChannelProps.LIGHTS_COLOR, String.format("#%06X", (0xFFFFFF & channel.getLightColor())));
        }

        properties.putInt(ChannelProps.LOCKSCREEN_VISIBILITY, channel.getLockscreenVisibility());
        properties.putString(ChannelProps.NAME, channel.getName().toString());
        properties.putBoolean(ChannelProps.SHOW_BADGE, channel.canShowBadge());

        final String soundPath = channel.getSound() != null ? channel.getSound().getPath() : null;

        if (soundPath != null) {
            properties.putString(ChannelProps.SOUND, soundPath.startsWith("/raw/") ? soundPath.substring(5) : soundPath);
        }

        if (channel.shouldVibrate()) {
            properties.putLongArray(ChannelProps.VIBRATION_PATTERN, channel.getVibrationPattern());
        }

        return new ChannelProps(context, properties);
    }
}
