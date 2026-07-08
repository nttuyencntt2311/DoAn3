const express = require('express');
const router = express.Router();
const vocabController = require('../controllers/vocabController');

router.get('/vocabulary', vocabController.getAllVocabularies);
router.get('/vocabulary/stats', vocabController.getVocabStats);
router.get('/vocabulary/:word', vocabController.getOrFetchVocabulary);
router.post('/vocabulary', vocabController.createVocab); // Đã thêm /vocabulary
router.put('/vocabulary/:id', vocabController.updateVocab); // Đã thêm /vocabulary
router.delete('/vocabulary/:id', vocabController.deleteVocab); // Đã thêm /vocabulary
module.exports = router;