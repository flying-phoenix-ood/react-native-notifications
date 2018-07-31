import {NativeModules, DeviceEventEmitter} from "react-native";
import NotificationAndroid from "./notification";

const RNNotifications = NativeModules.WixRNNotifications;

let notificationReceivedListener;
let notificationOpenedListener;
let registrationTokenUpdateListener;
let actionFiredListener;

export const EVENT_OPENED = "com.wix.reactnativenotifications.notificationOpened";
export const EVENT_RECEIVED = "com.wix.reactnativenotifications.notificationReceived";
export const EVENT_REGISTERED = "com.wix.reactnativenotifications.remoteNotificationsRegistered";
export const EVENT_ACTION = "com.wix.reactnativenotifications.actionFired";

export const NotificationChannelAndroid = Object.freeze({
  IMPORTANCE: Object.freeze({
    NONE: 0,
    MIN: 1,
    LOW: 2,
    DEFAULT: 3,
    HIGH: 4,
    MAX: 5,
  }),
  VISIBILITY: Object.freeze({
    SECRET: -1,
    PRIVATE: 0,
    PUBLIC: 1,
  })
});

export class NotificationsAndroid {
  static setNotificationOpenedListener(listener) {
    NotificationsAndroid.clearNotificationOpenedListener();
    notificationOpenedListener = DeviceEventEmitter.addListener(EVENT_OPENED, (notification) => listener(new NotificationAndroid(notification)));
  }

  static clearNotificationOpenedListener() {
    if (notificationOpenedListener) {
      notificationOpenedListener.remove();
      notificationOpenedListener = null;
    }
  }

  static setNotificationReceivedListener(listener) {
    NotificationsAndroid.clearNotificationReceivedListener();
    notificationReceivedListener = DeviceEventEmitter.addListener(EVENT_RECEIVED, (notification) => listener(new NotificationAndroid(notification)));
  }

  static clearNotificationReceivedListener() {
    if (notificationReceivedListener) {
      notificationReceivedListener.remove();
      notificationReceivedListener = null;
    }
  }

  static setRegistrationTokenUpdateListener(listener) {
    NotificationsAndroid.clearRegistrationTokenUpdateListener();
    registrationTokenUpdateListener = DeviceEventEmitter.addListener(EVENT_REGISTERED, listener);
  }

  static clearRegistrationTokenUpdateListener() {
    if (registrationTokenUpdateListener) {
      registrationTokenUpdateListener.remove();
      registrationTokenUpdateListener = null;
    }
  }

  static setActionFiredListener(listener) {
    NotificationsAndroid.clearActionFiredListener();
    actionFiredListener = DeviceEventEmitter.addListener(EVENT_ACTION, (event) => listener(event.action, event.input, event.notificationId, new NotificationAndroid(event.notification)));
  }

  static clearActionFiredListener() {
    if (actionFiredListener) {
      actionFiredListener.remove();
      actionFiredListener = null;
    }
  }

  static refreshToken() {
    RNNotifications.refreshToken();
  }

  static invalidateToken() {
    RNNotifications.invalidateToken();
  }

  static createChannel(id, options) {
    options = {...options}

    if (typeof options.importance === 'string') {
	  options.importance = NotificationChannelAndroid.IMPORTANCE[options.importance.toUpperCase()] || NotificationChannelAndroid.IMPORTANCE.DEFAULT;
    }

    if (typeof options.visibility === 'string') {
      options.visibility = NotificationChannelAndroid.VISIBILITY[options.visibility.toUpperCase()] || NotificationChannelAndroid.IMPORTANCE.PRIVATE;
    }

    return RNNotifications.createChannel(id, options);
  }

  static getChannel(id) {
    return RNNotifications.getChannel(id);
  }

  static getChannels() {
    return RNNotifications.getChannels();
  }

  static localNotification(notification, id, channel) {
    const notificationProperties = notification instanceof NotificationAndroid ? notification.properties : notification;

    if (!id && id !== 0) {
      id = notificationProperties.tag ? 0 : Math.random() * 100000000 | 0; // Bitwise-OR forces value onto a 32bit limit
    }

    RNNotifications.postLocalNotification(notificationProperties, id, channel || null);
    return id;
  }

  static cancelLocalNotification(id, tag) {
    RNNotifications.cancelLocalNotification(id, tag);
  }

  static cancelAllLocalNotifications() {
    RNNotifications.cancelAllLocalNotifications();
  }

  static getInitialNotification() {
    return RNNotifications.getInitialNotification()
      .then((rawNotification) => {
        return rawNotification ? new NotificationAndroid(rawNotification) : undefined;
      });
  }

  static consumeBackgroundQueue() {
    RNNotifications.consumeBackgroundQueue();
  }
}
