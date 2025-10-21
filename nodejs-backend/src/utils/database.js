const sql = require("mssql");
const config = require("../config/config");

// 創建連接池
const poolPromise = new sql.ConnectionPool(config.db)
  .connect()
  .then((pool) => {
    return pool;
  })
  .catch((err) => {
    console.error("Database connection failed:", err);
    throw err;
  });

// 執行SQL查詢
async function executeQuery(query, params = []) {
  try {
    const pool = await poolPromise;
    const request = pool.request();

    // 添加參數
    params.forEach((param, index) => {
      request.input(`param${index + 1}`, param);
    });

    // 修改查詢語句，參數替換為 @param1, @param2 等
    let paramCount = 0;
    const parameterizedQuery = query.replace(
      /\?/g,
      () => `@param${++paramCount}`
    );

    const result = await request.query(parameterizedQuery);
    return result;
  } catch (error) {
    console.error("SQL執行錯誤:", error);
    throw error;
  }
}

// 關閉連接
async function closePool() {
  try {
    const pool = await poolPromise;
    await pool.close();
  } catch (error) {
    console.error("關閉SQL連接池錯誤:", error);
  }
}

module.exports = {
  executeQuery,
  closePool,
  poolPromise,
  sql, // 導出sql模組以便可以直接使用其類型
};
