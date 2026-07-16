const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  gender: { type: String, default: "Nam" },
  birthDate: { type: String, default: "" },
  
  // === CÁC CHỈ SỐ GAMIFICATION (HỆ THỐNG ĐIỂM THƯỞNG) ===
  xp: { type: Number, default: 0 },                    // Điểm kinh nghiệm (Mặc định: 0)
  level: { type: Number, default: 0 },                 // Cấp độ (Mặc định: 0 - Mới nhập môn)
  streakDays: { type: Number, default: 0 },            // Chuỗi ngày học (Mặc định: 0)
  lastStudyDate: { type: Date, default: null }, // THÊM DÒNG NÀY ĐỂ TÍNH STREAK
  wordsLearned: { type: Number, default: 0 },          // Số từ vựng đã học (Mặc định: 0)
  timeLearnedMinutes: { type: Number, default: 0 },    // Thời gian học (phút) (Mặc định: 0)
  quizzesCount: { type: Number, default: 0 },          // Số bài Quiz đã làm

  // === BỔ SUNG TÍNH NĂNG "SỔ TAY / LƯU TỪ VỰNG" (NEW) ===
  // Note cho team Backend: 
  // - Trường 'savedWords' là một mảng (Array) chứa các String.
  // - Nhiệm vụ: Dùng để lưu lại _id của các từ vựng (Vocabulary) khi User bấm nút "LƯU TỪ" trên App.
  // - Mặc định ban đầu khi tạo tài khoản sẽ là mảng rỗng [].
  savedWords: { type: [String], default: [] }, 

  refreshTokens: { type: [String], default: [] }, 
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);