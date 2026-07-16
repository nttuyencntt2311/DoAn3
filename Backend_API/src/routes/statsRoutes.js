// File: routes/statsRoutes.js
const express = require('express');
const UserModel = require('../models/User');
const HistoryModel = require('../models/History');
const VocabularyModel = require('../models/Vocabulary'); // BỔ SUNG MODEL TỪ VỰNG
const verifyToken = require('../middleware/authMiddleware');

const router = express.Router();

// =====================================
// API: LẤY THỐNG KÊ CHI TIẾT HỌC TẬP
// =====================================
router.get('/learning', verifyToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const user = await UserModel.findById(userId);
    if (!user) return res.status(404).json({ error: "Không tìm thấy người dùng" });

    // 1. Tạo mảng dữ liệu biểu đồ 7 ngày qua từ bảng History
    const histories = await HistoryModel.find({ userId }).sort({ createdAt: 1 });
    let chartData = [0, 0, 0, 0, 0, 0, 0]; 
    histories.forEach(h => {
      const date = new Date(h.createdAt);
      let dayIndex = date.getDay() - 1; // 0 là CN, 1 là T2...
      if (dayIndex === -1) dayIndex = 6; // Đổi CN xuống cuối mảng (Index 6)
      
      chartData[dayIndex] += (h.totalQuestions || 0) * 5; 
      if (chartData[dayIndex] > 100) chartData[dayIndex] = 100; // Tối đa 100% cột
    });

    // =========================================================================
    // 2. KÉO DỮ LIỆU TỪ BẢNG VOCABULARY ĐỂ ĐỒNG BỘ VỚI MÀN HÌNH LEARNING
    // =========================================================================
    const allVocabs = await VocabularyModel.find();
    
    // Tổng số từ vựng (Sẽ đổ vào tvTotalWords)
    const totalWordsCount = allVocabs.length;

    // Đếm số từ đã học (Thành thạo >= 80)
    const masteredCount = allVocabs.filter(v => v.proficiency >= 80).length;
    
    // Tính phần trăm thành thạo (Sẽ đổ vào tvAccuracy)
    const masteryPercent = totalWordsCount > 0 ? Math.round((masteredCount / totalWordsCount) * 100) : 0;

    // =========================================================================
    // 3. TRẢ VỀ JSON CHO ANDROID (StatisticsViewModel sẽ tự động hứng)
    // =========================================================================
    res.status(200).json({
      stats: {
        streakDays: user.streakDays || 0,
        totalHours: Math.round((user.timeLearnedMinutes || 0) / 60), 
        totalWords: totalWordsCount,          // Dữ liệu đã đồng bộ
        accuracyPercent: masteryPercent,      // Dữ liệu đã đồng bộ
        chartData: chartData
      },
      memories: [
        {
          date: "Hôm nay",
          title: `🔥 Chuỗi ${user.streakDays || 0} ngày rực cháy`,
          description: `Bạn đã duy trì thói quen học tiếng Anh liên tiếp chuẩn chỉ!`,
          xp: user.xp || 0
        }
      ]
    });

  } catch (error) {
    console.error("Lỗi lấy thống kê:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi lấy thống kê học tập!" });
  }
});

module.exports = router;