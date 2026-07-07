const mongoose = require('mongoose');

const historySchema = new mongoose.Schema({
  userId: { type: String, required: true },
  topic: { type: String },
  score: { type: Number, required: true },
  totalQuestions: { type: Number },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('History', historySchema);