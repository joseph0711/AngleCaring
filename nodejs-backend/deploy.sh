#!/bin/bash

# Azure Web App 部署腳本
# 此腳本會打包 nodejs-backend 並部署到 Azure

set -e  # 遇到錯誤立即退出

echo "======================================"
echo "Azure Web App 部署腳本"
echo "======================================"

# 顏色輸出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 配置
APP_NAME="angle-caring-api"
RESOURCE_GROUP="angle-caring-rg"  # 請根據你的實際資源組名稱修改
ZIP_NAME="deploy.zip"

# 檢查是否在 nodejs-backend 目錄
if [ ! -f "package.json" ]; then
    echo -e "${RED}錯誤: 請在 nodejs-backend 目錄下執行此腳本${NC}"
    exit 1
fi

echo -e "${YELLOW}步驟 1: 清理舊的部署檔案...${NC}"
rm -f $ZIP_NAME

echo -e "${YELLOW}步驟 2: 創建部署壓縮檔...${NC}"
# 使用 zip 命令打包，排除不需要的文件
zip -r $ZIP_NAME . \
  -x "node_modules/*" \
  -x ".git/*" \
  -x ".env*" \
  -x "*.log" \
  -x ".DS_Store" \
  -x "deploy.sh" \
  -x "deploy.zip"

echo -e "${GREEN}✓ 壓縮檔創建成功: $ZIP_NAME${NC}"

# 顯示壓縮檔大小
SIZE=$(du -h $ZIP_NAME | cut -f1)
echo -e "${GREEN}✓ 檔案大小: $SIZE${NC}"

echo -e "${YELLOW}步驟 3: 檢查 Azure CLI 登入狀態...${NC}"
if ! az account show &> /dev/null; then
    echo -e "${RED}錯誤: 請先使用 'az login' 登入 Azure${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Azure CLI 已登入${NC}"

echo -e "${YELLOW}步驟 4: 部署到 Azure Web App...${NC}"
echo -e "${YELLOW}應用程式名稱: $APP_NAME${NC}"

# 部署到 Azure
# 使用 --async false 讓命令等待部署完成
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name $APP_NAME \
  --src-path $ZIP_NAME \
  --type zip \
  --async false

if [ $? -eq 0 ]; then
    echo -e "${GREEN}======================================"
    echo -e "✓ 部署成功！"
    echo -e "======================================${NC}"
    echo -e "${GREEN}應用程式 URL: https://$APP_NAME.azurewebsites.net${NC}"
    echo ""
    echo -e "${YELLOW}提示:${NC}"
    echo "1. Azure 會自動執行 npm install"
    echo "2. 確保在 Azure Portal 中配置了所有環境變數"
    echo "3. 可以使用以下命令查看日誌:"
    echo "   az webapp log tail --resource-group $RESOURCE_GROUP --name $APP_NAME"
else
    echo -e "${RED}======================================"
    echo -e "✗ 部署失敗"
    echo -e "======================================${NC}"
    echo "請檢查錯誤訊息或使用以下命令查看日誌:"
    echo "az webapp log tail --resource-group $RESOURCE_GROUP --name $APP_NAME"
    exit 1
fi

# 詢問是否清理 zip 檔案
read -p "是否刪除部署壓縮檔 $ZIP_NAME? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -f $ZIP_NAME
    echo -e "${GREEN}✓ 已刪除 $ZIP_NAME${NC}"
fi
