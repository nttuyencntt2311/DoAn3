// 0. IMPORT DOTENV Ở DÒNG ĐẦU TIÊN ĐỂ ĐỌC FILE .env
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db.js'); // Kéo file kết nối database vào

// ==========================================================
// 1. NHẬP CÁC FILE ĐỊNH TUYẾN (ROUTES) ĐÃ CHIA NHỎ
// ==========================================================
const authRoutes = require('./routes/authRoutes.js');   // Quản lý Đăng ký, Đăng nhập, Đăng xuất, Refresh Token
const quizRoutes = require('./routes/quizRoutes.js');   // Quản lý Nộp điểm Quiz/Bài tập
const vocabRoutes = require('./routes/vocabRoutes.js'); // Quản lý Từ vựng
const statsRoutes = require('./routes/statsRoutes.js'); // Quản lý Thống kê học tập & Dashboard
const aiRoutes = require('./routes/aiRoutes.js');       // Quản lý Chat AI & Khởi tạo lộ trình (Roadmap)
const userRoutes = require('./routes/userRoutes.js');   // Quản lý người dùng (Bổ sung từ code của bạn)

// (Tuỳ chọn) Nếu bạn tách riêng API Lộ trình ra file roadmapRoutes.js thì bỏ dấu // ở dòng dưới
// const roadmapRoutes = require('./routes/roadmapRoutes.js');

const app = express();

// ==========================================================
// 2. KÍCH HOẠT KẾT NỐI DATABASE
// ==========================================================
connectDB();

// ==========================================================
// 3. CẤU HÌNH MIDDLEWARE CHUNG
// ==========================================================
app.use(cors()); 
app.use(express.json());

// ==========================================================
// 4. KHAI BÁO SỬ DỤNG CÁC CỤM API
// ==========================================================
// Android gọi /api/auth/... -> Chuyển vào authRoutes.js
app.use('/api/auth', authRoutes);

// Android gọi /api/quiz/... -> Chuyển vào quizRoutes.js
app.use('/api/quiz', quizRoutes);

// Android gọi /api/stats/... -> Chuyển vào statsRoutes.js
app.use('/api/stats', statsRoutes); 

// Android gọi /api/vocabulary/... -> Chuyển vào vocabRoutes.js
app.use('/api', vocabRoutes);

// Android gọi /api/ai/chat hoặc /api/ai/generate-roadmap -> Chuyển vào aiRoutes.js
app.use('/api/ai', aiRoutes);

// Quản lý người dùng (Bổ sung từ code của bạn)
app.use('/api/users', userRoutes);

// (Tuỳ chọn) Kích hoạt nếu bạn dùng file roadmapRoutes.js riêng:
// app.use('/api', roadmapRoutes); 

// ==========================================================
// 5. KHỞI CHẠY SERVER
// ==========================================================
// Sẽ lấy PORT từ file .env, nếu không có thì mặc định chạy cổng 3000
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server Enterprise đang chạy mượt mà tại cổng http://localhost:${PORT}`);
});