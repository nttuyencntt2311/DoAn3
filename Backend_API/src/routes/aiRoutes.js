const express = require('express');
const router = express.Router();
const aiController = require('../controllers/aiController');
const verifyToken = require('../middleware/authMiddleware'); // Bắt buộc đăng nhập mới được chat

// ==========================================
// 1. TẠO PHÒNG CHAT MỚI
// Endpoint: POST /api/ai/session
// ==========================================
router.post('/session', verifyToken, aiController.createChatSession);

// ==========================================
// 2. GỬI TIN NHẮN VÀO PHÒNG CHAT
// Endpoint: POST /api/ai/chat
// ==========================================
router.post('/chat', verifyToken, aiController.chatWithAI);

// ==========================================
// 3. LẤY LỊCH SỬ TIN NHẮN CỦA 1 PHÒNG CHAT CỤ THỂ
// Endpoint: GET /api/ai/session/:sessionId/messages
// ==========================================
router.get('/session/:sessionId/messages', verifyToken, aiController.getChatHistory);

// ==========================================
// Lấy danh sách các phòng chat của 1 User
// Endpoint: GET /api/ai/sessions/user/:userId
// ==========================================
router.get('/sessions/user/:userId', verifyToken, aiController.getUserChatSessions);

// ==========================================
// 4. API DÀNH CHO ROADMAP (AI SINH LỘ TRÌNH)
// ==========================================
router.post('/generate-roadmap', verifyToken, aiController.generateRoadmap);
router.get('/my-roadmap', verifyToken, aiController.getMyRoadmap);
// Sinh câu hỏi trắc nghiệm theo chủ đề chặng học
router.get('/generate-quiz', verifyToken, aiController.generateStepQuiz);
// Lệnh export bắt buộc phải nằm ở cuối cùng của file
router.post('/roadmap/complete-step', verifyToken, aiController.completeStep);
router.post('/weakness', verifyToken, aiController.predictWeakness);
// Endpoint: POST /api/ai/post-mortem
// Chức năng: Phân tích toàn bộ bài Quiz sau khi nộp để ra "Bệnh án học tập"
router.post('/post-mortem', verifyToken, aiController.generatePostMortem);

module.exports = router;