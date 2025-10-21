const User = require("../models/user.model");
const { executeQuery } = require("../utils/database");

/**
 * 獲取所有用戶
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getAllUsers = async (req, res) => {
  try {
    // 排除當前登錄的管理員，避免管理員看到自己的資訊
    const result = await executeQuery(
      "SELECT * FROM users WHERE user_id != ? ORDER BY account_created_time DESC",
      [req.userId]
    );

    // 格式化所有用戶數據
    const users = result.recordset.map((user) => User.format(user));

    res.status(200).json({
      success: true,
      data: users,
    });
  } catch (error) {
    console.error("獲取用戶列表時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取用戶列表時發生錯誤",
    });
  }
};

/**
 * 根據ID獲取用戶
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getUserById = async (req, res) => {
  try {
    const userId = req.params.id;

    if (!userId || isNaN(userId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID",
      });
    }

    const user = await User.findById(userId);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    res.status(200).json({
      success: true,
      data: User.format(user),
    });
  } catch (error) {
    console.error("獲取用戶時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取用戶時發生錯誤",
    });
  }
};

/**
 * 刪除用戶
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.deleteUser = async (req, res) => {
  try {
    const userId = req.params.id;

    if (!userId || isNaN(userId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID",
      });
    }

    // 檢查用戶是否存在
    const user = await User.findById(userId);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 防止刪除自己
    if (req.user.id === parseInt(userId)) {
      return res.status(400).json({
        success: false,
        message: "無法刪除當前登錄的用戶",
      });
    }

    // 刪除用戶相關的資料（處理外鍵約束）
    try {
      // 1. 刪除用戶的鬧鐘設定
      await executeQuery("DELETE FROM alarm WHERE user_id = ?", [userId]);

      // 2. 刪除用戶的睡眠時間設定
      await executeQuery("DELETE FROM bed_time_settings WHERE user_id = ?", [
        userId,
      ]);

      // 3. 刪除用戶的家庭群組成員關係
      await executeQuery("DELETE FROM family_group_members WHERE user_id = ?", [
        userId,
      ]);

      // 4. 處理用戶創建的家庭群組（將創建者設為NULL或刪除群組）
      // 先檢查是否有用戶創建的群組
      const userGroups = await executeQuery(
        "SELECT group_id FROM family_groups WHERE created_by = ?",
        [userId]
      );

      if (userGroups.recordset.length > 0) {
        // 如果群組中還有其他成員，將創建者設為第一個成員
        for (const group of userGroups.recordset) {
          const groupMembers = await executeQuery(
            "SELECT user_id FROM family_group_members WHERE group_id = ? AND user_id != ? ORDER BY joined_time ASC",
            [group.group_id, userId]
          );

          if (groupMembers.recordset.length > 0) {
            // 將群組創建者設為第一個成員
            await executeQuery(
              "UPDATE family_groups SET created_by = ? WHERE group_id = ?",
              [groupMembers.recordset[0].user_id, group.group_id]
            );
          } else {
            // 如果沒有其他成員，刪除群組
            await executeQuery("DELETE FROM family_groups WHERE group_id = ?", [
              group.group_id,
            ]);
          }
        }
      }

      // 5. 刪除用戶的操作日誌（可選，根據業務需求決定是否保留）
      // await executeQuery("DELETE FROM operation_logs WHERE user_id = ?", [userId]);

      // 6. 最後刪除用戶
      await executeQuery("DELETE FROM users WHERE user_id = ?", [userId]);
    } catch (deleteError) {
      console.error("刪除用戶相關資料時發生錯誤:", deleteError);
      throw new Error("刪除用戶時發生錯誤，可能因為存在相關資料");
    }

    res.status(200).json({
      success: true,
      message: "用戶已成功刪除",
    });
  } catch (error) {
    console.error("刪除用戶時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "刪除用戶時發生錯誤",
    });
  }
};

/**
 * 更新用戶管理員狀態
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateUserAdmin = async (req, res) => {
  try {
    const userId = req.params.id;
    const { isAdmin } = req.body;

    if (!userId || isNaN(userId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID",
      });
    }

    if (typeof isAdmin !== "boolean") {
      return res.status(400).json({
        success: false,
        message: "isAdmin 必須為boolean value",
      });
    }

    // 檢查用戶是否存在
    const user = await User.findById(userId);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 更新用戶管理員狀態
    await executeQuery("UPDATE users SET is_admin = ? WHERE user_id = ?", [
      isAdmin,
      userId,
    ]);

    res.status(200).json({
      success: true,
      message: "用戶管理員狀態已更新",
    });
  } catch (error) {
    console.error("更新用戶管理員狀態時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "更新用戶管理員狀態時發生錯誤",
    });
  }
};

/**
 * 更新用戶個人資料
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateProfile = async (req, res) => {
  try {
    const userId = req.user.id; // 從驗證middleware獲取用戶ID
    const { user_name, Email, user_image } = req.body;

    // 基本驗證
    if (!user_name || !Email) {
      return res.status(400).json({
        success: false,
        message: "用戶名和郵箱不能為空",
      });
    }

    // 驗證郵箱格式
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(Email)) {
      return res.status(400).json({
        success: false,
        message: "請輸入有效的郵箱地址",
      });
    }

    // 準備用戶圖片數據
    let userImageBuffer = null;
    if (user_image) {
      try {
        // 如果是base64字符串，轉換為Buffer
        if (typeof user_image === "string") {
          userImageBuffer = Buffer.from(user_image, "base64");
        } else if (Array.isArray(user_image)) {
          // 如果是字節數組，轉換為Buffer
          userImageBuffer = Buffer.from(user_image);
        }
      } catch (error) {
        console.error("圖片處理錯誤:", error);
        return res.status(400).json({
          success: false,
          message: "圖片格式錯誤",
        });
      }
    }

    const updatedUser = await User.updateProfile(userId, {
      userName: user_name,
      email: Email,
      userImage: userImageBuffer,
    });

    res.status(200).json({
      success: true,
      message: "個人資料更新成功",
      user: User.format(updatedUser),
    });
  } catch (error) {
    console.error("更新個人資料時發生錯誤:", error);

    if (error.message === "郵箱已被其他用戶使用") {
      return res.status(409).json({
        success: false,
        message: error.message,
      });
    }

    res.status(500).json({
      success: false,
      message: "更新個人資料時發生錯誤",
    });
  }
};

/**
 * 更新用戶密碼
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updatePassword = async (req, res) => {
  try {
    const userId = req.user.id; // 從驗證middleware獲取用戶ID
    const { current_password, new_password } = req.body;

    // 基本驗證
    if (!current_password || !new_password) {
      return res.status(400).json({
        success: false,
        message: "請輸入當前密碼和新密碼",
      });
    }

    // 驗證新密碼長度
    if (new_password.length < 6) {
      return res.status(400).json({
        success: false,
        message: "新密碼至少需要6個字符",
      });
    }

    await User.updatePassword(userId, current_password, new_password);

    res.status(200).json({
      success: true,
      message: "密碼更新成功",
    });
  } catch (error) {
    console.error("更新密碼時發生錯誤:", error);

    if (error.message === "當前密碼錯誤") {
      return res.status(400).json({
        success: false,
        message: error.message,
      });
    }

    if (error.message === "新密碼不能與當前密碼相同") {
      return res.status(400).json({
        success: false,
        message: error.message,
      });
    }

    res.status(500).json({
      success: false,
      message: "更新密碼時發生錯誤",
    });
  }
};

/**
 * 批量刪除用戶
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.bulkDeleteUsers = async (req, res) => {
  try {
    const { userIds } = req.body;

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "請提供要刪除的用戶ID列表",
      });
    }

    // 防止刪除自己
    if (userIds.includes(req.userId)) {
      return res.status(400).json({
        success: false,
        message: "無法刪除當前登錄的用戶",
      });
    }

    const deletedUsers = [];
    const failedDeletions = [];

    for (const userId of userIds) {
      try {
        // 檢查用戶是否存在
        const user = await User.findById(userId);
        if (!user) {
          failedDeletions.push({ userId, reason: "用戶不存在" });
          continue;
        }

        // 刪除用戶相關的資料（處理外鍵約束）
        try {
          // 1. 刪除用戶的鬧鐘設定
          await executeQuery("DELETE FROM alarm WHERE user_id = ?", [userId]);

          // 2. 刪除用戶的睡眠時間設定
          await executeQuery(
            "DELETE FROM bed_time_settings WHERE user_id = ?",
            [userId]
          );

          // 3. 刪除用戶的家庭群組成員關係
          await executeQuery(
            "DELETE FROM family_group_members WHERE user_id = ?",
            [userId]
          );

          // 4. 處理用戶創建的家庭群組（將創建者設為NULL或刪除群組）
          const userGroups = await executeQuery(
            "SELECT group_id FROM family_groups WHERE created_by = ?",
            [userId]
          );

          if (userGroups.recordset.length > 0) {
            for (const group of userGroups.recordset) {
              const groupMembers = await executeQuery(
                "SELECT user_id FROM family_group_members WHERE group_id = ? AND user_id != ? ORDER BY joined_time ASC",
                [group.group_id, userId]
              );

              if (groupMembers.recordset.length > 0) {
                await executeQuery(
                  "UPDATE family_groups SET created_by = ? WHERE group_id = ?",
                  [groupMembers.recordset[0].user_id, group.group_id]
                );
              } else {
                await executeQuery(
                  "DELETE FROM family_groups WHERE group_id = ?",
                  [group.group_id]
                );
              }
            }
          }

          // 5. 最後刪除用戶
          await executeQuery("DELETE FROM users WHERE user_id = ?", [userId]);
        } catch (deleteError) {
          console.error(`刪除用戶 ${userId} 相關資料時發生錯誤:`, deleteError);
          throw new Error("刪除用戶時發生錯誤，可能因為存在相關資料");
        }
        deletedUsers.push({
          userId,
          userName: user.user_name,
          email: user.Email,
        });
      } catch (error) {
        console.error(`刪除用戶 ${userId} 時發生錯誤:`, error);
        failedDeletions.push({ userId, reason: error.message });
      }
    }

    // 記錄批量操作日誌
    await logger.operation(
      req.userId,
      "BULK_DELETE_USERS",
      {
        totalRequested: userIds.length,
        successfullyDeleted: deletedUsers.length,
        failedDeletions: failedDeletions.length,
        deletedUsers: deletedUsers,
        failedUsers: failedDeletions,
      },
      req.ip,
      req.user.isAdmin
    );

    res.status(200).json({
      success: true,
      message: `成功刪除 ${deletedUsers.length} 位用戶`,
      data: {
        deletedUsers,
        failedDeletions,
      },
    });
  } catch (error) {
    console.error("批量刪除用戶時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "批量刪除用戶時發生錯誤",
    });
  }
};

/**
 * 批量更新用戶管理員狀態
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.bulkUpdateUserAdmin = async (req, res) => {
  try {
    const { userIds, isAdmin } = req.body;

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: "請提供要更新的用戶ID列表",
      });
    }

    if (typeof isAdmin !== "boolean") {
      return res.status(400).json({
        success: false,
        message: "isAdmin 必須為boolean value",
      });
    }

    // 防止修改自己的管理員狀態
    if (userIds.includes(req.userId)) {
      return res.status(400).json({
        success: false,
        message: "無法修改當前登錄用戶的管理員狀態",
      });
    }

    const updatedUsers = [];
    const failedUpdates = [];

    for (const userId of userIds) {
      try {
        // 檢查用戶是否存在
        const user = await User.findById(userId);
        if (!user) {
          failedUpdates.push({ userId, reason: "用戶不存在" });
          continue;
        }

        // 更新用戶管理員狀態
        await executeQuery("UPDATE users SET is_admin = ? WHERE user_id = ?", [
          isAdmin,
          userId,
        ]);

        updatedUsers.push({
          userId,
          userName: user.user_name,
          email: user.Email,
          previousAdminStatus: user.is_admin,
          newAdminStatus: isAdmin,
        });
      } catch (error) {
        console.error(`更新用戶 ${userId} 管理員狀態時發生錯誤:`, error);
        failedUpdates.push({ userId, reason: error.message });
      }
    }

    // 記錄批量操作日誌
    await logger.operation(
      req.userId,
      "BULK_UPDATE_USER_ADMIN",
      {
        totalRequested: userIds.length,
        successfullyUpdated: updatedUsers.length,
        failedUpdates: failedUpdates.length,
        newAdminStatus: isAdmin,
        updatedUsers: updatedUsers,
        failedUsers: failedUpdates,
      },
      req.ip,
      req.user.isAdmin
    );

    res.status(200).json({
      success: true,
      message: `成功更新 ${updatedUsers.length} 位用戶的管理員狀態`,
      data: {
        updatedUsers,
        failedUpdates,
      },
    });
  } catch (error) {
    console.error("批量更新用戶管理員狀態時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "批量更新用戶管理員狀態時發生錯誤",
    });
  }
};

/**
 * 管理員更新其他用戶資料
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateUserProfile = async (req, res) => {
  try {
    const userId = req.params.id;
    const { user_name, Email, user_image } = req.body;

    if (!userId || isNaN(userId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID",
      });
    }

    // 基本驗證 - 至少需要提供一個要更新的欄位
    if (
      (!user_name || user_name === null) &&
      (!Email || Email === null) &&
      (!user_image || user_image === null)
    ) {
      return res.status(400).json({
        success: false,
        message: "至少需要提供一個要更新的欄位",
      });
    }

    // 驗證郵箱格式（如果提供了郵箱）
    if (Email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(Email)) {
        return res.status(400).json({
          success: false,
          message: "請輸入有效的郵箱地址",
        });
      }
    }

    // 檢查用戶是否存在
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 準備用戶圖片數據
    let userImageBuffer = null;
    if (user_image) {
      try {
        // 如果是base64字符串，轉換為Buffer
        if (typeof user_image === "string") {
          userImageBuffer = Buffer.from(user_image, "base64");
        } else if (Array.isArray(user_image)) {
          // 如果是字節數組，轉換為Buffer
          userImageBuffer = Buffer.from(user_image);
        }
      } catch (error) {
        console.error("圖片處理錯誤:", error);
        return res.status(400).json({
          success: false,
          message: "圖片格式錯誤",
        });
      }
    }

    const updatedUser = await User.updateProfile(userId, {
      userName: user_name && user_name !== null ? user_name : undefined,
      email: Email && Email !== null ? Email : undefined,
      userImage: userImageBuffer,
    });

    res.status(200).json({
      success: true,
      message: "用戶資料更新成功",
      data: User.format(updatedUser),
    });
  } catch (error) {
    console.error("更新用戶資料時發生錯誤:", error);

    if (error.message === "郵箱已被其他用戶使用") {
      return res.status(409).json({
        success: false,
        message: error.message,
      });
    }

    res.status(500).json({
      success: false,
      message: "更新用戶資料時發生錯誤",
    });
  }
};

/**
 * 獲取用戶統計數據
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getUserStats = async (req, res) => {
  try {
    const userId = req.params.id;

    if (!userId || isNaN(userId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID",
      });
    }

    // 檢查用戶是否存在
    const user = await User.findById(userId);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 獲取登錄次數（從操作日誌中統計登錄操作）
    const loginCountResult = await executeQuery(
      "SELECT COUNT(*) as loginCount FROM operation_logs WHERE user_id = ? AND action = 'LOGIN'",
      [userId]
    );
    const loginCount = loginCountResult.recordset[0]?.loginCount || 0;

    // 獲取操作次數（從操作日誌中統計所有操作）
    const operationCountResult = await executeQuery(
      "SELECT COUNT(*) as operationCount FROM operation_logs WHERE user_id = ?",
      [userId]
    );
    const operationCount =
      operationCountResult.recordset[0]?.operationCount || 0;

    // 獲取在線時長（從操作日誌中計算最後登錄到現在的時間差）
    const lastLoginResult = await executeQuery(
      "SELECT TOP 1 created_at FROM operation_logs WHERE user_id = ? AND action = 'LOGIN' ORDER BY created_at DESC",
      [userId]
    );

    let onlineHours = 0;
    if (lastLoginResult.recordset.length > 0) {
      const lastLoginTime = new Date(lastLoginResult.recordset[0].created_at);
      const now = new Date();
      onlineHours =
        Math.round(((now - lastLoginTime) / (1000 * 60 * 60)) * 100) / 100; // 保留兩位小數
    }

    res.status(200).json({
      success: true,
      data: {
        loginCount: loginCount,
        operationCount: operationCount,
        onlineHours: onlineHours,
      },
    });
  } catch (error) {
    console.error("獲取用戶統計數據時發生錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取用戶統計數據時發生錯誤",
    });
  }
};
