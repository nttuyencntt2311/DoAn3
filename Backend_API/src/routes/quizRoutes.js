// File: routes/quizRoutes.js
const express = require('express');
const HistoryModel = require('../models/History');
const UserModel = require('../models/User');
const verifyToken = require('../middleware/authMiddleware');

const router = express.Router();

// =====================================
// API: NỘP KẾT QUẢ BÀI QUIZ / BÀI HỌC
// =====================================
router.post('/submit', verifyToken, async (req, res) => {
  try {
    const { topic, score, totalQuestions, earnedXp, learnedWords, timeSpentMinutes } = req.body;
    const userId = req.user.userId;

    // 1. LƯU LỊCH SỬ LÀM BÀI VÀO BẢNG HISTORY
    const newHistory = new HistoryModel({
      userId: userId,
      topic: topic || "Bài học chung",
      score: score || 0,
      totalQuestions: totalQuestions || 0
    });
    await newHistory.save();

    // 2. CỘNG DỒN CHỈ SỐ VÀO BẢNG USER (GAMIFICATION)
    const user = await UserModel.findById(userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng!" });
    }

    user.xp += (earnedXp || 0);
    user.wordsLearned += (learnedWords || 0);
    user.timeLearnedMinutes += (timeSpentMinutes || 0);

    // [BỔ SUNG 1]: TỰ ĐỘNG TĂNG LEVEL DỰA TRÊN XP
    const currentCalculatedLevel = Math.floor(user.xp / 100);
    let isLevelUp = false; 
    if (currentCalculatedLevel > user.level) {
      user.level = currentCalculatedLevel;
      isLevelUp = true; 
    }

    // [BỔ SUNG 2]: TĂNG TỔNG SỐ BÀI QUIZ
    if (user.quizzesCount === undefined || user.quizzesCount === null) {
        user.quizzesCount = 0;
    }
    user.quizzesCount += 1;

    // =========================================================================
    // --- 3. TÍNH TOÁN STREAKDAYS (ĐÃ SỬA LỖI BỊ KẸT SỐ 0 KHI HỌC CÙNG NGÀY) ---
    // =========================================================================
    const getVNFormatDate = (date) => {
      const vnTime = new Date(date.getTime() + (7 * 60 * 60 * 1000));
      return vnTime.toISOString().split('T')[0];
    };

    const todayVNStr = getVNFormatDate(new Date());

    // 👉 ĐÃ SỬA: Nếu chưa có lastStudyDate HOẶC streakDays đang bằng 0 -> Set về 1 ngay!
    if (!user.lastStudyDate || !user.streakDays || user.streakDays === 0) {
      user.streakDays = 1;
    } else {
      const lastStudyVNStr = getVNFormatDate(user.lastStudyDate);
      
      const todayDate = new Date(todayVNStr);
      const lastStudyDate = new Date(lastStudyVNStr);
      
      const diffTime = todayDate - lastStudyDate;
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 1) {
        // Hôm qua học, hôm nay học tiếp -> Tăng dồn 1 ngày
        user.streakDays += 1;
      } else if (diffDays > 1) {
        // Bỏ học quá 1 ngày -> Reset về 1
        user.streakDays = 1;
      }
      // Nếu diffDays === 0 (Học nhiều bài trong cùng 1 ngày) -> Giữ nguyên streakDays (lúc này đã là >= 1)
    }

    // Cập nhật ngày học cuối cùng là thời điểm hiện tại
    user.lastStudyDate = new Date();

    // 4. LƯU LẠI THAY ĐỔI VÀ TRẢ VỀ CHO ANDROID
    await user.save();

    res.status(200).json({ 
      message: "Nộp kết quả bài quiz thành công!", 
      isLevelUp: isLevelUp, 
      updatedStats: {
        xp: user.xp,
        level: user.level,
        streakDays: user.streakDays,
        wordsLearned: user.wordsLearned,
        timeLearnedMinutes: user.timeLearnedMinutes,
        quizzesCount: user.quizzesCount
      }
    });

  } catch (error) {
    console.error("Lỗi nộp bài:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi xử lý nộp bài quiz!" });
  }
});

module.exports = router;