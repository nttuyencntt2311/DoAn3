const express = require("express");
const router = express.Router();
const vocabController = require("../controllers/vocabController");
const verifyToken = require("../middleware/authMiddleware");
const requireAdmin = require("../middleware/requireAdmin");

// =========================================================
// ROUTER TỪ VỰNG CHUẨN DUY NHẤT ("/vocabularies")
// =========================================================
router.get("/vocabularies", vocabController.getAllVocabularies);
router.get("/vocabularies/stats", verifyToken, vocabController.getVocabStats);

// Sổ tay Tiên tri XAI (đặt trước /:word để tránh xung đột định tuyến)
router.get("/vocabularies/predict-forgetting", verifyToken, vocabController.getPredictiveFlashcards);
router.get("/vocabularies/custom-categories", verifyToken, vocabController.getCustomQuizCategories);
router.get("/vocabularies/:word", vocabController.getOrFetchVocabulary);

// Chỉ Admin được phép Seed / Thêm / Sửa / Xóa kho từ vựng
router.post("/vocabularies/seed", verifyToken, requireAdmin, vocabController.seedDefaultVocabulary);
router.post("/vocabularies", verifyToken, requireAdmin, vocabController.createVocab);
router.put("/vocabularies/:id", verifyToken, requireAdmin, vocabController.updateVocab);
router.delete("/vocabularies/:id", verifyToken, requireAdmin, vocabController.deleteVocab);

// Yêu cầu đăng nhập cho các hành động của học viên
router.put("/vocabularies/:id/proficiency", verifyToken, vocabController.updateProficiency);
router.post("/vocabularies/custom-quiz", verifyToken, vocabController.generateCustomQuiz);

module.exports = router;
