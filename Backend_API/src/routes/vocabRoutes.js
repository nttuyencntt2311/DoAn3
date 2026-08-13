const express = require("express");
const router = express.Router();
const vocabController = require("../controllers/vocabController");

// 🚀 1. BỔ SUNG IMPORT MIDDLEWARE GIẢI MÃ TOKEN (Từ code của bạn)
const verifyToken = require("../middleware/authMiddleware");

// =========================================================
// CODE CỦA BẠN: ROUTER CŨ (SỬ DỤNG "/vocabulary")
// =========================================================
router.get("/vocabulary", vocabController.getAllVocabularies);
router.get("/vocabulary/stats", vocabController.getVocabStats);
router.get("/vocabulary/:word", vocabController.getOrFetchVocabulary);

// Đường dẫn dùng để tự động bơm dữ liệu
router.post("/vocabulary/seed", vocabController.seedDefaultVocabulary);
router.post("/vocabulary", vocabController.createVocab); 
router.put("/vocabulary/:id", vocabController.updateVocab); 
router.delete("/vocabulary/:id", vocabController.deleteVocab); 

// 🚀 ĐÂY LÀ ĐƯỜNG DẪN MỚI BỔ SUNG ĐỂ LƯU ĐIỂM TỪ ANDROID:
router.put("/vocabulary/:id/proficiency", vocabController.updateProficiency);
router.post("/vocabulary/custom-quiz", vocabController.generateCustomQuiz);
router.get("/vocabulary/custom-categories", vocabController.getCustomQuizCategories);


// =========================================================
// CODE ĐƯỢC BỔ SUNG: ROUTER MỚI (SỬ DỤNG "/vocabularies" & CÓ BẢO MẬT TOKEN)
// =========================================================
// Đã đổi "vocabulary" thành "vocabularies" để khớp 100% với React Admin
// 🚀 2. GẮN VERIFYTOKEN VÀO ĐỂ API BIẾT LÀ TÀI KHOẢN NÀO ĐANG GỌI
router.get("/vocabularies", vocabController.getAllVocabularies);

// 🚀 BỔ SUNG: GẮN VERIFYTOKEN CHO CẢ API THỐNG KÊ ĐỂ LẤY ĐÚNG SỐ LIỆU CÁ NHÂN
router.get("/vocabularies/stats", verifyToken, vocabController.getVocabStats);

router.get("/vocabularies/:word", vocabController.getOrFetchVocabulary);

// Đường dẫn dùng để tự động bơm dữ liệu
router.post("/vocabularies/seed", vocabController.seedDefaultVocabulary);
router.post("/vocabularies", vocabController.createVocab);
router.put("/vocabularies/:id", vocabController.updateVocab);
router.delete("/vocabularies/:id", vocabController.deleteVocab);

// ĐƯỜNG DẪN MỚI BỔ SUNG ĐỂ LƯU ĐIỂM TỪ ANDROID:
// 🚀 3. GẮN VERIFYTOKEN CHO HÀM LƯU ĐIỂM ĐỂ KHÔNG BỊ LƯU NHẦM TÀI KHOẢN KHÁC
router.put(
  "/vocabularies/:id/proficiency",
  verifyToken,
  vocabController.updateProficiency,
);

router.post("/vocabularies/custom-quiz", vocabController.generateCustomQuiz);
router.get(
  "/vocabularies/custom-categories",
  vocabController.getCustomQuizCategories,
);

module.exports = router;