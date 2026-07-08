// File: routes/statsRoutes.js
const express = require('express');
const UserModel = require('../models/User');
const HistoryModel = require('../models/History');
const verifyToken = require('../middleware/authMiddleware');

const router = express.Router();

// =====================================
// API: LẤY THỐNG KÊ CHI TIẾT HỌC TẬP
// Địa chỉ thực tế sẽ thành: GET /api/stats/learning
// =====================================
router.get('/learning', verifyToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const user = await UserModel.findById(userId);
    if (!user) return res.status(404).json({ error: "Không tìm thấy người dùng" });

    // Lấy toàn bộ lịch sử làm bài để tính toán dữ liệu
    const histories = await HistoryModel.find({ userId }).sort({ createdAt: 1 });

    // 1. Tính tỷ lệ chính xác trung bình (%)
    let totalCorrect = 0;
    let totalQuestions = 0;
    histories.forEach(h => {
      totalCorrect += h.score || 0;
      totalQuestions += h.totalQuestions || 0;
    });
    const accuracyPercent = totalQuestions > 0 ? Math.round((totalCorrect / totalQuestions) * 100) : 0;

    // 2. Tạo mảng dữ liệu biểu đồ 7 ngày qua (T2 đến CN)
    let chartData = [0, 0, 0, 0, 0, 0, 0]; 
    histories.forEach(h => {
      const date = new Date(h.createdAt);
      let dayIndex = date.getDay() - 1; // 0 là CN, 1 là T2...
      if (dayIndex === -1) dayIndex = 6; // Đổi CN xuống cuối mảng (Index 6)
      
      chartData[dayIndex] += (h.totalQuestions || 0) * 5; 
      if (chartData[dayIndex] > 100) chartData[dayIndex] = 100; // Tối đa 100% cột
    });

    // Trả về gói dữ liệu thống kê sạch sẽ
    res.status(200).json({
      streakDays: user.streakDays || 0,
      totalHours: Math.round((user.timeLearnedMinutes || 0) / 60), // Đổi từ phút ra giờ
      totalWords: user.wordsLearned || 0,
      accuracyPercent: accuracyPercent,
      chartData: chartData
    });

  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống khi lấy thống kê học tập!" });
  }
});

module.exports = router;