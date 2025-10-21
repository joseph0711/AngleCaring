const express = require("express");
const router = express.Router();
const sensorController = require("../controllers/sensor.controller");
const authMiddleware = require("../middlewares/auth.middleware");

/**
 * @route   GET /api/sensors
 * @desc    獲取所有感測器數據
 * @access  Public (測試用)
 */
router.get("/", sensorController.getAllSensorData);

/**
 * @route   GET /api/sensors/:sensorId
 * @desc    獲取特定感測器數據
 * @access  Public (測試用)
 */
router.get("/:sensorId", sensorController.getSensorDataById);

/**
 * @route   POST /api/sensors
 * @desc    添加感測器數據
 * @access  Public (測試用)
 */
router.post("/", sensorController.addSensorData);

module.exports = router;
