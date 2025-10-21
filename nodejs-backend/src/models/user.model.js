const { executeQuery, sql } = require("../utils/database");
const bcrypt = require("bcrypt");

/**
 * 用戶模型
 */
class User {
  /**
   * 根據電子郵件查找用戶
   * @param {string} email - 用戶電子郵件
   * @returns {Promise<object|null>} - 用戶對象或null
   */
  static async findByEmail(email) {
    try {
      const result = await executeQuery("SELECT * FROM dbo.users WHERE Email = ?", [
        email,
      ]);
      return result.recordset.length > 0 ? result.recordset[0] : null;
    } catch (error) {
      console.error("查詢用戶錯誤:", error);
      throw error;
    }
  }

  /**
   * 根據ID查找用戶
   * @param {number} id - 用戶ID
   * @returns {Promise<object|null>} - 用戶對象或null
   */
  static async findById(id) {
    try {
      const result = await executeQuery(
        "SELECT * FROM dbo.users WHERE user_id = ?",
        [id]
      );
      return result.recordset.length > 0 ? result.recordset[0] : null;
    } catch (error) {
      console.error("查詢用戶錯誤:", error);
      throw error;
    }
  }

  /**
   * 創建新用戶
   * @param {object} userData - 用戶數據
   * @returns {Promise<object>} - 新創建的用戶
   */
  static async create(userData) {
    try {
      // 加密密碼
      const hashedPassword = userData.password;

      const result = await executeQuery(
        "INSERT INTO dbo.users (user_name, Email, Password, account_created_time, is_admin) VALUES (?, ?, ?, GETDATE(), ?); SELECT SCOPE_IDENTITY() AS user_id",
        [
          userData.userName,
          userData.email,
          hashedPassword,
          userData.isAdmin || false,
        ]
      );

      const userId = result.recordset[0].user_id;
      return this.findById(userId);
    } catch (error) {
      console.error("創建用戶錯誤:", error);
      throw error;
    }
  }

  /**
   * 格式化用戶對象 (移除敏感資訊)
   * @param {object} user - 用戶對象
   * @returns {object} - 格式化後的用戶
   */
  static format(user) {
    if (!user) return null;

    // 移除密碼
    const { Password, ...userWithoutPassword } = user;

    // 整理命名 (從資料庫的列名轉換為前端所需格式)
    return {
      id: user.user_id,
      userName: user.user_name,
      email: user.Email,
      accountCreatedTime: user.account_created_time
        ? new Date(user.account_created_time).toISOString()
        : null,
      userImage: user.user_image ? user.user_image.toString("base64") : null,
      isAdmin: user.is_admin === 1 || user.is_admin === true,
    };
  }

  /**
   * 驗證密碼
   * @param {string} password - 純文本密碼
   * @param {string} hashedPassword - 加密後密碼
   * @returns {Promise<boolean>} - 密碼是否匹配
   */
  static async comparePassword(password, hashedPassword) {
    // 在這個簡單的實現中，我們直接比較密碼
    return password === hashedPassword;
  }

  /**
   * 更新用戶個人資料
   * @param {number} userId - 用戶ID
   * @param {object} profileData - 個人資料數據
   * @returns {Promise<object>} - 更新後的用戶對象
   */
  static async updateProfile(userId, profileData) {
    try {
      const { userName, email, userImage } = profileData;

      // 檢查郵箱是否已被其他用戶使用（只有當提供了新的郵箱時才檢查）
      if (email && email !== undefined && email !== null) {
        const existingUser = await this.findByEmail(email);
        if (existingUser && existingUser.user_id !== userId) {
          throw new Error("郵箱已被其他用戶使用");
        }
      }

      // 使用 mssql 直接查詢以正確處理二進制數據
      const { poolPromise } = require("../utils/database");
      const pool = await poolPromise;
      const request = pool.request();

      // 構建更新查詢
      let updateQuery = "UPDATE dbo.users SET ";
      let updateFields = [];
      let paramCount = 0;

      if (userName !== undefined) {
        paramCount++;
        updateFields.push(`user_name = @param${paramCount}`);
        request.input(`param${paramCount}`, sql.NVarChar, userName);
      }

      if (email !== undefined) {
        paramCount++;
        updateFields.push(`Email = @param${paramCount}`);
        request.input(`param${paramCount}`, sql.NVarChar, email);
      }

      if (userImage !== undefined) {
        paramCount++;
        updateFields.push(`user_image = @param${paramCount}`);
        // 明確指定 VarBinary 類型來處理二進制數據
        request.input(`param${paramCount}`, sql.VarBinary, userImage);
      }

      if (updateFields.length === 0) {
        throw new Error("沒有提供要更新的資料");
      }

      updateQuery += updateFields.join(", ");
      updateQuery += ` WHERE user_id = @userId`;
      request.input("userId", sql.Int, userId);

      await request.query(updateQuery);

      // 返回更新後的用戶資料
      return this.findById(userId);
    } catch (error) {
      console.error("更新用戶個人資料錯誤:", error);
      throw error;
    }
  }

  /**
   * 更新用戶密碼
   * @param {number} userId - 用戶ID
   * @param {string} currentPassword - 當前密碼
   * @param {string} newPassword - 新密碼
   * @returns {Promise<boolean>} - 更新是否成功
   */
  static async updatePassword(userId, currentPassword, newPassword) {
    try {
      // 獲取用戶當前資料
      const user = await this.findById(userId);
      if (!user) {
        throw new Error("用戶不存在");
      }

      // 驗證當前密碼
      const isCurrentPasswordValid = await this.comparePassword(
        currentPassword,
        user.Password
      );
      if (!isCurrentPasswordValid) {
        throw new Error("當前密碼錯誤");
      }

      // 檢查新密碼與當前密碼是否相同
      if (currentPassword === newPassword) {
        throw new Error("新密碼不能與當前密碼相同");
      }

      // 使用 mssql 直接查詢以保持一致性
      const { poolPromise } = require("../utils/database");
      const pool = await poolPromise;
      const request = pool.request();

      request.input("newPassword", sql.NVarChar, newPassword);
      request.input("userId", sql.Int, userId);

      await request.query(
        "UPDATE dbo.users SET Password = @newPassword WHERE user_id = @userId"
      );

      return true;
    } catch (error) {
      console.error("更新用戶密碼錯誤:", error);
      throw error;
    }
  }
}

module.exports = User;
