const express = require("express");
const testNotificationController = require("../controllers/testNotification.controller");

const router = express.Router();

/**
 * @route POST /api/test-notifications/warning
 * @desc 測試警告等級通知 (IMPORTANCE_LOW)
 * @access Public
 */
router.post("/warning", testNotificationController.testWarningNotification);

/**
 * @route POST /api/test-notifications/danger
 * @desc 測試危險等級通知 (IMPORTANCE_DEFAULT)
 * @access Public
 */
router.post("/danger", testNotificationController.testDangerNotification);

/**
 * @route POST /api/test-notifications/critical
 * @desc 測試嚴重等級通知 (IMPORTANCE_HIGH)
 * @access Public
 */
router.post("/critical", testNotificationController.testCriticalNotification);

/**
 * @route POST /api/test-notifications/all
 * @desc 測試所有等級的通知
 * @access Public
 */
router.post("/all", testNotificationController.testAllNotifications);

module.exports = router;
