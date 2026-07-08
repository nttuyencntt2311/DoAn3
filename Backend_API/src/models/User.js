const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  gender: { type: String, default: "Nam" },
  birthDate: { type: String, default: "" },
  
  // === THÊM CÁC CHỈ SỐ GAMIFICATION ===
  xp: { type: Number, default: 0 },                    // Điểm kinh nghiệm (Mặc định: 0)
  level: { type: String, default: "Mới nhập môn" },    // Cấp độ (Mặc định: Mới nhập môn)
  streakDays: { type: Number, default: 0 },            // Chuỗi ngày học (Mặc định: 0)
  wordsLearned: { type: Number, default: 0 },          // Số từ vựng đã học (Mặc định: 0)
  timeLearnedMinutes: { type: Number, default: 0 },    // Thời gian học (phút) (Mặc định: 0)
  
  refreshTokens: { type: [String], default: [] }, 
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);