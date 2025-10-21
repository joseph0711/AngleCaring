const express = require("express");
const router = express.Router();
const {
  getSensorStatusSummary,
  getDeviceStatuses,
} = require("../controllers/sensorStatus.controller");

// 獲取感應器狀態摘要
router.get("/summary/:userId", getSensorStatusSummary);

// 獲取設備狀態列表
router.get("/device-statuses", getDeviceStatuses);

module.exports = router;
