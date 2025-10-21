const Sensor = require("../models/sensor.model");

/**
 * 獲取所有感測器數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAllSensorData = async (req, res) => {
  try {
    // Fetch real data from the database instead of dummy data
    const sensorData = await Sensor.getAllReadings();

    res.status(200).json({
      success: true,
      data: sensorData,
    });
  } catch (error) {
    console.error("Error in sensor controller:", error);
    res.status(500).json({
      success: false,
      message: "Error fetching sensor data",
    });
  }
};

/**
 * 獲取特定感測器數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getSensorDataById = async (req, res) => {
  try {
    const { sensorId } = req.params;

    if (!sensorId) {
      return res.status(400).json({
        success: false,
        message: "裝置ID是必需的",
      });
    }

    const sensorData = await Sensor.getReadingsBySensorId(sensorId);

    res.status(200).json({
      success: true,
      data: sensorData,
    });
  } catch (error) {
    console.error("獲取感測器數據錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取感測器數據時發生錯誤",
    });
  }
};

/**
 * 添加感測器數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.addSensorData = async (req, res) => {
  try {
    const { deviceId, readingTime, motionStatus, coPpm, co2Ppm } = req.body;

    // 驗證輸入
    if (!deviceId || !readingTime) {
      return res.status(400).json({
        success: false,
        message: "裝置ID和讀取時間是必需的",
      });
    }

    // 添加新的感測器讀數
    const newReading = await Sensor.addReading({
      deviceId,
      readingTime: new Date(readingTime),
      motionStatus,
      coPpm,
      co2Ppm,
    });

    res.status(201).json({
      success: true,
      message: "感測器數據添加成功",
      data: newReading,
    });
  } catch (error) {
    console.error("添加感測器數據錯誤:", error);
    res.status(500).json({
      success: false,
      message: "添加感測器數據時發生錯誤",
    });
  }
};
