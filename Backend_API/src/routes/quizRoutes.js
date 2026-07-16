// File: routes/quizRoutes.js
const express = require('express');
const HistoryModel = require('../models/History');
const UserModel = require('../models/User');
const verifyToken = require('../middleware/authMiddleware'); // Nhúng middleware kiểm tra Token

const router = express.Router();

// =====================================
// API: NỘP KẾT QUẢ BÀI QUIZ / BÀI HỌC
// =====================================
router.post('/submit', verifyToken, async (req, res) => {
  try {
    // Nhận dữ liệu từ Android gửi lên (Đã thêm timeSpentMinutes)
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

    // Cộng dồn điểm kinh nghiệm, số từ vựng, và thời gian học
    user.xp += (earnedXp || 0);
    user.wordsLearned += (learnedWords || 0);
    user.timeLearnedMinutes += (timeSpentMinutes || 0);

    // =========================================================================
    // [BỔ SUNG 1]: LOGIC TỰ ĐỘNG TĂNG CẤP ĐỘ (LEVEL UP) DỰA TRÊN XP
    // =========================================================================
    const currentCalculatedLevel = Math.floor(user.xp / 100);
    
    let isLevelUp = false; 
    if (currentCalculatedLevel > user.level) {
      user.level = currentCalculatedLevel;
      isLevelUp = true; 
    }

    // =========================================================================
    // [BỔ SUNG 2]: TĂNG TỔNG SỐ BÀI QUIZ ĐÃ LÀM (Lịch sử học tập tổng quan)
    // =========================================================================
    // Kiểm tra nếu chưa có trường quizzesCount thì khởi tạo là 0 trước khi cộng
    if (user.quizzesCount === undefined || user.quizzesCount === null) {
        user.quizzesCount = 0;
    }
    user.quizzesCount += 1;

    // =========================================================================
    // --- 3. TÍNH TOÁN STREAKDAYS (CHUỖI NGÀY HỌC) CHUẨN MÚI GIỜ VN (GMT+7) ---
    // =========================================================================
    
    // Hàm phụ: Lấy chuỗi ngày YYYY-MM-DD theo đúng giờ Việt Nam
    const getVNFormatDate = (date) => {
      const vnTime = new Date(date.getTime() + (7 * 60 * 60 * 1000));
      return vnTime.toISOString().split('T')[0];
    };

    const todayVNStr = getVNFormatDate(new Date());

    if (!user.lastStudyDate) {
      // Học viên làm bài lần đầu tiên trong đời -> Khởi tạo streak = 1
      user.streakDays = 1;
    } else {
      const lastStudyVNStr = getVNFormatDate(user.lastStudyDate);
      
      const todayDate = new Date(todayVNStr);
      const lastStudyDate = new Date(lastStudyVNStr);
      
      const diffTime = todayDate - lastStudyDate;
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 1) {
        // Hôm qua học, hôm nay học tiếp
        user.streakDays += 1;
      } else if (diffDays > 1) {
        // Bỏ học quá 1 ngày -> Đứt chuỗi, reset về 1
        user.streakDays = 1;
      }
      // Nếu diffDays === 0 (Học nhiều lần trong cùng 1 ngày), streakDays giữ nguyên
    }

    // Cập nhật lại ngày học cuối cùng là hôm nay
    user.lastStudyDate = new Date();

    // =========================================================================
    // 4. LƯU LẠI THAY ĐỔI VÀ TRẢ VỀ CHO ANDROID
    // =========================================================================
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