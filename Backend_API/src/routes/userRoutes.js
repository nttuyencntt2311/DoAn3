const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

// API lấy danh sách người dùng (tạm bỏ verifyToken để test)
router.get('/', userController.getAllUsers);
router.put('/:id/status', userController.updateUserStatus);
module.exports = router;