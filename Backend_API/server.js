const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');

// 1. Nhận diện chìa khóa Firebase
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore(); 

// 2. Cài đặt Server Express
const app = express();
app.use(cors()); 
app.use(express.json()); 

// 3. Viết API đầu tiên: Nhận điểm bài test TOEIC
app.post('/api/quiz/submit', async (req, res) => {
  try {
    const { userId, topic, score, totalQuestions } = req.body;

    if (!userId || score === undefined) {
      return res.status(400).json({ error: "Thiếu dữ liệu truyền lên!" });
    }

    const historyRef = db.collection('users').doc(userId).collection('history').doc();
    
    const newRecord = {
      topic: topic,
      score: score,
      totalQuestions: totalQuestions,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await historyRef.set(newRecord);

    res.status(200).json({ 
      message: "Lưu kết quả thành công!", 
      recordId: historyRef.id 
    });

  } catch (error) {
    console.error("Lỗi khi lưu điểm:", error);
    res.status(500).json({ error: "Lỗi hệ thống máy chủ" });
  }
});

// 4. Khởi động Server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server Backend đã chạy thành công tại port ${PORT}`);
});