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
    user.quizzesCount += 1; // <-- BỔ SUNG: Tăng số bài quiz học viên đã làm lên 1
    // Tính toán lại Cấp độ (Level): Cứ 100 XP thì lên 1 cấp
    user.level = Math.floor(user.xp / 100);

    // --- 3. LOGIC TÍNH STREAK DAYS (CHUỖI NGÀY HỌC) ---
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Đưa về 00:00:00 để so sánh ngày chuẩn xác

    // Tìm bài học gần nhất TRƯỚC bài học hiện tại vừa lưu
    const lastHistory = await HistoryModel.findOne({ 
      userId, 
      _id: { $ne: newHistory._id } // Loại trừ bài vừa tạo ra
    }).sort({ createdAt: -1 });

    if (!lastHistory) {
      // Học viên làm bài lần đầu tiên trong đời
      user.streakDays = 1;
    } else {
      const lastQuizDate = new Date(lastHistory.createdAt);
      lastQuizDate.setHours(0, 0, 0, 0);

      const diffTime = today.getTime() - lastQuizDate.getTime();
      const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24)); // Quy đổi ra số ngày lệch

      if (diffDays === 1) {
        user.streakDays += 1; // Hôm qua có học, hôm nay học tiếp -> Tăng chuỗi ngày
      } else if (diffDays > 1) {
        user.streakDays = 1;  // Quá 1 ngày không học -> Bị đứt chuỗi, reset về 1
      }
      // Lưu ý: Nếu diffDays === 0 (Hôm nay học nhiều bài), streakDays giữ nguyên không đổi
    }

    await user.save();

    // Trả về kết quả thành công kèm các chỉ số mới nhất để Frontend nếu cần có thể cập nhật ngay
    res.status(200).json({ 
        message: "Lưu kết quả thành công!",
        streakDays: user.streakDays,
        level: user.level,
        xp: user.xp
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Lỗi hệ thống khi nộp bài!" });
  }
});

module.exports = router;