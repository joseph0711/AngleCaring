const { executeQuery } = require("../utils/database");

/**
 * 獲取用戶的上床時間設置
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getBedTimeSettings = async (req, res) => {
  try {
    const { userId } = req.params;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: "用戶ID是必需的",
      });
    }

    // 確保userId是數字
    const numUserId = parseInt(userId, 10);
    if (isNaN(numUserId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID格式",
      });
    }

    // 查詢資料庫獲取床時間設置
    const result = await executeQuery(
      "SELECT * FROM bed_time_settings WHERE user_id = ?",
      [numUserId]
    );

    // 如果找不到設置，自動創建預設設定
    if (!result.recordset || result.recordset.length === 0) {
      try {
        // 創建預設的床時間設定
        await executeQuery(
          "INSERT INTO bed_time_settings (user_id, go_to_bed_time, wake_up_time, is_active, alert_if_late, tolerance_minutes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())",
          [numUserId, "22:00:00", "07:00:00", 1, 0, 15]
        );

        // 獲取剛創建的設定
        const newResult = await executeQuery(
          "SELECT * FROM bed_time_settings WHERE user_id = ?",
          [numUserId]
        );

        if (newResult.recordset && newResult.recordset.length > 0) {
          const settings = newResult.recordset[0];

          const formattedSettings = {
            user_id: settings.user_id,
            go_to_bed_time: "22:00:00",
            wake_up_time: "07:00:00",
            is_active: settings.is_active === 1,
            alert_if_late: settings.alert_if_late === 1,
            tolerance_minutes: settings.tolerance_minutes,
            created_at: settings.created_at,
            updated_at: settings.updated_at,
          };

          return res.status(200).json({
            success: true,
            data: formattedSettings,
          });
        } else {
          // 如果創建失敗，返回預設值
          const defaultSettings = {
            user_id: numUserId,
            go_to_bed_time: "22:00:00",
            wake_up_time: "07:00:00",
            is_active: true,
            alert_if_late: false,
            tolerance_minutes: 15,
            created_at: null,
            updated_at: null,
          };

          return res.status(200).json({
            success: true,
            data: defaultSettings,
          });
        }
      } catch (createError) {
        console.error("創建預設床時間設定錯誤:", createError);
        // 如果創建失敗，返回預設值
        const defaultSettings = {
          user_id: numUserId,
          go_to_bed_time: "22:00:00",
          wake_up_time: "07:00:00",
          is_active: true,
          alert_if_late: false,
          tolerance_minutes: 15,
          created_at: null,
          updated_at: null,
        };

        return res.status(200).json({
          success: true,
          data: defaultSettings,
        });
      }
    }

    // 獲取第一個活躍的設置
    const settings = result.recordset[0];

    // 格式化時間
    const formatTime = (timeValue) => {
      if (!timeValue) return "22:00:00";

      // 如果是字符串，檢查格式
      if (typeof timeValue === "string") {
        // 如果已經是 HH:mm:ss 格式，直接返回
        if (timeValue.match(/^\d{2}:\d{2}:\d{2}$/)) {
          return timeValue;
        }

        // 如果是 ISO 格式 (1970-01-01T21:00:00.000Z)，直接提取時間部分，不進行時區轉換
        if (timeValue.includes("T")) {
          const timePart = timeValue.split("T")[1].split(".")[0];
          return timePart;
        }
      }

      // 如果是 Date 對象，格式化為 HH:mm:ss（避免時區轉換問題）
      if (timeValue instanceof Date) {
        const hours = timeValue.getUTCHours().toString().padStart(2, "0");
        const minutes = timeValue.getUTCMinutes().toString().padStart(2, "0");
        const seconds = timeValue.getUTCSeconds().toString().padStart(2, "0");
        return `${hours}:${minutes}:${seconds}`;
      }

      return "22:00:00";
    };

    const formattedSettings = {
      user_id: settings.user_id,
      go_to_bed_time: formatTime(settings.go_to_bed_time),
      wake_up_time: formatTime(settings.wake_up_time),
      is_active: settings.is_active === 1,
      alert_if_late: settings.alert_if_late === 1,
      tolerance_minutes: settings.tolerance_minutes,
      created_at: settings.created_at,
      updated_at: settings.updated_at,
    };

    res.status(200).json({
      success: true,
      data: formattedSettings,
    });
  } catch (error) {
    console.error("獲取床時間設置錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取床時間設置時發生錯誤",
    });
  }
};

/**
 * 更新用戶的上床時間設置
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateBedTimeSettings = async (req, res) => {
  try {
    const { userId } = req.params;
    const {
      go_to_bed_time,
      wake_up_time,
      is_active,
      alert_if_late,
      tolerance_minutes,
    } = req.body;

    // 使用序列化後的字段名
    const goToBedTime = go_to_bed_time;
    const wakeUpTime = wake_up_time;
    const isActive = is_active !== undefined ? is_active : true;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: "用戶ID是必需的",
      });
    }

    // 確保userId是數字
    const numUserId = parseInt(userId, 10);
    if (isNaN(numUserId)) {
      return res.status(400).json({
        success: false,
        message: "無效的用戶ID格式",
      });
    }

    // 檢查必需的參數
    if (!goToBedTime || !wakeUpTime) {
      return res.status(400).json({
        success: false,
        message: "上床時間和起床時間是必需的",
      });
    }

    // 檢查該用戶是否已有設置
    const checkResult = await executeQuery(
      "SELECT user_id FROM bed_time_settings WHERE user_id = ?",
      [numUserId]
    );

    const alertIfLate = alert_if_late !== undefined ? alert_if_late : false;
    const toleranceMinutes =
      tolerance_minutes !== undefined ? tolerance_minutes : 15;

    if (checkResult.recordset && checkResult.recordset.length > 0) {
      // 更新現有設置
      await executeQuery(
        "UPDATE bed_time_settings SET go_to_bed_time = ?, wake_up_time = ?, is_active = ?, alert_if_late = ?, tolerance_minutes = ?, updated_at = GETDATE() WHERE user_id = ?",
        [
          goToBedTime,
          wakeUpTime,
          isActive ? 1 : 0,
          alertIfLate ? 1 : 0,
          toleranceMinutes,
          numUserId,
        ]
      );
    } else {
      // 創建新設置
      await executeQuery(
        "INSERT INTO bed_time_settings (user_id, go_to_bed_time, wake_up_time, is_active, alert_if_late, tolerance_minutes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())",
        [
          numUserId,
          goToBedTime,
          wakeUpTime,
          isActive ? 1 : 0,
          alertIfLate ? 1 : 0,
          toleranceMinutes,
        ]
      );
    }

    // 獲取更新後的設置
    const result = await executeQuery(
      "SELECT * FROM bed_time_settings WHERE user_id = ?",
      [numUserId]
    );

    if (!result.recordset || result.recordset.length === 0) {
      return res.status(404).json({
        success: false,
        message: "無法獲取更新後的設置",
      });
    }

    const settings = result.recordset[0];

    // 格式化時間
    const formatTime = (timeValue) => {
      if (!timeValue) return "22:00:00";

      // 如果是字符串，檢查格式
      if (typeof timeValue === "string") {
        // 如果已經是 HH:mm:ss 格式，直接返回
        if (timeValue.match(/^\d{2}:\d{2}:\d{2}$/)) {
          return timeValue;
        }

        // 如果是 ISO 格式 (1970-01-01T21:00:00.000Z)，直接提取時間部分，不進行時區轉換
        if (timeValue.includes("T")) {
          const timePart = timeValue.split("T")[1].split(".")[0];
          return timePart;
        }
      }

      // 如果是 Date 對象，格式化為 HH:mm:ss（避免時區轉換問題）
      if (timeValue instanceof Date) {
        const hours = timeValue.getUTCHours().toString().padStart(2, "0");
        const minutes = timeValue.getUTCMinutes().toString().padStart(2, "0");
        const seconds = timeValue.getUTCSeconds().toString().padStart(2, "0");
        return `${hours}:${minutes}:${seconds}`;
      }

      return "22:00:00";
    };

    const formattedSettings = {
      user_id: settings.user_id,
      go_to_bed_time: formatTime(settings.go_to_bed_time),
      wake_up_time: formatTime(settings.wake_up_time),
      is_active: settings.is_active === 1,
      alert_if_late: settings.alert_if_late === 1,
      tolerance_minutes: settings.tolerance_minutes,
      created_at: settings.created_at,
      updated_at: settings.updated_at,
    };

    res.status(200).json({
      success: true,
      data: formattedSettings,
    });
  } catch (error) {
    console.error("更新床時間設置錯誤:", error);
    console.error("錯誤詳情:", error.message);
    console.error("錯誤堆棧:", error.stack);
    res.status(500).json({
      success: false,
      message: "更新床時間設置時發生錯誤: " + error.message,
    });
  }
};
