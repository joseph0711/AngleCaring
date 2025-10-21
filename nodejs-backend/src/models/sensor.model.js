const { executeQuery } = require("../utils/database");

/**
 * 感測器模型
 */
class Sensor {
  /**
   * 格式化感測器讀數，確保日期格式一致
   * @param {Array} readings - 感測器讀數列表
   * @returns {Array} - 格式化後的感測器讀數列表
   */
  static formatReadings(readings) {
    return readings.map((reading) => {
      // Ensure dates are consistently formatted (as strings)
      const formattedReading = { ...reading };

      if (reading.reading_time) {
        if (reading.reading_time instanceof Date) {
          formattedReading.reading_time = reading.reading_time.toISOString();
        }
      }

      if (reading.created_at) {
        if (reading.created_at instanceof Date) {
          formattedReading.created_at = reading.created_at.toISOString();
        }
      }

      return formattedReading;
    });
  }

  /**
   * 獲取所有感測器讀數
   * @returns {Promise<Array>} - 感測器讀數列表
   */
  static async getAllReadings() {
    try {
      const result = await executeQuery(
        "SELECT sensor_reading_id AS reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at FROM dbo.sensor_readings ORDER BY reading_time DESC"
      );
      return this.formatReadings(result.recordset);
    } catch (error) {
      console.error("獲取感測器讀數錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取特定感測器的讀數
   * @param {string} deviceId - 裝置ID
   * @returns {Promise<Array>} - 感測器讀數列表
   */
  static async getReadingsBySensorId(deviceId) {
    try {
      const result = await executeQuery(
        "SELECT sensor_reading_id AS reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at FROM dbo.sensor_readings WHERE device_id = ? ORDER BY reading_time DESC",
        [deviceId]
      );
      return this.formatReadings(result.recordset);
    } catch (error) {
      console.error("獲取感測器讀數錯誤:", error);
      throw error;
    }
  }

  /**
   * 添加新的感測器讀數
   * @param {object} readingData - 讀數數據
   * @returns {Promise<object>} - 新添加的讀數
   */
  static async addReading(readingData) {
    try {
      const result = await executeQuery(
        "INSERT INTO dbo.sensor_readings (device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at) VALUES (?, ?, ?, ?, ?, GETDATE()); SELECT SCOPE_IDENTITY() AS sensor_reading_id",
        [
          readingData.deviceId,
          readingData.readingTime,
          readingData.motionStatus,
          readingData.coPpm,
          readingData.co2Ppm,
        ]
      );

      const newReadingId = result.recordset[0].sensor_reading_id;

      // 獲取新添加的數據
      const newReadingResult = await executeQuery(
        "SELECT sensor_reading_id AS reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at FROM dbo.sensor_readings WHERE sensor_reading_id = ?",
        [newReadingId]
      );

      return newReadingResult.recordset[0];
    } catch (error) {
      console.error("添加感測器讀數錯誤:", error);
      throw error;
    }
  }
}

module.exports = Sensor;
