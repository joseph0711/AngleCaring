const jwt = require("jsonwebtoken");
const User = require("../models/user.model");
const config = require("../config/config");

/**
 * 用戶登錄
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;

    // 驗證輸入
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: "電子郵件和密碼是必需的",
      });
    }

    // 查找用戶
    const user = await User.findByEmail(email);

    if (!user) {
      return res.status(401).json({
        success: false,
        message: "無效的憑據",
      });
    }

    // 驗證密碼
    const isPasswordValid = await User.comparePassword(password, user.Password);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: "無效的憑據",
      });
    }

    // 創建JWT令牌
    const token = jwt.sign(
      {
        id: user.user_id,
        user_id: user.user_id,
        sub: user.user_id.toString(),
        email: user.Email,
        userName: user.user_name,
      },
      config.jwt.secret,
      { expiresIn: config.jwt.expiresIn }
    );

    // 格式化用戶並發送響應
    const formattedUser = User.format(user);

    res.status(200).json({
      success: true,
      message: "登錄成功",
      user: formattedUser,
      token,
    });
  } catch (error) {
    console.error("登錄錯誤:", error);
    res.status(500).json({
      success: false,
      message: "登錄時發生錯誤",
    });
  }
};

/**
 * 用戶註冊
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.signup = async (req, res) => {
  try {
    const { userName, email, password } = req.body;

    // 驗證輸入
    if (!userName || !email || !password) {
      return res.status(400).json({
        success: false,
        message: "用戶名、電子郵件和密碼是必需的",
      });
    }

    // 驗證電子郵件格式
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        success: false,
        message: "無效的電子郵件格式",
      });
    }

    // 檢查電子郵件是否已存在
    const existingUser = await User.findByEmail(email);
    if (existingUser) {
      return res.status(409).json({
        success: false,
        message: "電子郵件已存在",
      });
    }

    // 創建新用戶
    const newUser = await User.create({
      userName,
      email,
      password,
    });

    // 創建JWT令牌
    const token = jwt.sign(
      {
        id: newUser.user_id,
        user_id: newUser.user_id,
        sub: newUser.user_id.toString(),
        email: newUser.Email,
        userName: newUser.user_name,
      },
      config.jwt.secret,
      { expiresIn: config.jwt.expiresIn }
    );

    // 格式化用戶並發送響應
    const formattedUser = User.format(newUser);

    res.status(201).json({
      success: true,
      message: "註冊成功",
      user: formattedUser,
      token,
    });
  } catch (error) {
    console.error("註冊錯誤:", error);
    res.status(500).json({
      success: false,
      message: "註冊時發生錯誤",
    });
  }
};
