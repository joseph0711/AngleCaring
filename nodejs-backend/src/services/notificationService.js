const admin = require("firebase-admin");

class NotificationService {
  constructor() {
    // Initialize Firebase Admin SDK
    try {
      if (!admin.apps.length) {
        const serviceAccount = {
          type: "service_account",
          project_id: process.env.FIREBASE_PROJECT_ID,
          private_key_id: process.env.FIREBASE_PRIVATE_KEY_ID,
          private_key: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
          client_email: process.env.FIREBASE_CLIENT_EMAIL,
          client_id: process.env.FIREBASE_CLIENT_ID,
          auth_uri: process.env.FIREBASE_AUTH_URI,
          token_uri: process.env.FIREBASE_TOKEN_URI,
          auth_provider_x509_cert_url:
            process.env.FIREBASE_AUTH_PROVIDER_X509_CERT_URL,
          client_x509_cert_url: process.env.FIREBASE_CLIENT_X509_CERT_URL,
          universe_domain: process.env.FIREBASE_UNIVERSE_DOMAIN,
        };

        admin.initializeApp({
          credential: admin.credential.cert(serviceAccount),
          projectId: process.env.FIREBASE_PROJECT_ID,
        });
      }
      this.firebase = admin;
    } catch (error) {
      console.error("Failed to initialize Firebase Admin SDK:", error.message);
      this.firebase = null;
    }
  }

  /**
   * 發送推播通知到所有註冊的設備
   * @param {string} title - 通知標題
   * @param {string} message - 通知內容
   * @param {string} severity - 警報等級 (WARNING, DANGER, CRITICAL)
   * @param {Object} customData - 自定義數據
   */
  async sendBroadcastNotification(
    title,
    message,
    severity = "DANGER",
    customData = {}
  ) {
    if (!this.firebase) {
      console.warn(
        "Firebase Admin SDK not initialized. Cannot send broadcast notification."
      );
      return { success: false, error: "Notification service not configured" };
    }

    try {
      // Create FCM message with Android notification channel
      const fcmMessage = {
        notification: {
          title: title,
          body: message,
        },
        android: {
          notification: {
            channel_id: this.getAndroidChannelId(severity),
            priority: this.getAndroidPriority(severity),
            sound: this.getAndroidSound(severity),
            vibrate_timings: this.getAndroidVibrateTimings(severity),
            light_settings: this.getAndroidLightSettings(severity),
            tag: `alarm_${severity.toLowerCase()}`,
          },
        },
        data: {
          severity: severity,
          timestamp: new Date().toISOString(),
          ...customData,
        },
        topic: "all", // Send to all devices subscribed to 'all' topic
      };

      const result = await this.firebase.messaging().send(fcmMessage);
      return { success: true, result: { messageId: result } };
    } catch (error) {
      console.error(`發送 ${severity} 等級通知失敗:`, error);
      return { success: false, error: error.message };
    }
  }

  /**
   * 發送推播通知到特定標籤的設備
   * @param {string} tag - 標籤名稱
   * @param {string} title - 通知標題
   * @param {string} message - 通知內容
   * @param {string} severity - 警報等級 (WARNING, DANGER, CRITICAL)
   * @param {Object} customData - 自定義數據
   */
  async sendTaggedNotification(
    tag,
    title,
    message,
    severity = "DANGER",
    customData = {}
  ) {
    if (!this.firebase) {
      console.warn(
        "Firebase Admin SDK not initialized. Cannot send tagged notification."
      );
      return { success: false, error: "Notification service not configured" };
    }

    try {
      // Create FCM message with condition for specific tag
      const fcmMessage = {
        notification: {
          title: title,
          body: message,
        },
        android: {
          notification: {
            channel_id: this.getAndroidChannelId(severity),
            priority: this.getAndroidPriority(severity),
            sound: this.getAndroidSound(severity),
            vibrate_timings: this.getAndroidVibrateTimings(severity),
            light_settings: this.getAndroidLightSettings(severity),
            tag: `alarm_${severity.toLowerCase()}`,
          },
        },
        data: {
          severity: severity,
          timestamp: new Date().toISOString(),
          tag: tag,
          ...customData,
        },
        condition: `'${tag}' in topics`, // Send to devices subscribed to the specific tag topic
      };

      const result = await this.firebase.messaging().send(fcmMessage);
      return { success: true, result: { messageId: result } };
    } catch (error) {
      console.error(`Error sending tagged notification to ${tag}:`, error);
      return { success: false, error: error.message };
    }
  }

