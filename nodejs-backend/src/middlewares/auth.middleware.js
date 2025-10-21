const jwt = require("jsonwebtoken");
const config = require("../config/config");
const User = require("../models/user.model");

/**
 * 驗證JWT令牌並檢查用戶權限
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 * @param {function} next - 下一個中間件函數
 */
exports.verifyToken = async (req, res, next) => {
  try {
    // 獲取請求頭中的令牌
    let token = req.headers["authorization"];

    if (!token) {
      return res.status(401).json({
        success: false,
        message: "需要授權令牌",
      });
    }

    // 移除 'Bearer '前綴 (如果存在)
    if (token.startsWith("Bearer ")) {
      token = token.slice(7);
    }

    // 驗證令牌
    const decoded = jwt.verify(token, config.jwt.secret);

    // 查找用戶
    const user = await User.findById(decoded.id);

    if (!user) {
      return res.status(401).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 將用戶添加到請求對象
    req.user = User.format(user);
    req.userId = user.user_id;

    next();
  } catch (error) {
    if (error.name === "TokenExpiredError") {
      return res.status(401).json({
        success: false,
        message: "令牌已過期",
      });
    }

    if (error.name === "JsonWebTokenError") {
      return res.status(401).json({
        success: false,
        message: "無效的令牌",
      });
    }

    res.status(500).json({
      success: false,
      message: "授權時發生錯誤",
    });
  }
};

/**
 * 檢查用戶是否為管理員
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 * @param {function} next - 下一個中間件函數
 */
exports.isAdmin = (req, res, next) => {
  if (!req.user) {
    return res.status(401).json({
      success: false,
      message: "未授權，請先登錄",
    });
  }

  if (!req.user.isAdmin) {
    return res.status(403).json({
      success: false,
      message: "無權訪問，需要管理員權限",
    });
  }

  next();
};
