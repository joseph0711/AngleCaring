const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const morgan = require("morgan");
const config = require("./config/config");
const fs = require("fs");
const path = require("path");
const { errorHandler } = require("./middlewares/errorHandler");

// 引入路由
const authRoutes = require("./routes/auth.routes");
const sensorRoutes = require("./routes/sensor.routes");
const alarmRoutes = require("./routes/alarm.routes");
const bedTimeRoutes = require("./routes/bedTime.routes");
const userRoutes = require("./routes/user.routes");
const familyGroupRoutes = require("./routes/familyGroup.routes");
const testNotificationRoutes = require("./routes/testNotification.routes");
const monitoringStatusRoutes = require("./routes/monitoringStatus.routes");
const sensorStatusRoutes = require("./routes/sensorStatus.routes");

// 創建服務器
const app = express();

// 創建日誌流
const accessLogStream = fs.createWriteStream(
  path.join(__dirname, "../server.log"),
  { flags: "a" }
);

// 中間件
app.use(cors());
app.use(helmet());
app.use(morgan("combined", { stream: accessLogStream }));
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ extended: true, limit: "50mb" }));

// 健康檢查路由
app.get("/", (req, res) => {
  res.send("Welcome to Angle Caring API Server!");
});

// API路由
app.use("/api/auth", authRoutes);
app.use("/api/sensors", sensorRoutes);
app.use("/api/alarms", alarmRoutes);
app.use("/api/bed-time-settings", bedTimeRoutes);
app.use("/api/users", userRoutes);
app.use("/api/family-groups", familyGroupRoutes);
app.use("/api/test-notifications", testNotificationRoutes);
app.use("/api/monitoring-status", monitoringStatusRoutes);
app.use("/api/sensor-status", sensorStatusRoutes);

// 錯誤處理中間件
app.use(errorHandler);

// 啟動服務器
const PORT = config.port;

const server = app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

// 處理端口被佔用的錯誤
server.on("error", (err) => {
  if (err.code === "EADDRINUSE") {
    console.error(
      `Port ${PORT} is already in use. Please try a different port.`
    );
    console.error(
      "You can set a different port by setting the PORT environment variable:"
    );
    console.error("PORT=3003 npm run dev");
    process.exit(1);
  } else {
    console.error("Server error:", err);
    process.exit(1);
  }
});

// 處理未捕獲的異常
process.on("uncaughtException", (error) => {
  console.error("Unhandled exception:", error);
  process.exit(1);
});

// 處理未處理的Promise拒絕
process.on("unhandledRejection", (reason, promise) => {
  console.error("Unhandled promise rejection:", reason);
});

module.exports = app;
