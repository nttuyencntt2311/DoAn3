const express = require("express");
const router = express.Router();
const vocabController = require("../controllers/vocabController");

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

module.exports = router;