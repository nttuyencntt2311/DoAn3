const User = require("../models/User");

// Công thức cấp độ lũy tiến thống nhất: bắt đầu từ Cấp 1, tăng đều theo căn bậc hai của XP
const calculateLevel = (xp) => Math.floor(Math.sqrt(Math.max(0, Number(xp) || 0) / 100)) + 1;

// API: Lấy Bảng xếp hạng Top 10 XP
exports.getLeaderboard = async (req, res) => {
  try {
    const topUsers = await User.find()
      .sort({ xp: -1 })
      .limit(10)
      .select("name xp level");

    const normalized = topUsers.map((u) => ({
      _id: u._id,
      name: u.name,
      xp: u.xp || 0,
      level: calculateLevel(u.xp || 0),
    }));

    res.status(200).json(normalized);
  } catch (error) {
    console.error("Lỗi getLeaderboard:", error);
    res.status(500).json({ error: "Lỗi server khi lấy bảng xếp hạng" });
  }
};

// API: Lấy danh sách người dùng
exports.getAllUsers = async (req, res) => {
  try {
    const users = await User.find().select("-password");
    res.status(200).json(users);
  } catch (error) {
    console.error("Lỗi getAllUsers:", error);
    res.status(500).json({ message: "Lỗi server khi lấy danh sách user" });
  }
};

// API: Lấy thống kê cá nhân cho App Android (Trang chủ)
exports.getUserStats = async (req, res) => {
  try {
    const userId = req.user ? (req.user.id || req.user.userId) : req.params.userId;

    if (!userId) {
      return res.status(400).json({ error: "Thiếu thông tin người dùng" });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng" });
    }

    const masteredCount = Array.isArray(user.vocabProgress)
      ? user.vocabProgress.filter((v) => (v.proficiency || 0) >= 80).length
      : (user.wordsLearned || 0);

    // Chặn hiển thị số phút bị hỏng do lỗi timestamp cũ (> 100,000 phút)
    const rawMinutes = Number(user.timeLearnedMinutes) || 0;
    const safeMinutes = rawMinutes > 100000 ? 0 : Math.max(0, rawMinutes);

    res.status(200).json({
      stats: {
        name: user.name,
        xp: user.xp || 0,
        level: calculateLevel(user.xp || 0),
        streakDays: user.streakDays || 0,
        wordsLearned: masteredCount,
        timeLearnedMinutes: safeMinutes,
        quizzesCount: user.quizzesCount || 0,
      },
    });
  } catch (error) {
    console.error("Lỗi getUserStats:", error);
    res.status(500).json({ error: "Lỗi server khi lấy thống kê" });
  }
};

// API: Cập nhật trạng thái người dùng (Khóa / Mở khóa)
exports.updateUserStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    if (!["Active", "Banned"].includes(status)) {
      return res.status(400).json({ message: "Trạng thái không hợp lệ" });
    }

    const updatedUser = await User.findByIdAndUpdate(
      id,
      { status: status },
      { new: true }
    ).select("-password");

    if (!updatedUser) {
      return res.status(404).json({ message: "Không tìm thấy người dùng" });
    }

    res.status(200).json(updatedUser);
  } catch (error) {
    console.error("Lỗi updateUserStatus:", error);
    res.status(500).json({ message: "Lỗi server khi cập nhật trạng thái" });
  }
};
