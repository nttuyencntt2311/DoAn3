
require('dotenv').config();


const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db.js');

// ==========================================================
// 1. IMPORT CÁC ROUTES CHÍNH THỨC (ĐÃ CHUẨN HÓA)
// ==========================================================
const authRoutes = require('./routes/authRoutes.js');
const quizRoutes = require('./routes/quizRoutes.js');
const vocabRoutes = require('./routes/vocabRoutes.js');
const statsRoutes = require('./routes/statsRoutes.js');
const aiRoutes = require('./routes/aiRoutes.js');
const userRoutes = require('./routes/userRoutes.js');

const app = express();

// ==========================================================
// 2. KẾT NỐI MONGODB
// ==========================================================
connectDB();

// ==========================================================
// 3. MIDDLEWARE CHUNG
// ==========================================================
app.use(cors());
app.use(express.json());

// ==========================================================
// 4. ĐỊNH TUYẾN API
// ==========================================================

app.use('/api/auth', authRoutes);


app.use('/api/quiz', quizRoutes);
app.use('/api/stats', statsRoutes);
app.use('/api', vocabRoutes);


app.use('/api/ai', aiRoutes);


app.use('/api/users', userRoutes);



// ==========================================================
// 5. KHỞI CHẠY SERVER (ĐỒNG BỘ CỔNG MẶC ĐỊNH 8080)
// ==========================================================
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`🚀 Server LexiVT EdTech XAI đang chạy tại http://localhost:${PORT}`);
});
