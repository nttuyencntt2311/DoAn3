const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

// API lấy danh sách người dùng (tạm bỏ verifyToken để test)
router.get('/', userController.getAllUsers);

module.exports = router;