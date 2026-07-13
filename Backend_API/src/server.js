// 0. IMPORT DOTENV Ở DÒNG ĐẦU TIÊN ĐỂ ĐỌC FILE .env
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db.js'); // Kéo file kết nối database vào

// 1. NHẬP CÁC FILE ĐỊNH TUYẾN (ROUTES) ĐÃ CHIA NHỎ
const authRoutes = require('./routes/authRoutes.js');   // Quản lý Đăng ký, Đăng nhập, Đăng xuất, Refresh Token
const quizRoutes = require('./routes/quizRoutes.js');   // Quản lý Nộp điểm TOEIC
const vocabRoutes = require('./routes/vocabRoutes.js'); // Quản lý Từ vựng & Lộ trình học (Của Văn)
const statsRoutes = require('./routes/statsRoutes.js'); // Quản lý Thống kê học tập (Của Tuyến)
const aiRoutes = require('./routes/aiRoutes.js');

const app = express();

// 2. KÍCH HOẠT KẾT NỐI DATABASE
connectDB();

// Chèn tạm đoạn này vào src/server.js để kiểm tra

// 3. CẤU HÌNH MIDDLEWARE CHUNG
app.use(cors()); 
app.use(express.json());


// 4. KHAI BÁO SỬ DỤNG CÁC CỤM API

// Khi app Android gọi đến địa chỉ bắt đầu bằng /api/auth/..., Express sẽ tự chuyển tiếp vào file authRoutes.js
app.use('/api/auth', authRoutes);

// Khi app Android gọi đến địa chỉ bắt đầu bằng /api/quiz/..., Express sẽ tự chuyển tiếp vào file quizRoutes.js
app.use('/api/quiz', quizRoutes);

// Kích hoạt đường dẫn cho module Thống kê học tập (Tiền tố endpoint là /api/stats)
app.use('/api/stats', statsRoutes); 

// Kích hoạt đường dẫn cho module Từ vựng & Lộ trình (Tiền tố endpoint là /api/vocabulary, /api/roadmap...)
app.use('/api', vocabRoutes);

app.use('/api/ai', aiRoutes);


// 5. KHỞI CHẠY SERVER
// Sẽ lấy PORT từ file .env, nếu không có thì mặc định chạy cổng 3000
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server Enterprise đang chạy mượt mà tại cổng http://localhost:${PORT}`);
});