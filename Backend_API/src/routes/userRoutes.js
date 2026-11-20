const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const verifyToken = require('../middleware/authMiddleware');
const requireAdmin = require('../middleware/requireAdmin');

// Chỉ Admin đã xác thực mới được xem toàn bộ danh sách và khóa/mở khóa tài khoản
router.get('/', verifyToken, requireAdmin, userController.getAllUsers);
router.put('/:id/status', verifyToken, requireAdmin, userController.updateUserStatus);

// Bảng xếp hạng yêu cầu đăng nhập (Token hợp lệ)
router.get('/leaderboard', verifyToken, userController.getLeaderboard);

module.exports = router;
