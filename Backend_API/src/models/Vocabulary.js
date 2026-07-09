const mongoose = require('mongoose');

const vocabSchema = new mongoose.Schema({
  word: { type: String, required: true, trim: true },
  phonetic: { type: String, default: "" },
  meaning: { type: String, required: true },
  
  // 1. BỔ SUNG CATEGORY ĐỂ LỌC THEO TAB (Công việc, Du lịch...)
  category: { 
    type: String, 
    default: 'Tất cả' 
  },

  // 2. BỔ SUNG PROFICIENCY ĐỂ TÍNH ĐIỂM "ĐÃ THUỘC", "ĐANG HỌC"
  proficiency: { 
    type: Number, 
    default: 0 
  },
  
  // ĐÃ SỬA LỖI LOGIC: Chuyển required sang false. 
  // Vì khi gọi hàm getOrFetchVocabulary để tra cứu từ bên ngoài, từ vựng chưa thuộc lộ trình nào cả.
  roadmapId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Roadmap',
    required: false 
  },
  
  status: { type: String, enum: ['Active', 'Draft'], default: 'Active' }
}, { 
  timestamps: true 
});

module.exports = mongoose.model('Vocabulary', vocabSchema);