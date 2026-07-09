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
    // Công thức: Cấp độ = Phần nguyên của (Tổng XP / 100)
    // Ví dụ: 0 - 99 XP = Level 0 | 100 - 199 XP = Level 1 | 200 - 299 XP = Level 2,...
    const currentCalculatedLevel = Math.floor(user.xp / 100);
    
    let isLevelUp = false; // Biến cờ hiệu để báo về cho Android nếu người dùng lên cấp
    if (currentCalculatedLevel > user.level) {
      user.level = currentCalculatedLevel;
      isLevelUp = true; 
    }

    // =========================================================================
    // [BỔ SUNG 2]: TĂNG TỔNG SỐ BÀI QUIZ ĐÃ LÀM (Lịch sử học tập tổng quan)
    // =========================================================================
    user.quizzesCount += 1;


    // =========================================================================
    // --- 3. TÍNH TOÁN STREAKDAYS (CHUỖI NGÀY HỌC) CHUẨN MÚI GIỜ VN (GMT+7) ---
    // =========================================================================
    
    // Hàm phụ: Chuyển đổi một mốc thời gian bất kỳ về chuỗi ngày YYYY-MM-DD theo giờ VN
    const getVNFormatDate = (dateInput) => {
      const options = { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' };
      const formatter = new Intl.DateTimeFormat('en-CA', options); // 'en-CA' xuất ra định dạng YYYY-MM-DD
      return formatter.format(new Date(dateInput));
    };

    // Hàm phụ: Tính khoảng cách giữa 2 chuỗi ngày (YYYY-MM-DD) theo đơn vị Ngày
    const getDaysDiff = (dateStr1, dateStr2) => {
      const d1 = new Date(dateStr1);
      const d2 = new Date(dateStr2);
      const diffTime = d1.getTime() - d2.getTime();
      return Math.round(diffTime / (1000 * 60 * 60 * 24));
    };

    // Lấy ngày hôm nay theo múi giờ Việt Nam (Định dạng: "YYYY-MM-DD")
    const todayVNStr = getVNFormatDate(new Date());

    // Tìm bài học gần nhất TRƯỚC bài học hiện tại vừa lưu
    const lastHistory = await HistoryModel.findOne({ 
      userId, 
      _id: { $ne: newHistory._id } // Loại trừ bài vừa nộp
    }).sort({ createdAt: -1 });

    if (!lastHistory) {
      // Học viên làm bài lần đầu tiên trong đời -> Khởi tạo streak bằng 1
      user.streakDays = 1;
    } else {
      // Lấy ngày của bài học cũ gần nhất theo múi giờ Việt Nam (Định dạng: "YYYY-MM-DD")
      const lastQuizDateVNStr = getVNFormatDate(lastHistory.createdAt);

      // Tính toán khoảng cách ngày dựa trên chuỗi ngày đã loại bỏ giờ giấc
      const diffDays = getDaysDiff(todayVNStr, lastQuizDateVNStr);

      if (diffDays === 1) {
        user.streakDays += 1; // Hôm qua có học, hôm nay học tiếp -> Tăng chuỗi ngày học
      } else if (diffDays > 1) {
        user.streakDays = 1;  // Quá 1 ngày không học -> Bị đứt chuỗi, reset về ngày đầu tiên
      }
      // Lưu ý: Nếu diffDays === 0 (Trong cùng một ngày làm nhiều bài), streakDays giữ nguyên
    }

    // Lưu lại thay đổi vào MongoDB
    await user.save();

    // Trả về kết quả thành công kèm thông tin cập nhật mới nhất
    res.status(200).json({ 
      message: "Nộp kết quả bài quiz thành công!", 
      isLevelUp: isLevelUp, // Báo cho Android biết có nổ hiệu ứng chúc mừng không
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