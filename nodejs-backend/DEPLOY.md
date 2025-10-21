# Azure Web App 部署指南

## 前置需求

1. 安裝 Azure CLI
   ```bash
   brew install azure-cli  # macOS
   ```

2. 登入 Azure
   ```bash
   az login
   ```

## 部署方式一：使用部署腳本（推薦）

1. 進入 nodejs-backend 目錄
   ```bash
   cd nodejs-backend
   ```

2. 執行部署腳本
   ```bash
   ./deploy.sh
   ```

3. 腳本會自動：
   - 清理舊的部署檔案
   - 創建新的 deploy.zip（排除 node_modules, .env 等）
   - 部署到 Azure Web App
   - Azure 會自動執行 `npm install`

## 部署方式二：手動部署

1. 創建部署壓縮檔
   ```bash
   cd nodejs-backend
   zip -r deploy.zip . \
     -x "node_modules/*" \
     -x ".git/*" \
     -x ".env*" \
     -x "*.log" \
     -x ".DS_Store" \
     -x "deploy.sh" \
     -x "deploy.zip"
   ```

2. 部署到 Azure
   ```bash
   az webapp deploy \
     --resource-group angle-caring-rg \
     --name angle-caring-api \
     --src-path deploy.zip \
     --type zip \
     --async false
   ```

## 配置 Azure 應用程式設定

確保在 Azure Portal 的 Configuration > Application Settings 中設定以下環境變數：

```
NODE_ENV=production
PORT=8080 (或 Azure 自動設定的 PORT)
DB_SERVER=你的資料庫伺服器
DB_NAME=你的資料庫名稱
DB_USER=你的資料庫使用者
DB_PASSWORD=你的資料庫密碼
JWT_SECRET=你的 JWT 密鑰
FIREBASE_PROJECT_ID=你的 Firebase 專案 ID
# ... 其他環境變數
```

## 查看部署日誌

```bash
# 即時查看日誌
az webapp log tail --resource-group angle-caring-rg --name angle-caring-api

# 下載日誌
az webapp log download --resource-group angle-caring-rg --name angle-caring-api
```

## 重啟應用程式

```bash
az webapp restart --resource-group angle-caring-rg --name angle-caring-api
```

## 常見問題

### 1. 部署失敗：找不到 node_modules
**原因**: node_modules 應該被排除在部署包之外，Azure 會自動執行 npm install

**解決方案**: 確保 package.json 和 package-lock.json 都被包含在部署包中

### 2. 應用程式無法啟動
**檢查清單**:
- 確認所有環境變數都已在 Azure Portal 中設定
- 確認 package.json 中的 `start` 腳本正確
- 檢查日誌找出錯誤原因
- 確認資料庫連線設定正確

### 3. 連線資料庫失敗
**可能原因**:
- Azure SQL 防火牆規則未開放
- 連線字串設定錯誤
- 需要開啟 "Allow Azure services" 選項

### 4. 之前的 GitHub Actions 問題
如果你之前嘗試使用 GitHub Actions 但權限被拒絕，現在已經：
- 移除了 nodejs-backend 的獨立 .git 目錄
- 將 nodejs-backend 整合到主專案中
- 改用本地 Azure CLI 部署

## 驗證部署

部署成功後，訪問以下 URL 確認：

```
https://angle-caring-api.azurewebsites.net/
```

應該會看到: "Welcome to Angle Caring API Server!"

測試 API 端點：
```bash
curl https://angle-caring-api.azurewebsites.net/api/sensors
```
