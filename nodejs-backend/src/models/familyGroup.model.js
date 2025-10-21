const { executeQuery, sql } = require("../utils/database");

/**
 * 家庭群組模型
 */
class FamilyGroup {
  /**
   * 創建新的家庭群組
   * @param {object} groupData - 群組數據
   * @returns {Promise<object>} - 新創建的群組
   */
  static async create(groupData) {
    try {
      const result = await executeQuery(
        `INSERT INTO dbo.family_groups (group_name, description, created_by)
         VALUES (?, ?, ?);
         SELECT SCOPE_IDENTITY() AS group_id`,
        [
          groupData.groupName,
          groupData.description || null,
          groupData.createdBy,
        ]
      );

      const groupId = result.recordset[0].group_id;

      // 將創建者加入群組作為管理員
      await executeQuery(
        `INSERT INTO dbo.family_group_members (group_id, user_id, role, invited_by)
         VALUES (?, ?, 'admin', ?)`,
        [groupId, groupData.createdBy, groupData.createdBy]
      );

      return this.findById(groupId);
    } catch (error) {
      console.error("創建家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 根據ID查找群組
   * @param {number} groupId - 群組ID
   * @returns {Promise<object|null>} - 群組對象或null
   */
  static async findById(groupId) {
    try {
      const result = await executeQuery(
        `SELECT fg.*, creator.user_name as creator_name
         FROM dbo.family_groups fg
         LEFT JOIN dbo.users creator ON fg.created_by = creator.user_id
         WHERE fg.group_id = ? AND fg.is_active = 1`,
        [groupId]
      );
      return result.recordset.length > 0 ? result.recordset[0] : null;
    } catch (error) {
      console.error("查詢家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 根據ID查找群組，並包含邀請特定用戶的人的信息
   * @param {number} groupId - 群組ID
   * @param {number} userId - 當前用戶ID
   * @returns {Promise<object|null>} - 群組對象或null
   */
  static async findByIdWithInviter(groupId, userId) {
    try {
      const result = await executeQuery(
        `SELECT fg.*,
                COALESCE(inviter.user_name, creator.user_name) as creator_name
         FROM dbo.family_groups fg
         LEFT JOIN dbo.users creator ON fg.created_by = creator.user_id
         LEFT JOIN dbo.family_group_members fgm ON fg.group_id = fgm.group_id AND fgm.user_id = ?
         LEFT JOIN dbo.users inviter ON fgm.invited_by = inviter.user_id
         WHERE fg.group_id = ? AND fg.is_active = 1`,
        [userId, groupId]
      );
      return result.recordset.length > 0 ? result.recordset[0] : null;
    } catch (error) {
      console.error("查詢家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取用戶所在的所有群組
   * @param {number} userId - 用戶ID
   * @returns {Promise<Array>} - 群組列表
   */
  static async findByUserId(userId) {
    try {
      const result = await executeQuery(
        `SELECT fg.*,
                COALESCE(inviter.user_name, creator.user_name) as creator_name,
                fgm.role, fgm.joined_time
         FROM dbo.family_groups fg
         INNER JOIN dbo.family_group_members fgm ON fg.group_id = fgm.group_id
         LEFT JOIN dbo.users creator ON fg.created_by = creator.user_id
         LEFT JOIN dbo.users inviter ON fgm.invited_by = inviter.user_id
         WHERE fgm.user_id = ? AND fg.is_active = 1 AND fgm.is_active = 1
         ORDER BY fgm.joined_time DESC`,
        [userId]
      );
      return result.recordset;
    } catch (error) {
      console.error("查詢用戶家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 更新群組信息
   * @param {number} groupId - 群組ID
   * @param {object} updateData - 更新數據
   * @returns {Promise<object>} - 更新後的群組
   */
  static async update(groupId, updateData) {
    try {
      const setClause = [];
      const params = [];

      if (updateData.groupName) {
        setClause.push("group_name = ?");
        params.push(updateData.groupName);
      }
      if (updateData.description !== undefined) {
        setClause.push("description = ?");
        params.push(updateData.description);
      }

      setClause.push("updated_time = GETDATE()");
      params.push(groupId);

      await executeQuery(
        `UPDATE dbo.family_groups SET ${setClause.join(", ")} WHERE group_id = ?`,
        params
      );

      return this.findById(groupId);
    } catch (error) {
      console.error("更新家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 刪除群組（軟刪除）
   * @param {number} groupId - 群組ID
   * @returns {Promise<boolean>} - 是否成功
   */
  static async delete(groupId) {
    try {
      await executeQuery(
        "UPDATE dbo.family_groups SET is_active = 0, updated_time = GETDATE() WHERE group_id = ?",
        [groupId]
      );
      return true;
    } catch (error) {
      console.error("刪除家庭群組錯誤:", error);
      throw error;
    }
  }

  /**
   * 獲取群組成員列表
   * @param {number} groupId - 群組ID
   * @returns {Promise<Array>} - 成員列表
   */
  static async getMembers(groupId) {
    try {
      const result = await executeQuery(
        `SELECT fgm.*, u.user_name, u.Email, u.account_created_time, u.user_image, invited_user.user_name as invited_by_name
         FROM dbo.family_group_members fgm
         INNER JOIN dbo.users u ON fgm.user_id = u.user_id
         LEFT JOIN dbo.users invited_user ON fgm.invited_by = invited_user.user_id
         WHERE fgm.group_id = ? AND fgm.is_active = 1
         ORDER BY fgm.role DESC, fgm.joined_time ASC`,
        [groupId]
      );
      return result.recordset;
    } catch (error) {
      console.error("查詢群組成員錯誤:", error);
      throw error;
    }
  }

  /**
   * 添加成員到群組
   * @param {number} groupId - 群組ID
   * @param {number} userId - 用戶ID
   * @param {number} invitedBy - 邀請者ID
   * @param {string} role - 角色 (member/admin)
   * @returns {Promise<object>} - 新成員信息
   */
  static async addMember(groupId, userId, invitedBy, role = "member") {
    try {
      // 檢查用戶是否已經在群組中（包括非活躍的記錄）
      const existingMember = await executeQuery(
        "SELECT * FROM dbo.family_group_members WHERE group_id = ? AND user_id = ?",
        [groupId, userId]
      );

      if (existingMember.recordset.length > 0) {
        const member = existingMember.recordset[0];

        // 如果用戶已經是活躍成員
        if (member.is_active === 1) {
          throw new Error("用戶已經是群組成員");
        }

        // 如果用戶之前被移除，重新激活該記錄
        await executeQuery(
          "UPDATE dbo.family_group_members SET is_active = 1, role = ?, invited_by = ?, joined_time = GETDATE() WHERE group_id = ? AND user_id = ?",
          [role, invitedBy, groupId, userId]
        );
      } else {
        // 如果用戶從未加入過群組，創建新記錄
        await executeQuery(
          "INSERT INTO dbo.family_group_members (group_id, user_id, role, invited_by) VALUES (?, ?, ?, ?)",
          [groupId, userId, role, invitedBy]
        );
      }

      // 返回新添加的成員信息
      const result = await executeQuery(
        `SELECT fgm.*, u.user_name, u.Email
         FROM dbo.family_group_members fgm
         INNER JOIN dbo.users u ON fgm.user_id = u.user_id
         WHERE fgm.group_id = ? AND fgm.user_id = ? AND fgm.is_active = 1`,
        [groupId, userId]
      );

      return result.recordset[0];
    } catch (error) {
      console.error("添加群組成員錯誤:", error);
      throw error;
    }
  }

  /**
   * 從群組中移除成員
   * @param {number} groupId - 群組ID
   * @param {number} userId - 用戶ID
   * @returns {Promise<boolean>} - 是否成功
   */
  static async removeMember(groupId, userId) {
    try {
      await executeQuery(
        "UPDATE dbo.family_group_members SET is_active = 0 WHERE group_id = ? AND user_id = ?",
        [groupId, userId]
      );
      return true;
    } catch (error) {
      console.error("移除群組成員錯誤:", error);
      throw error;
    }
  }

  /**
   * 檢查用戶是否為群組管理員
   * @param {number} groupId - 群組ID
   * @param {number} userId - 用戶ID
   * @returns {Promise<boolean>} - 是否為管理員
   */
  static async isGroupAdmin(groupId, userId) {
    try {
      const result = await executeQuery(
        "SELECT role FROM dbo.family_group_members WHERE group_id = ? AND user_id = ? AND is_active = 1",
        [groupId, userId]
      );

      return (
        result.recordset.length > 0 && result.recordset[0].role === "admin"
      );
    } catch (error) {
      console.error("檢查群組管理員權限錯誤:", error);
      throw error;
    }
  }

  /**
   * 格式化群組對象
   * @param {object} group - 群組對象
   * @returns {object} - 格式化後的群組
   */
  static format(group) {
    if (!group) return null;

    return {
      groupId: group.group_id,
      groupName: group.group_name,
      description: group.description,
      createdBy: group.created_by,
      creatorName: group.creator_name,
      createdTime: group.created_time
        ? new Date(group.created_time).toISOString()
        : null,
      updatedTime: group.updated_time
        ? new Date(group.updated_time).toISOString()
        : null,
      isActive: group.is_active === 1 || group.is_active === true,
      role: group.role || null,
      joinedTime: group.joined_time
        ? new Date(group.joined_time).toISOString()
        : null,
    };
  }

  /**
   * 格式化成員對象
   * @param {object} member - 成員對象
   * @returns {object} - 格式化後的成員
   */
  static formatMember(member) {
    if (!member) return null;

    return {
      membershipId: member.membership_id,
      groupId: member.group_id,
      userId: member.user_id,
      userName: member.user_name,
      email: member.Email,
      role: member.role,
      joinedTime: member.joined_time
        ? new Date(member.joined_time).toISOString()
        : null,
      invitedBy: member.invited_by,
      invitedByName: member.invited_by_name,
      isActive: member.is_active === 1 || member.is_active === true,
      userImage: member.user_image
        ? member.user_image.toString("base64")
        : null,
    };
  }
}

module.exports = FamilyGroup;
