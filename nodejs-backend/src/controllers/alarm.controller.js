const Alarm = require("../models/alarm.model");

/**
 * 獲取所有警報數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAllAlarms = async (req, res) => {
  try {
    const alarms = await Alarm.getAllAlarms();

    res.status(200).json({
      success: true,
      data: alarms,
    });
  } catch (error) {
    console.error("Error in alarm controller:", error);
    res.status(500).json({
      success: false,
      message: "Error fetching alarm data",
    });
  }
};

/**
 * 獲取特定警報數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAlarmById = async (req, res) => {
  try {
    const { alarmId } = req.params;

    const alarm = await Alarm.getAlarmById(alarmId);

    if (!alarm) {
      return res.status(404).json({
        success: false,
        message: "警報不存在",
      });
    }

    res.status(200).json({
      success: true,
      data: alarm,
    });
  } catch (error) {
    console.error("獲取警報數據錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取警報數據時發生錯誤",
    });
  }
};

/**
 * 更新警報的啟用狀態
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateAlarmStatus = async (req, res) => {
  try {
    const { alarmId } = req.params;
    const { isActive } = req.body;

    if (isActive === undefined) {
      return res.status(400).json({
        success: false,
        message: "缺少isActive參數",
      });
    }

    const success = await Alarm.updateAlarmStatus(alarmId, isActive);

    if (!success) {
      return res.status(404).json({
        success: false,
        message: "警報不存在或更新失敗",
      });
    }

    res.status(200).json({
      success: true,
      message: `警報已${isActive ? "啟用" : "停用"}`,
    });
  } catch (error) {
    console.error("更新警報狀態錯誤:", error);
    res.status(500).json({
      success: false,
      message: "更新警報狀態時發生錯誤",
    });
  }
};

/**
 * 獲取特定用戶的所有警報
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAlarmsByUserId = async (req, res) => {
  try {
    const { userId } = req.params;

    const alarms = await Alarm.getAlarmsByUserId(userId);

    res.status(200).json({
      success: true,
      data: alarms,
    });
  } catch (error) {
    console.error("獲取用戶警報錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取用戶警報時發生錯誤",
    });
  }
};

/**
 * 獲取警報相關的感測器讀數
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAlarmSensorReadings = async (req, res) => {
  try {
    const { alarmId } = req.params;
    const { readingsCount = 10 } = req.query;

    // 首先獲取警報資訊
    const alarm = await Alarm.getAlarmById(alarmId);

    if (!alarm) {
      return res.status(404).json({
        success: false,
        message: "警報不存在",
      });
    }

    // 獲取相關的感測器讀數
    const sensorReadings = await Alarm.getSensorReadingsForAlarm(
      alarm.device_id,
      alarm.alarm_time,
      parseInt(readingsCount)
    );

    res.status(200).json({
      success: true,
      data: {
        alarm: alarm,
        sensorReadings: sensorReadings,
      },
    });
  } catch (error) {
    console.error("獲取警報感測器讀數錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取警報感測器讀數時發生錯誤",
    });
  }
};
