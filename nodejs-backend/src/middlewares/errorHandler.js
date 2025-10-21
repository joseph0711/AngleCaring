/**
 * 錯誤處理中間件
 * @param {Error} err - 錯誤對象
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 * @param {function} next - 下一個中間件函數
 */
exports.errorHandler = (err, req, res, next) => {
  console.error(err.stack);

  // 默認為500內部服務器錯誤
  const statusCode = res.statusCode !== 200 ? res.statusCode : 500;

  res.status(statusCode).json({
    success: false,
    message: err.message,
    stack: process.env.NODE_ENV === "production" ? "🥞" : err.stack,
  });
};
