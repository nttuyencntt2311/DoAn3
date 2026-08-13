const User = require("../models/User");

// 1. HÀM PHỤC HỒI LẠI ĐỂ SỬA LỖI SẬP SERVER: Lấy danh sách người dùng
exports.getAllUsers = async (req, res) => {
  try {
    const users = await User.find().select("-password"); // Lấy tất cả user nhưng giấu password đi
    res.status(200).json(users);
  } catch (error) {
    console.error("Lỗi getAllUsers:", error);
    res.status(500).json({ message: "Lỗi server khi lấy danh sách user" });
  }
};

// 2. API: Lấy thống kê cá nhân cho App Android (Trang chủ)
exports.getUserStats = async (req, res) => {
  try {
    // req.user.id lấy từ middleware xác thực (Token)
    const userId = req.user ? req.user.id : req.params.userId;

    if (!userId) {
      return res.status(400).json({ error: "Thiếu thông tin người dùng" });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng" });
    }

    // Gửi đúng cấu trúc mà Android (UserStatsData) đang chờ đợi
    res.status(200).json({
      stats: {
        name: user.name,
        xp: user.xp || 0,
        level: user.level || 1,
        streakDays: user.streakDays || 0,
        wordsLearned: user.wordsLearned || 0, // 🚀 ĐÂY LÀ CHÌA KHÓA GIÚP TRANG CHỦ HIỆN ĐÚNG SỐ
        timeLearnedMinutes: user.timeLearnedMinutes || 0,
        quizzesCount: user.quizzesCount || 0,
      },
    });
  } catch (error) {
    console.error("Lỗi getUserStats:", error);
    res.status(500).json({ error: "Lỗi server khi lấy thống kê" });
  }
};
