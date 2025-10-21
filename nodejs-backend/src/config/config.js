require("dotenv").config();

// 驗證必需的環境變量
const requiredEnvVars = [
  "JWT_SECRET",
  "JWT_EXPIRES_IN",
  "DB_SERVER",
  "DB_DATABASE",
  "DB_USER",
  "DB_PASSWORD",
];

module.exports = {
  // 服務器設置
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || "development",

  // 資料庫設置
  db: {
    server: process.env.DB_SERVER,
    database: process.env.DB_DATABASE,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    port: parseInt(process.env.DB_PORT) || 1433,
    options: {
      encrypt: true, // Azure需要加密
      trustServerCertificate: false, // 生產環境中需設為false
      enableArithAbort: true,
      schema: 'dbo', // 明確指定預設 schema
    },
  },

  // JWT設置
  jwt: {
    secret: process.env.JWT_SECRET,
    expiresIn: process.env.JWT_EXPIRES_IN || "24h",
  },
};
