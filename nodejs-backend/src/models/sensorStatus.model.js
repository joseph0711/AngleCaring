const { executeQuery } = require("../utils/database");

/**
 * 感應器狀態模型
 */
class SensorStatus {
  /**
   * 獲取感應器狀態摘要
   * @param {number} userId - 用戶ID
   * @returns {Promise<object>} - 感應器狀態摘要
   */
  static async getSensorStatusSummary(userId) {
    try {
      // 獲取設備狀態
      const deviceStatuses = await this.getDeviceStatuses();

      // 分類設備 - 只分為正常和異常兩類
      const normalDevices = deviceStatuses.filter(
        (device) => !device.isAbnormal
      );
      const abnormalDevices = deviceStatuses.filter(
        (device) => device.isAbnormal
      );

      // 生成錯誤訊息
      const errorMessages = this.generateErrorMessages(abnormalDevices);

      // 計算整體狀態
      const overallStatus = this.calculateOverallStatus(abnormalDevices);

      return {
        deviceStatuses,
        normalDevices,
        abnormalDevices,
        overallStatus,
        errorMessages,
        lastUpdated: new Date(),
      };
    } catch (error) {
      console.error("獲取感應器狀態摘要錯誤:", error);
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

      // 處理每個設備的狀態判斷
      const deviceStatuses = [];

      for (const device of result.recordset) {
        // 檢查最近3筆資料的時間間隔
        const isAbnormal = await this.checkSensorAbnormal(device.device_id);

        deviceStatuses.push({
          deviceId: device.device_id,
          deviceType: device.device_type,
          location: device.location,
          status: device.status,
          lastOnlineTime: device.last_online_time,
          isOnline: Boolean(device.is_online),
          lastReadingTime: device.last_reading_time,
          hasRecentData: Boolean(device.has_recent_data),
          isAbnormal: isAbnormal,
        });
      }

      return deviceStatuses;
    } catch (error) {
      console.error("獲取設備狀態錯誤:", error);
      throw error;
    }
  }

  /**
   * 檢查感應器是否異常（最近3筆資料時間間隔超過30秒）
   * @param {string} deviceId - 設備ID
   * @returns {Promise<boolean>} - 是否異常
   */
  static async checkSensorAbnormal(deviceId) {
    try {
      const result = await executeQuery(
        `SELECT TOP 3 reading_time
         FROM dbo.sensor_readings
         WHERE device_id = ?
         ORDER BY reading_time DESC`,
        [deviceId]
      );

      const readings = result.recordset;

      // 如果資料少於3筆，視為異常
      if (readings.length < 3) {
        return true;
      }

      // 檢查時間間隔
      for (let i = 0; i < readings.length - 1; i++) {
        const currentTime = new Date(readings[i].reading_time);
        const nextTime = new Date(readings[i + 1].reading_time);
        const timeDiff = (currentTime - nextTime) / 1000; // 轉換為秒

        // 如果任何兩個連續讀數間隔超過30秒，視為異常
        if (timeDiff > 30) {
          return true;
        }
      }

      return false;
    } catch (error) {
      console.error(`檢查設備 ${deviceId} 異常狀態錯誤:`, error);
      return true; // 發生錯誤時視為異常
    }
  }

  /**
   * 生成錯誤訊息
   * @param {Array} abnormalDevices - 異常設備列表
   * @returns {Array} - 錯誤訊息列表
   */
  static generateErrorMessages(abnormalDevices) {
    const messages = [];

    // 異常設備錯誤訊息
    if (abnormalDevices.length > 0) {
      const deviceTypes = [
        ...new Set(abnormalDevices.map((d) => d.deviceType)),
      ];
      deviceTypes.forEach((type) => {
        const count = abnormalDevices.filter(
          (d) => d.deviceType === type
        ).length;
        messages.push(
          `${this.getDeviceTypeDisplay(type)}有 ${count} 個設備異常`
        );
      });
    }

    return messages;
  }

  /**
   * 計算整體狀態
   * @param {Array} abnormalDevices - 異常設備列表
   * @returns {string} - 整體狀態
   */
  static calculateOverallStatus(abnormalDevices) {
    if (abnormalDevices.length > 0) {
      return "異常";
    }
    return "正常";
  }

  /**
   * 獲取設備類型顯示名稱
   * @param {string} type - 設備類型
   * @returns {string} - 顯示名稱
   */
  static getDeviceTypeDisplay(type) {
    const typeMap = {
      co2: "二氧化碳感應器",
      co: "一氧化碳感應器",
      ir: "紅外線感應器",
      temperature: "溫度感應器",
    };
    return typeMap[type.toLowerCase()] || type;
  }

  /**
   * 獲取位置顯示名稱
   * @param {string} location - 位置
   * @returns {string} - 顯示名稱
   */
  static getLocationDisplay(location) {
    const locationMap = {
      bedroom: "臥室",
      kitchen: "廚房",
      livingroom: "客廳",
      bathroom: "浴室",
    };
    return locationMap[location.toLowerCase()] || location;
  }
}

module.exports = SensorStatus;
