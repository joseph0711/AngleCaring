const { executeQuery } = require("../utils/database");

/**
 * 監控狀態模型
 */
class MonitoringStatus {
  /**
   * 獲取用戶的監控狀態摘要
   * @param {number} userId - 用戶ID
   * @returns {Promise<object>} - 監控狀態摘要
   */
  static async getMonitoringStatusSummary(userId) {
    try {
      // 獲取最新警報
      const latestAlarm = await this.getLatestAlarm(userId);

      // 獲取設備狀態
      const deviceStatuses = await this.getDeviceStatuses();

      // 獲取最新感應器讀數
      const latestSensorReadings = await this.getLatestSensorReadings();

      // 計算整體狀態
      const overallStatus = this.calculateOverallStatus(
        deviceStatuses,
        latestAlarm
      );

      return {
        latestAlarm,
        alarmLevel: this.getAlarmLevel(latestAlarm),
        deviceStatuses,
        latestSensorReadings,
        overallStatus,
        lastUpdated: new Date(),
      };
    } catch (error) {
      console.error("獲取監控狀態摘要錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取用戶的最新警報（最近1小時內）
   * @param {number} userId - 用戶ID
   * @returns {Promise<object|null>} - 最新警報
   */
  static async getLatestAlarm(userId) {
    try {
      const result = await executeQuery(
        `SELECT TOP 1 a.alarm_id, a.user_id, a.alarm_time, a.alarm_label,
         a.risk_level, a.device_id, a.created_at
         FROM dbo.alarm a
         WHERE a.user_id = ?
         AND a.alarm_time >= DATEADD(hour, -1, GETDATE())
         ORDER BY a.alarm_time DESC`,
        [userId]
      );

      return result.recordset[0] || null;
    } catch (error) {
      console.error("獲取最新警報錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取設備狀態列表
   * @returns {Promise<Array>} - 設備狀態列表
   */
  static async getDeviceStatuses() {
    try {
      const result = await executeQuery(
        `SELECT
           d.device_id,
           d.device_type,
           d.location,
           d.status,
           d.last_online_time,
           CASE
             WHEN d.last_online_time IS NULL THEN 0
             WHEN DATEDIFF(minute, d.last_online_time, GETDATE()) > 5 THEN 0
             ELSE 1
           END AS is_online,
           sr.reading_time AS last_reading_time,
           CASE
             WHEN sr.reading_time IS NULL THEN 0
             WHEN DATEDIFF(minute, sr.reading_time, GETDATE()) > 10 THEN 0
             ELSE 1
           END AS has_recent_data
         FROM dbo.devices d
         LEFT JOIN (
           SELECT device_id, reading_time,
             ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY reading_time DESC) as rn
           FROM dbo.sensor_readings
         ) sr ON d.device_id = sr.device_id AND sr.rn = 1
         ORDER BY d.device_id`
      );

      return result.recordset.map((device) => ({
        deviceId: device.device_id,
        deviceType: device.device_type,
        location: device.location,
        status: device.status,
        lastOnlineTime: device.last_online_time,
        isOnline: Boolean(device.is_online),
        lastReadingTime: device.last_reading_time,
        hasRecentData: Boolean(device.has_recent_data),
      }));
    } catch (error) {
      console.error("獲取設備狀態錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取最新感應器讀數
   * @returns {Promise<Array>} - 最新感應器讀數列表
   */
  static async getLatestSensorReadings() {
    try {
      const result = await executeQuery(
        `SELECT sr.sensor_reading_id, sr.device_id, sr.reading_time,
         sr.motion_status, sr.co_ppm, sr.co2_ppm, sr.created_at,
         d.device_type, d.location
         FROM dbo.sensor_readings sr
         INNER JOIN dbo.devices d ON sr.device_id = d.device_id
         INNER JOIN (
           SELECT device_id, MAX(reading_time) as max_time
           FROM dbo.sensor_readings
           GROUP BY device_id
         ) latest ON sr.device_id = latest.device_id AND sr.reading_time = latest.max_time
         ORDER BY sr.reading_time DESC`
      );

      return result.recordset.map((reading) => ({
        sensor_reading_id: reading.sensor_reading_id,
        device_id: reading.device_id,
        reading_time: reading.reading_time,
        motion_status: reading.motion_status,
        co_ppm: reading.co_ppm,
        co2_ppm: reading.co2_ppm,
        created_at: reading.created_at,
      }));
    } catch (error) {
      console.error("獲取最新感應器讀數錯誤:", error);
      throw error;
    }
  }

  /**
   * 計算整體狀態
   * @param {Array} deviceStatuses - 設備狀態列表
   * @param {object|null} latestAlarm - 最新警報
   * @returns {string} - 整體狀態
   */
  static calculateOverallStatus(deviceStatuses, latestAlarm) {
    // 只基於警報資料值來判斷異常，不檢查設備狀態
    if (latestAlarm && this.isRecentAlarm(latestAlarm.alarm_time)) {
      return "異常";
    }

    return "正常";
  }

  /**
   * 獲取警報等級
   * @param {object|null} alarm - 警報物件
   * @returns {string} - 警報等級
   */
  static getAlarmLevel(alarm) {
    if (!alarm) return "NORMAL";

    switch (alarm.risk_level?.toLowerCase()) {
      case "warning":
        return "WARNING";
      case "severe":
        return "SEVERE";
      case "danger":
        return "DANGER";
      default:
        return "NORMAL";
    }
  }

  /**
   * 檢查是否為最近的警報（24小時內）
   * @param {Date} alarmTime - 警報時間
   * @returns {boolean} - 是否為最近警報
   */
  static isRecentAlarm(alarmTime) {
    if (!alarmTime) return false;

    const now = new Date();
    const alarmDate = new Date(alarmTime);
    const diffHours = (now - alarmDate) / (1000 * 60 * 60);

    return diffHours <= 24;
  }
}

module.exports = MonitoringStatus;
