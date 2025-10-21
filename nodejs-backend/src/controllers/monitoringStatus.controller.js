const MonitoringStatus = require("../models/monitoringStatus.model");

/**
 * 獲取監控狀態摘要
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getMonitoringStatusSummary = async (req, res) => {
  try {
    const { userId } = req.params;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: "用戶ID是必需的",
      });
    }

    const summary = await MonitoringStatus.getMonitoringStatusSummary(
      parseInt(userId)
    );

    res.status(200).json({
      success: true,
      data: summary,
    });
  } catch (error) {
    console.error("獲取監控狀態摘要錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取監控狀態摘要時發生錯誤",
    });
  }
};

/**
 * 獲取最新警報
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getLatestAlarm = async (req, res) => {
  try {
    const { userId } = req.params;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: "用戶ID是必需的",
      });
    }

    const latestAlarm = await MonitoringStatus.getLatestAlarm(parseInt(userId));

    res.status(200).json({
      success: true,
      data: latestAlarm,
    });
  } catch (error) {
    console.error("獲取最新警報錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取最新警報時發生錯誤",
    });
  }
};

/**
 * 獲取設備狀態列表
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getDeviceStatuses = async (req, res) => {
  try {
    const deviceStatuses = await MonitoringStatus.getDeviceStatuses();

    res.status(200).json({
      success: true,
      data: deviceStatuses,
    });
  } catch (error) {
    console.error("獲取設備狀態錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取設備狀態時發生錯誤",
    });
  }
};

/**
 * 獲取最新感應器讀數
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getLatestSensorReadings = async (req, res) => {
  try {
    const latestReadings = await MonitoringStatus.getLatestSensorReadings();

    res.status(200).json({
      success: true,
      data: latestReadings,
    });
  } catch (error) {
    console.error("獲取最新感應器讀數錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取最新感應器讀數時發生錯誤",
    });
  }
};