  /**
   * 發送推播通知到特定用戶
   * @param {string} userId - 用戶ID
   * @param {string} title - 通知標題
   * @param {string} message - 通知內容
   * @param {string} severity - 警報等級 (WARNING, DANGER, CRITICAL)
   * @param {Object} customData - 自定義數據
   */
  async sendUserNotification(
    userId,
    title,
    message,
    severity = "DANGER",
    customData = {}
  ) {
    return this.sendTaggedNotification(
      `user_${userId}`,
      title,
      message,
      severity,
      customData
    );
  }

  /**
   * 發送推播通知到管理員
   * @param {string} title - 通知標題
   * @param {string} message - 通知內容
   * @param {string} severity - 警報等級 (WARNING, DANGER, CRITICAL)
   * @param {Object} customData - 自定義數據
   */
  async sendAdminNotification(
    title,
    message,
    severity = "DANGER",
    customData = {}
  ) {
    return this.sendTaggedNotification(
      "admin",
      title,
      message,
      severity,
      customData
    );
  }

  /**
   * 根據警報等級獲取 Android 通知頻道 ID
   * @param {string} severity - 警報等級
   * @returns {string} Android 通知頻道 ID
   */
  getAndroidChannelId(severity) {
    switch (severity.toUpperCase()) {
      case "WARNING":
        return "angle_caring_warning";
      case "DANGER":
        return "angle_caring_danger";
      case "CRITICAL":
        return "angle_caring_critical";
      default:
        return "angle_caring_danger";
    }
  }

  /**
   * 根據警報等級獲取 Android 通知優先級
   * @param {string} severity - 警報等級
   * @returns {string} Android 通知優先級
   */
  getAndroidPriority(severity) {
    switch (severity.toUpperCase()) {
      case "WARNING":
        return "low";
      case "DANGER":
        return "default";
      case "CRITICAL":
        return "high";
      default:
        return "default";
    }
  }

  /**
   * 根據警報等級獲取 Android 通知聲音
   * @param {string} severity - 警報等級
   * @returns {string|null} Android 通知聲音
   */
  getAndroidSound(severity) {
    switch (severity.toUpperCase()) {
      case "WARNING":
        return "default";
      case "DANGER":
        return "default";
      case "CRITICAL":
        return "default";
      default:
        return "default";
    }
  }

  /**
   * 根據警報等級獲取 Android 震動模式
   * @param {string} severity - 警報等級
   * @returns {Array|null} Android 震動時間陣列
   */
  getAndroidVibrateTimings(severity) {
    switch (severity.toUpperCase()) {
      case "WARNING":
        return null; // 警告不震動
      case "DANGER":
        return ["0s", "0.2s", "0.2s", "0.2s"]; // 危險震動模式
      case "CRITICAL":
        return ["0s", "0.5s", "0.2s", "0.5s", "0.2s", "0.5s"]; // 嚴重震動模式
      default:
        return ["0s", "0.2s", "0.2s", "0.2s"];
    }
  }

  /**
   * 根據警報等級獲取 Android 燈光設定
   * @param {string} severity - 警報等級
   * @returns {Object|null} Android 燈光設定
   */
  getAndroidLightSettings(severity) {
    switch (severity.toUpperCase()) {
      case "WARNING":
        return null; // 警告不閃燈
      case "DANGER":
        return {
          color: {
            red: 1.0,
            green: 0.5,
            blue: 0.0,
          },
          light_on_duration: "0.2s",
          light_off_duration: "0.2s",
        };
      case "CRITICAL":
        return {
          color: {
            red: 1.0,
            green: 0.0,
            blue: 0.0,
          },
          light_on_duration: "0.5s",
          light_off_duration: "0.2s",
        };
      default:
        return {
          color: {
            red: 1.0,
            green: 0.5,
            blue: 0.0,
          },
          light_on_duration: "0.2s",
          light_off_duration: "0.2s",
        };
    }
  }
}

module.exports = new NotificationService();
