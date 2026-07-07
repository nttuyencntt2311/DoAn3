const express = require('express');
const cors = require('cors');

// TODO: Sau này bạn sẽ import Mongoose ở đây
// const mongoose = require('mongoose');
// mongoose.connect('URL_MONGODB_CUA_BAN');

// 1. Cài đặt Server Express
const app = express();
app.use(cors()); 
app.use(express.json()); 

// 2. Viết API: Nhận điểm bài test TOEIC
app.post('/api/quiz/submit', async (req, res) => {
  try {
    const { userId, topic, score, totalQuestions } = req.body;

    if (!userId || score === undefined) {
      return res.status(400).json({ error: "Thiếu dữ liệu truyền lên!" });
    }

    // TODO: Khởi tạo Model Mongoose và lưu vào MongoDB
    /* Ví dụ:
    const newRecord = new HistoryModel({ userId, topic, score, totalQuestions });
    await newRecord.save();
    */

    res.status(200).json({ 
      message: "Lưu kết quả thành công!", 
      // recordId: newRecord._id 
    });

  } catch (error) {
    console.error("Lỗi khi lưu điểm:", error);
    res.status(500).json({ error: "Lỗi hệ thống máy chủ" });
  }
});

// Chạy server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});