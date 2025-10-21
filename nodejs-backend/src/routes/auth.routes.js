const express = require("express");
const router = express.Router();
const authController = require("../controllers/auth.controller");
const authMiddleware = require("../middlewares/auth.middleware");

/**
 * @route   POST /api/auth/login
 * @desc    用戶登錄
 * @access  Public
 */
router.post("/login", authController.login);

/**
 * @route   POST /api/auth/signup
 * @desc    用戶註冊
 * @access  Public
 */
router.post("/signup", authController.signup);

/**
 * @route   GET /api/auth/profile
 * @desc    獲取用戶資料
 * @access  Private
 */
router.get("/profile", authMiddleware.verifyToken, (req, res) => {
  res.status(200).json({
    success: true,
    user: req.user,
  });
});

module.exports = router;
