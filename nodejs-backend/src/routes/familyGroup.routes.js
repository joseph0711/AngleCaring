const express = require("express");
const router = express.Router();
const familyGroupController = require("../controllers/familyGroup.controller");
const authMiddleware = require("../middlewares/auth.middleware");

// 所有路由都需要身份驗證
router.use(authMiddleware.verifyToken);

/**
 * @route POST /api/family-groups
 * @desc 創建新的家庭群組
 * @access 私有 (一般用戶)
 */
router.post("/", familyGroupController.createGroup);

/**
 * @route GET /api/family-groups
 * @desc 獲取當前用戶的家庭群組列表
 * @access 私有 (一般用戶)
 */
router.get("/", familyGroupController.getUserGroups);

/**
 * @route GET /api/family-groups/:groupId
 * @desc 獲取特定群組的詳細信息和成員列表
 * @access 私有 (群組成員)
 */
router.get("/:groupId", familyGroupController.getGroupDetails);

/**
 * @route PUT /api/family-groups/:groupId
 * @desc 更新群組信息
 * @access 私有 (群組管理員)
 */
router.put("/:groupId", familyGroupController.updateGroup);

/**
 * @route DELETE /api/family-groups/:groupId
 * @desc 刪除群組
 * @access 私有 (群組創建者)
 */
router.delete("/:groupId", familyGroupController.deleteGroup);

/**
 * @route POST /api/family-groups/:groupId/members
 * @desc 添加成員到群組
 * @access 私有 (群組管理員)
 */
router.post("/:groupId/members", familyGroupController.addMember);

/**
 * @route DELETE /api/family-groups/:groupId/members/:memberId
 * @desc 從群組中移除成員
 * @access 私有 (群組管理員)
 */
router.delete(
  "/:groupId/members/:memberId",
  familyGroupController.removeMember
);

module.exports = router;
