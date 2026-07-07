// 0. IMPORT DOTENV Ở DÒNG ĐẦU TIÊN ĐỂ ĐỌC FILE .env
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const connectDB = require('./config/db.js');

const app = express();

// 2. KÍCH HOẠT KẾT NỐI DATABASE
connectDB();

// 3. KHỞI TẠO MODEL (Cấu trúc bảng lưu lịch sử làm bài TOEIC)
const historySchema = new mongoose.Schema({
  userId: { type: String, required: true },
  topic: { type: String },
  score: { type: Number, required: true },
  totalQuestions: { type: Number },
  createdAt: { type: Date, default: Date.now }
});
const HistoryModel = mongoose.model('History', historySchema);

// 4. CẤU HÌNH MIDDLEWARE
app.use(cors()); 
app.use(express.json()); 

// 5. API: Nhận điểm bài test TOEIC và lưu vào MongoDB
app.post('/api/quiz/submit', async (req, res) => {
  try {
    const { userId, topic, score, totalQuestions } = req.body;

    if (!userId || score === undefined) {
      return res.status(400).json({ error: "Thiếu dữ liệu truyền lên!" });
    }

    // Tạo bản ghi mới từ dữ liệu Android gửi lên
    const newRecord = new HistoryModel({ 
      userId, 
      topic, 
      score, 
      totalQuestions 
    });

    // Lưu trực tiếp vào MongoDB
    await newRecord.save();

    res.status(200).json({ 
      message: "Lưu kết quả thành công!", 
      recordId: newRecord._id 
    });

  } catch (error) {
    console.error("Lỗi khi lưu điểm:", error);
    res.status(500).json({ error: "Lỗi hệ thống máy chủ" });
  }
});

// 6. CHẠY SERVER
// Sẽ lấy PORT=3000 từ file .env, nếu không có thì mặc định chạy cổng 3000
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server đang chạy tại cổng http://localhost:${PORT}`);
});