const express = require("express");
const router = express.Router();
const bedTimeController = require("../controllers/bedTime.controller");
const authMiddleware = require("../middlewares/auth.middleware");

/**
 * @route   GET /api/bed-time-settings/:userId
 * @desc    獲取特定用戶的上下床時間設置
 * @access  Public (測試用，實際應用中應加入認證中間件)
 */
router.get("/:userId", bedTimeController.getBedTimeSettings);

/**
 * @route   PUT /api/bed-time-settings/:userId
 * @desc    更新特定用戶的上下床時間設置
 * @access  Public (測試用，實際應用中應加入認證中間件)
 */
router.put("/:userId", bedTimeController.updateBedTimeSettings);

module.exports = router;
