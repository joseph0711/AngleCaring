const { executeQuery } = require("../utils/database");

class Alarm {
  /**
   * Fetch all alarms
   * @returns {Promise<Array>} - Alarm list
   */
  static async getAllAlarms() {
    try {
      const result = await executeQuery(
        `SELECT a.alarm_id, a.user_id, a.alarm_time, a.alarm_label, a.created_at,
        a.risk_level, a.device_id, d.location AS device_location, d.device_type AS device_name,
        CASE
          WHEN d.device_type = 'co' THEN sr.co_ppm
          WHEN d.device_type = 'co2' THEN sr.co2_ppm
          ELSE NULL
        END AS value,
        sr.motion_status,
        CASE
          WHEN d.device_type = 'co' THEN 9.4
          WHEN d.device_type = 'co2' THEN 1000
          ELSE NULL
        END AS standard_value
        FROM dbo.alarm a
        LEFT JOIN dbo.devices d ON a.device_id = d.device_id
        OUTER APPLY (
            SELECT TOP 1 co_ppm, co2_ppm, motion_status
            FROM dbo.sensor_readings sr2
            WHERE sr2.device_id = a.device_id
              AND ABS(DATEDIFF(second, a.alarm_time, sr2.reading_time)) <= 300
            ORDER BY ABS(DATEDIFF(second, a.alarm_time, sr2.reading_time))
        ) sr
        ORDER BY a.alarm_time DESC`
      );
      return result.recordset;
    } catch (error) {
      console.error("Error fetching alarms:", error);
      throw error;
    }
  }

  /**
   * Fetch specific alarm
   * @param {number} alarmId - Alarm ID
   * @returns {Promise<object>} - Alarm data
   */
  static async getAlarmById(alarmId) {
    try {
      const result = await executeQuery(
        `SELECT TOP 1 a.alarm_id, a.user_id, a.alarm_time, a.alarm_label, a.created_at,
       a.risk_level, a.device_id, d.location AS device_location, d.device_type AS device_name,
       CASE
         WHEN d.device_type = 'co' THEN sr.co_ppm
         WHEN d.device_type = 'co2' THEN sr.co2_ppm
         ELSE NULL
       END AS value,
       sr.motion_status,
       CASE
         WHEN d.device_type = 'co' THEN 9.4
         WHEN d.device_type = 'co2' THEN 1000
         ELSE NULL
       END AS standard_value
      FROM dbo.alarm a
      LEFT JOIN dbo.devices d ON a.device_id = d.device_id
      LEFT JOIN dbo.sensor_readings sr ON a.device_id = sr.device_id
        AND ABS(DATEDIFF(second, a.alarm_time, sr.reading_time)) <= 300
      WHERE a.alarm_id = ?
      ORDER BY ABS(DATEDIFF(second, a.alarm_time, sr.reading_time))`,
        [alarmId]
      );
      return result.recordset[0];
    } catch (error) {
      console.error("Error fetching alarm:", error);
      throw error;
    }
  }

  /**
   * Update alarm status
   * @param {number} alarmId - Alarm ID
   * @param {boolean} isActive - Active status
   * @returns {Promise<boolean>} - Operation result
   */
  static async updateAlarmStatus(alarmId, isActive) {
    try {
      const result = await executeQuery(
        "UPDATE dbo.alarm SET is_active = ?, updated_at = GETDATE() WHERE alarm_id = ?",
        [isActive ? 1 : 0, alarmId]
      );
      return result.rowsAffected[0] > 0;
    } catch (error) {
      console.error("Error updating alarm status:", error);
      throw error;
    }
  }

  /**
   * Fetch all alarms by user ID
   * @param {number} userId - User ID
   * @returns {Promise<Array>} - Alarm list
   */
  static async getAlarmsByUserId(userId) {
    try {
      const result = await executeQuery(
        `SELECT a.alarm_id, a.user_id, a.alarm_time, a.alarm_label, a.created_at,
        a.risk_level, a.device_id, d.location AS device_location, d.type AS device_name,
        CASE
          WHEN d.type = 'co' THEN sr.co_ppm
          WHEN d.type = 'co2' THEN sr.co2_ppm
          ELSE NULL
        END AS value,
        sr.motion_status,
        CASE
          WHEN d.type = 'co' THEN 9.4
          WHEN d.type = 'co2' THEN 1000
          ELSE NULL
        END AS standard_value
        FROM dbo.alarm a
        LEFT JOIN dbo.devices d ON a.device_id = d.id
        LEFT JOIN dbo.sensor_readings sr ON a.device_id = sr.device_id
          AND ABS(DATEDIFF(second, a.alarm_time, sr.reading_time)) <= 300
        WHERE a.user_id = ?
        ORDER BY a.alarm_time DESC`,
        [userId]
      );
      return result.recordset;
    } catch (error) {
      console.error("Error fetching alarms for user:", error);
      throw error;
    }
  }

  /**
   * Fetch sensor readings for alarm detail page
   * @param {string} deviceId - Device ID
   * @param {string} alarmTime - Alarm time
   * @param {number} readingsCount - Number of readings before and including alarm time (default: 10)
   * @returns {Promise<Array>} - Sensor readings list (including alarm time point)
   */
  static async getSensorReadingsForAlarm(
    deviceId,
    alarmTime,
    readingsCount = 5
  ) {
    try {
      // 獲取警報時間前4筆 + 最接近警報時間的1筆（確保包含警報時間點）
      const result = await executeQuery(
        `WITH BeforeAlarmReadings AS (
          -- 獲取警報時間點前5筆讀數
          SELECT TOP ${
            readingsCount - 1
          } sensor_reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at
          FROM dbo.sensor_readings
          WHERE device_id = ?
            AND reading_time < ?
          ORDER BY reading_time DESC
        ),
        AlarmTimeReading AS (
          -- 獲取最接近警報時間的讀數（在警報時間前後5分鐘內，確保包含警報時間點）
          SELECT TOP 1 sensor_reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at
          FROM dbo.sensor_readings
          WHERE device_id = ?
            AND ABS(DATEDIFF(second, reading_time, ?)) <= 300  -- 警報時間前後5分鐘內
          ORDER BY ABS(DATEDIFF(second, reading_time, ?))
        )
        SELECT sensor_reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at
        FROM BeforeAlarmReadings
        UNION ALL
        SELECT sensor_reading_id, device_id, reading_time, motion_status, co_ppm, co2_ppm, created_at
        FROM AlarmTimeReading
        ORDER BY reading_time ASC`,
        [deviceId, alarmTime, deviceId, alarmTime, alarmTime]
      );
      return result.recordset;
    } catch (error) {
      console.error("Error fetching sensor readings for alarm:", error);
      throw error;
    }
  }
}

module.exports = Alarm;
