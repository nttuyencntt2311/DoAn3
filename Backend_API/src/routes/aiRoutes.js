const express = require('express');
const router = express.Router();
const aiController = require('../controllers/aiController');
const verifyToken = require('../middleware/authMiddleware'); // Bắt buộc đăng nhập mới được chat

// Endpoint: POST /api/ai/chat
router.post('/chat', verifyToken, aiController.chatWithAI);

module.exports = router;