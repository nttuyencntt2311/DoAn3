const mongoose = require('mongoose');

const roadmapSchema = new mongoose.Schema({
  name: { type: String, required: true }, // VD: "Nền tảng", "Giao tiếp"
  description: { type: String, default: "" },
  totalWords: { type: Number, default: 0 }
}, { 
  timestamps: true // Tự động lưu ngày tạo, ngày cập nhật
}); 

module.exports = mongoose.model('Roadmap', roadmapSchema);