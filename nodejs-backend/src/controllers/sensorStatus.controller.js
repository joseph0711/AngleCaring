const SensorStatus = require("../models/sensorStatus.model");

/**
 * 獲取感應器狀態摘要
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getSensorStatusSummary = async (req, res) => {
  try {
    const { userId } = req.params;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: "用戶ID是必需的",
      });
    }

    const summary = await SensorStatus.getSensorStatusSummary(parseInt(userId));

    res.status(200).json({
      success: true,
      data: summary,
    });
  } catch (error) {
    console.error("獲取感應器狀態摘要錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取感應器狀態摘要時發生錯誤",
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
    const deviceStatuses = await SensorStatus.getDeviceStatuses();

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
