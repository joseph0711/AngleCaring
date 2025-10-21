const FamilyGroup = require("../models/familyGroup.model");
const User = require("../models/user.model");

/**
 * 創建家庭群組
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.createGroup = async (req, res) => {
  try {
    const { groupName, description } = req.body;
    const userId = req.user.id;

    // 驗證輸入
    if (!groupName || groupName.trim().length === 0) {
      return res.status(400).json({
        success: false,
        message: "群組名稱是必需的",
      });
    }

    // 驗證用戶不是系統管理員
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    if (user.is_admin === 1 || user.is_admin === true) {
      return res.status(403).json({
        success: false,
        message: "系統管理員無法創建家庭群組",
      });
    }

    // 創建群組
    const groupData = {
      groupName: groupName.trim(),
      description: description ? description.trim() : null,
      createdBy: userId,
    };

    const newGroup = await FamilyGroup.create(groupData);
    const formattedGroup = FamilyGroup.format(newGroup);

    res.status(201).json({
      success: true,
      message: "家庭群組創建成功",
      group: formattedGroup,
    });
  } catch (error) {
    console.error("創建家庭群組錯誤:", error);
    res.status(500).json({
      success: false,
      message: "創建家庭群組時發生錯誤",
    });
  }
};

/**
 * 獲取用戶的家庭群組列表
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getUserGroups = async (req, res) => {
  try {
    const userId = req.user.id;

    // 驗證用戶不是系統管理員
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    if (user.is_admin === 1 || user.is_admin === true) {
      return res.status(403).json({
        success: false,
        message: "系統管理員無法查看家庭群組",
      });
    }

    const groups = await FamilyGroup.findByUserId(userId);
    const formattedGroups = groups.map((group) => FamilyGroup.format(group));

    res.status(200).json({
      success: true,
      groups: formattedGroups,
    });
  } catch (error) {
    console.error("獲取用戶群組錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取群組列表時發生錯誤",
    });
  }
};

/**
 * 獲取群組詳細信息
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.getGroupDetails = async (req, res) => {
  try {
    const { groupId } = req.params;
    const userId = req.user.id;

    // 驗證用戶不是系統管理員
    const user = await User.findById(userId);
    if (user.is_admin === 1 || user.is_admin === true) {
      return res.status(403).json({
        success: false,
        message: "系統管理員無法查看家庭群組",
      });
    }

    // 檢查用戶是否為群組成員
    const userGroups = await FamilyGroup.findByUserId(userId);
    const isMember = userGroups.some((group) => group.group_id == groupId);

    if (!isMember) {
      return res.status(403).json({
        success: false,
        message: "您不是此群組的成員",
      });
    }

    // 獲取群組訊息（包含邀請當前用戶的人的信息）
    const group = await FamilyGroup.findByIdWithInviter(groupId, userId);
    if (!group) {
      return res.status(404).json({
        success: false,
        message: "群組不存在",
      });
    }

    // 獲取群組成員
    const members = await FamilyGroup.getMembers(groupId);
    const formattedMembers = members.map((member) =>
      FamilyGroup.formatMember(member)
    );

    // 檢查當前用戶是否為管理員
    const isAdmin = await FamilyGroup.isGroupAdmin(groupId, userId);

    res.status(200).json({
      success: true,
      group: FamilyGroup.format(group),
      members: formattedMembers,
      isAdmin: isAdmin,
    });
  } catch (error) {
    console.error("獲取群組詳情錯誤:", error);
    res.status(500).json({
      success: false,
      message: "獲取群組詳情時發生錯誤",
    });
  }
};

/**
 * 添加成員到群組
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.addMember = async (req, res) => {
  try {
    const { groupId } = req.params;
    const { email, role = "member" } = req.body;
    const userId = req.user.id;

    // 驗證輸入
    if (!email) {
      return res.status(400).json({
        success: false,
        message: "用戶電子郵件是必需的",
      });
    }

    // 驗證角色
    if (!["member", "admin"].includes(role)) {
      return res.status(400).json({
        success: false,
        message: "無效的角色",
      });
    }

    // 檢查當前用戶是否為群組管理員
    const isAdmin = await FamilyGroup.isGroupAdmin(groupId, userId);
    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: "只有群組管理員可以添加成員",
      });
    }

    // 查找要添加的用戶
    const targetUser = await User.findByEmail(email);
    if (!targetUser) {
      return res.status(404).json({
        success: false,
        message: "用戶不存在",
      });
    }

    // 檢查目標用戶不是系統管理員
    if (targetUser.is_admin === 1 || targetUser.is_admin === true) {
      return res.status(400).json({
        success: false,
        message: "無法將系統管理員添加到家庭群組",
      });
    }

    // 添加成員
    const newMember = await FamilyGroup.addMember(
      groupId,
      targetUser.user_id,
      userId,
      role
    );
    const formattedMember = FamilyGroup.formatMember(newMember);

    res.status(201).json({
      success: true,
      message: "成員添加成功",
      member: formattedMember,
    });
  } catch (error) {
    console.error("添加群組成員錯誤:", error);

    if (error.message === "用戶已經是群組成員") {
      return res.status(409).json({
        success: false,
        message: error.message,
      });
    }

    res.status(500).json({
      success: false,
      message: "添加成員時發生錯誤",
    });
  }
};

/**
 * 從群組中移除成員
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.removeMember = async (req, res) => {
  try {
    const { groupId, memberId } = req.params;
    const userId = req.user.id;

    // 檢查當前用戶是否為群組管理員
    const isAdmin = await FamilyGroup.isGroupAdmin(groupId, userId);
    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: "只有群組管理員可以移除成員",
      });
    }

    // 檢查不能移除自己（群組創建者）
    const group = await FamilyGroup.findById(groupId);
    if (group.created_by == memberId) {
      return res.status(400).json({
        success: false,
        message: "無法移除群組創建者",
      });
    }

    // 移除成員
    const success = await FamilyGroup.removeMember(groupId, memberId);

    if (success) {
      res.status(200).json({
        success: true,
        message: "成員移除成功",
      });
    } else {
      res.status(404).json({
        success: false,
        message: "成員不存在或已被移除",
      });
    }
  } catch (error) {
    console.error("移除群組成員錯誤:", error);
    res.status(500).json({
      success: false,
      message: "移除成員時發生錯誤",
    });
  }
};

/**
 * 更新群組信息
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.updateGroup = async (req, res) => {
  try {
    const { groupId } = req.params;
    const { groupName, description } = req.body;
    const userId = req.user.id;

    // 檢查當前用戶是否為群組管理員
    const isAdmin = await FamilyGroup.isGroupAdmin(groupId, userId);
    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: "只有群組管理員可以更新群組信息",
      });
    }

    // 驗證輸入
    if (!groupName || groupName.trim().length === 0) {
      return res.status(400).json({
        success: false,
        message: "群組名稱是必需的",
      });
    }

    const updateData = {
      groupName: groupName.trim(),
      description: description ? description.trim() : null,
    };

    const updatedGroup = await FamilyGroup.update(groupId, updateData);
    const formattedGroup = FamilyGroup.format(updatedGroup);

    res.status(200).json({
      success: true,
      message: "群組信息更新成功",
      group: formattedGroup,
    });
  } catch (error) {
    console.error("更新群組信息錯誤:", error);
    res.status(500).json({
      success: false,
      message: "更新群組信息時發生錯誤",
    });
  }
};

/**
 * 刪除群組
 * @param {object} req - 請求對象
 * @param {object} res - 響應對象
 */
exports.deleteGroup = async (req, res) => {
  try {
    const { groupId } = req.params;
    const userId = req.user.id;

    // 檢查群組是否存在且用戶是創建者
    const group = await FamilyGroup.findById(groupId);
    if (!group) {
      return res.status(404).json({
        success: false,
        message: "群組不存在",
      });
    }

    if (group.created_by !== userId) {
      return res.status(403).json({
        success: false,
        message: "只有群組創建者可以刪除群組",
      });
    }

    const success = await FamilyGroup.delete(groupId);

    if (success) {
      res.status(200).json({
        success: true,
        message: "群組刪除成功",
      });
    } else {
      res.status(500).json({
        success: false,
        message: "刪除群組失敗",
      });
    }
  } catch (error) {
    console.error("刪除群組錯誤:", error);
    res.status(500).json({
      success: false,
      message: "刪除群組時發生錯誤",
    });
  }
};
