const express = require('express');
const HistoryModel = require('../models/History'); // Nhúng mô hình lịch sử vào

const router = express.Router();

// 4.1 API: Nhận điểm bài test TOEIC và lưu vào MongoDB
router.post('/submit', async (req, res) => {
  try {
    const { userId, topic, score, totalQuestions } = req.body;
    if (!userId || score === undefined) {
      return res.status(400).json({ error: "Thiếu dữ liệu truyền lên!" });
    }

    const newRecord = new HistoryModel({ userId, topic, score, totalQuestions });
    await newRecord.save();

    res.status(200).json({ message: "Lưu kết quả thành công!", recordId: newRecord._id });
  } catch (error) {
    console.error("Lỗi khi lưu điểm:", error);
    res.status(500).json({ error: "Lỗi hệ thống máy chủ" });
  }
});

module.exports = router;