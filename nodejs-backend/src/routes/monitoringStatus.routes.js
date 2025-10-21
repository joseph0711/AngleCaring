const express = require("express");
const router = express.Router();
const {
  getMonitoringStatusSummary,
  getLatestAlarm,
  getDeviceStatuses,
  getLatestSensorReadings,
} = require("../controllers/monitoringStatus.controller");

// 獲取監控狀態摘要
router.get("/summary/:userId", getMonitoringStatusSummary);

// 獲取最新警報
router.get("/latest-alarm/:userId", getLatestAlarm);

// 獲取設備狀態列表
router.get("/device-statuses", getDeviceStatuses);

// 獲取最新感應器讀數
router.get("/latest-sensor-readings", getLatestSensorReadings);

module.exports = router;
