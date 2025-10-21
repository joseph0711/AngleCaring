const express = require("express");
const router = express.Router();
const authMiddleware = require("../middlewares/auth.middleware");
const userController = require("../controllers/user.controller");

/**
 * @route   GET /api/users
 * @desc    Get all users
 * @access  Private/Admin
 */
router.get(
  "/",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.getAllUsers
);

/**
 * @route   GET /api/users/:id
 * @desc    Get user by ID
 * @access  Private/Admin
 */
router.get(
  "/:id",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.getUserById
);

/**
 * @route   DELETE /api/users/:id
 * @desc    Delete user
 * @access  Private/Admin
 */
router.delete(
  "/:id",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.deleteUser
);

/**
 * @route   PUT /api/users/:id/admin
 * @desc    Update user admin status
 * @access  Private/Admin
 */
router.put(
  "/:id/admin",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.updateUserAdmin
);

/**
 * @route   GET /api/users/:id/stats
 * @desc    Get user statistics
 * @access  Private/Admin
 */
router.get(
  "/:id/stats",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.getUserStats
);

/**
 * @route   PUT /api/users/profile
 * @desc    Update user profile
 * @access  Private
 */
router.put(
  "/profile",
  authMiddleware.verifyToken,
  userController.updateProfile
);

/**
 * @route   PUT /api/users/password
 * @desc    Update user password
 * @access  Private
 */
router.put(
  "/password",
  authMiddleware.verifyToken,
  userController.updatePassword
);

/**
 * @route   POST /api/users/bulk-delete
 * @desc    Bulk delete users
 * @access  Private/Admin
 */
router.post(
  "/bulk-delete",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.bulkDeleteUsers
);

/**
 * @route   POST /api/users/bulk-update-admin
 * @desc    Bulk update user admin status
 * @access  Private/Admin
 */
router.post(
  "/bulk-update-admin",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.bulkUpdateUserAdmin
);

/**
 * @route   PUT /api/users/:id/profile
 * @desc    Update user profile by admin
 * @access  Private/Admin
 */
router.put(
  "/:id/profile",
  authMiddleware.verifyToken,
  authMiddleware.isAdmin,
  userController.updateUserProfile
);

module.exports = router;
