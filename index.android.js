import {AppRegistry, NativeModules, DeviceEventEmitter} from "react-native";
import NotificationAndroid from "./notification";

const RNNotifications = NativeModules.WixRNNotifications;

let notificationReceivedListener;
let notificationOpenedListener;
let registrationTokenUpdateListener;

let notificationReceivedEventListener;
let notificationOpenedEventListener;
let registrationTokenUpdateEventListener;

export const EVENT_OPENED = "com.wix.reactnativenotifications.notificationOpened";
export const EVENT_RECEIVED = "com.wix.reactnativenotifications.notificationReceived";
export const EVENT_REGISTERED = "com.wix.reactnativenotifications.remoteNotificationsRegistered";

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
    notificationOpenedListener = listener;
    notificationOpenedEventListener = DeviceEventEmitter.addListener(EVENT_OPENED, (notification) => listener(new NotificationAndroid(notification)));
  }

  static clearNotificationOpenedListener() {
    if (notificationOpenedEventListener) {
      notificationOpenedEventListener.remove();
      notificationOpenedEventListener = null;
      notificationOpenedListener = null;
    }
  }

  static setNotificationReceivedListener(listener) {
    NotificationsAndroid.clearNotificationReceivedListener();
    notificationReceivedListener = listener;
    notificationReceivedEventListener = DeviceEventEmitter.addListener(EVENT_RECEIVED, (notification) => listener(new NotificationAndroid(notification)));
  }

  static clearNotificationReceivedListener() {
    if (notificationReceivedEventListener) {
      notificationReceivedEventListener.remove();
      notificationReceivedEventListener = null;
      notificationReceivedListener = null;
    }
  }

  static setRegistrationTokenUpdateListener(listener) {
    NotificationsAndroid.clearRegistrationTokenUpdateListener();
    registrationTokenUpdateListener = listener;
    registrationTokenUpdateEventListener = DeviceEventEmitter.addListener(EVENT_REGISTERED, listener);
  }

  static clearRegistrationTokenUpdateListener() {
    if (registrationTokenUpdateEventListener) {
      registrationTokenUpdateEventListener.remove();
      registrationTokenUpdateEventListener = null;
      registrationTokenUpdateListener = null;
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

AppRegistry.registerHeadlessTask('com.wix.reactnativenotifications.core.background.event', () => (async (event) => {
  switch (event.name) {
    case EVENT_OPENED:
      if (notificationOpenedListener) {
        notificationOpenedListener(new NotificationAndroid(event.data));
      }
      break;
    case EVENT_RECEIVED:
      if (notificationReceivedListener) {
        notificationReceivedListener(new NotificationAndroid(event.data));
      }
      break;
    case EVENT_REGISTERED:
      if (registrationTokenUpdateListener) {
        registrationTokenUpdateListener(event.data);
      }
      break;
  }
}));
