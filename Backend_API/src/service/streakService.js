// File: service/streakService.js
const UserModel = require('../models/User');

// Hàm hỗ trợ: Lấy ngày định dạng YYYY-MM-DD theo đúng múi giờ Việt Nam
const getVNDateStr = (date = new Date()) => {
  // Cộng thêm 7 tiếng từ UTC để ra giờ VN
  const vnTime = new Date(date.getTime() + (7 * 60 * 60 * 1000));
  return vnTime.toISOString().split('T')[0];
};

const updateStreak = async (userId) => {
  try {
    const user = await UserModel.findById(userId);
    if (!user) return null;

    const todayStr = getVNDateStr(new Date()); // Ngày hôm nay (VN)

    if (!user.lastStudyDate) {
      // TRƯỜNG HỢP 1: Học lần đầu tiên trong đời -> Bắt đầu Streak = 1
      user.streakDays = 1;
    } else {
      const lastStudyStr = getVNDateStr(user.lastStudyDate); // Ngày học lần cuối (VN)
      
      const todayDate = new Date(todayStr);
      const lastStudyDate = new Date(lastStudyStr);
      
      // Tính khoảng cách giữa 2 ngày
      const diffTime = todayDate - lastStudyDate;
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 1) {
        // TRƯỜNG HỢP 2: Hôm qua học, hôm nay học tiếp -> Cộng dồn
        user.streakDays += 1;
      } else if (diffDays > 1) {
        // TRƯỜNG HỢP 3: Bỏ học quá 1 ngày -> Đứt chuỗi, reset về 1
        user.streakDays = 1;
      }
      // TRƯỜNG HỢP 4: diffDays === 0 (Học nhiều bài trong cùng 1 ngày) -> Không làm gì cả, giữ nguyên streakDays
    }

    // Luôn luôn cập nhật thời điểm học cuối cùng là bây giờ
    user.lastStudyDate = new Date(); 
    
    await user.save();
    return user; // Trả về object user đã được cập nhật
  } catch (error) {
    console.error("Lỗi cập nhật streak:", error);
    return null;
  }
};

module.exports = { updateStreak };