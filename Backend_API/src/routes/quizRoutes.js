const express = require('express');
const HistoryModel = require('../models/History'); // Nhúng mô hình lịch sử vào
const verifyToken = require('../middleware/authMiddleware'); // Nhúng middleware bảo mật vào đây

const router = express.Router();

// =====================================
// API: Nhận điểm bài test TOEIC và lưu vào MongoDB - ĐÃ BẢO MẬT
// =====================================
router.post('/submit', verifyToken, async (req, res) => {
  try {
    // Chỉ nhận thông tin bài thi từ client, không nhận userId từ body nữa nhằm tránh hack điểm hộ
    const { topic, score, totalQuestions } = req.body;
    
    // Tự động gán userId bằng ID trích xuất từ Token
    const userId = req.user.userId;

    if (score === undefined) {
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