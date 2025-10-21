const notificationService = require("../services/notificationService");

/**
 * 測試通知控制器
 */
class TestNotificationController {
  /**
   * 測試警告等級通知 (IMPORTANCE_LOW)
   */
  async testWarningNotification(req, res) {
    try {
      const result = await notificationService.sendBroadcastNotification(
        "⚠️ 警告通知",
        "這是一個警告等級的測試通知，優先級較低",
        "WARNING",
        {
          test_type: "warning",
          timestamp: new Date().toISOString(),
        }
      );

      res.json({
        success: true,
        message: "警告通知已發送",
        result: result,
      });
    } catch (error) {
      console.error("測試警告通知失敗:", error);
      res.status(500).json({
        success: false,
        message: "發送警告通知失敗",
        error: error.message,
      });
    }
  }

  /**
   * 測試危險等級通知 (IMPORTANCE_DEFAULT)
   */
  async testDangerNotification(req, res) {
    try {
      const result = await notificationService.sendBroadcastNotification(
        "🚨 危險通知",
        "這是一個危險等級的測試通知，優先級中等",
        "DANGER",
        {
          test_type: "danger",
          timestamp: new Date().toISOString(),
        }
      );

      res.json({
        success: true,
        message: "危險通知已發送",
        result: result,
      });
    } catch (error) {
      console.error("測試危險通知失敗:", error);
      res.status(500).json({
        success: false,
        message: "發送危險通知失敗",
        error: error.message,
      });
    }
  }

  /**
   * 測試嚴重等級通知 (IMPORTANCE_HIGH)
   */
  async testCriticalNotification(req, res) {
    try {
      const result = await notificationService.sendBroadcastNotification(
        "🔥 嚴重通知",
        "這是一個嚴重等級的測試通知，優先級最高",
        "CRITICAL",
        {
          test_type: "critical",
          timestamp: new Date().toISOString(),
        }
      );

      res.json({
        success: true,
        message: "嚴重通知已發送",
        result: result,
      });
    } catch (error) {
      console.error("測試嚴重通知失敗:", error);
      res.status(500).json({
        success: false,
        message: "發送嚴重通知失敗",
        error: error.message,
      });
    }
  }

  /**
   * 測試所有等級的通知
   */
  async testAllNotifications(req, res) {
    try {
      const results = [];

      // 發送警告通知
      const warningResult = await notificationService.sendBroadcastNotification(
        "⚠️ 警告通知",
        "這是一個警告等級的測試通知",
        "WARNING",
        { test_type: "warning" }
      );
      results.push({ level: "WARNING", result: warningResult });

      // 等待1秒
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // 發送危險通知
      const dangerResult = await notificationService.sendBroadcastNotification(
        "🚨 危險通知",
        "這是一個危險等級的測試通知",
        "DANGER",
        { test_type: "danger" }
      );
      results.push({ level: "DANGER", result: dangerResult });

      // 等待1秒
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // 發送嚴重通知
      const criticalResult =
        await notificationService.sendBroadcastNotification(
          "🔥 嚴重通知",
          "這是一個嚴重等級的測試通知",
          "CRITICAL",
          { test_type: "critical" }
        );
      results.push({ level: "CRITICAL", result: criticalResult });

      res.json({
        success: true,
        message: "所有等級的通知已發送",
        results: results,
      });
    } catch (error) {
      console.error("測試所有通知失敗:", error);
      res.status(500).json({
        success: false,
        message: "發送測試通知失敗",
        error: error.message,
      });
    }
  }
}

module.exports = new TestNotificationController();
