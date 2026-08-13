const User = require('../models/User'); 
const Vocabulary = require('../models/Vocabulary'); 

// API: Lấy dữ liệu thống kê cho Dashboard Admin
exports.getDashboardStats = async (req, res) => {
  try {
    // 1. Đếm tổng số từ vựng
    const totalVocab = await Vocabulary.countDocuments();

    // 2. Đếm tổng số học viên
    const totalUsers = await User.countDocuments();

    // 3. Lấy 4 người dùng đăng ký mới nhất làm "Hoạt động gần đây"
    const newestUsers = await User.find()
      .sort({ createdAt: -1 }) 
      .limit(4);

    const recentActivities = newestUsers.map(user => ({
      id: user._id,
      user: user.name,
      action: "vừa đăng ký tài khoản mới",
      time: new Date(user.createdAt).toLocaleDateString('vi-VN') 
    }));

    // Gửi dữ liệu về cho Frontend
    res.status(200).json({
      totalVocab,
      totalUsers,
      recentActivities
    });

  } catch (error) {
    console.error("Lỗi Controller Thống kê:", error);
    res.status(500).json({ message: "Lỗi Server khi lấy dữ liệu Dashboard" });
  }
};