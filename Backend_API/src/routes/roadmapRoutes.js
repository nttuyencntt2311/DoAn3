const express = require('express');
const router = express.Router();
const roadmapController = require('../controllers/roadmapController');
const verifyToken = require('../middleware/authMiddleware'); // Middleware xác thực token đăng nhập

// Lấy lộ trình đã lưu
router.get('/roadmap/user/:userId', verifyToken, roadmapController.getRoadmap);

// Tạo lộ trình mới bằng AI
router.post('/roadmap/generate', verifyToken, roadmapController.generateRoadmap);

module.exports = router;