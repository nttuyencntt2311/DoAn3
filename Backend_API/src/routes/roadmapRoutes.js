const express = require('express');
const router = express.Router();
const roadmapController = require('../controllers/roadmapController');
const verifyToken = require('../middleware/authMiddleware'); // Middleware xác thực token đăng nhập

// Lấy lộ trình đã lưu
router.get('/roadmap/user/:userId', verifyToken, roadmapController.getRoadmap);

// Tạo lộ trình mới bằng AI
router.post('/roadmap/generate', verifyToken, roadmapController.generateRoadmap);

// [BỔ SUNG MỚI]: API Hoàn thành 1 chặng trong lộ trình, cập nhật XP và Chuỗi ngày
router.post('/roadmap/complete-step', verifyToken, roadmapController.completeStep);

module.exports = router;