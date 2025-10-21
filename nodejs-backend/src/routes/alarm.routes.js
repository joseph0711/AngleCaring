const express = require("express");
const router = express.Router();
const alarmController = require("../controllers/alarm.controller");
const authMiddleware = require("../middlewares/auth.middleware");

/**
 * @route   GET /api/alarms
 * @desc    獲取所有警報數據
 * @access  Public (測試用)
 */
router.get("/", alarmController.getAllAlarms);

/**
 * @route   GET /api/alarms/:alarmId
 * @desc    獲取特定警報數據
 * @access  Public (測試用)
 */
router.get("/:alarmId", alarmController.getAlarmById);

/**
 * @route   PUT /api/alarms/:alarmId/status
 * @desc    更新警報的啟用狀態
 * @access  Public (測試用)
 */
router.put("/:alarmId/status", alarmController.updateAlarmStatus);

/**
 * @route   GET /api/alarms/:alarmId/sensor-readings
 * @desc    獲取警報相關的感測器讀數 (警報時間往前指定筆數，包含警報時間點)
 * @query   readingsCount - 警報時間往前的讀數筆數，包含警報時間點 (默認: 10)
 * @access  Public (測試用)
 */
router.get("/:alarmId/sensor-readings", alarmController.getAlarmSensorReadings);

/**
 * @route   GET /api/alarms/user/:userId
 * @desc    獲取特定用戶的所有警報
 * @access  Public (測試用)
 */
router.get("/user/:userId", alarmController.getAlarmsByUserId);

module.exports = router;
