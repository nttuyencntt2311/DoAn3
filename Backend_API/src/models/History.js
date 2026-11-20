const mongoose = require('mongoose');

const questionLogSchema = new mongoose.Schema({
  question: { type: String, default: "" },
  topic: { type: String, default: "" },
  isCorrect: { type: Boolean, default: false },
  elapsedTimeMs: { type: Number, default: 0 }
}, { _id: false });

const historySchema = new mongoose.Schema({
  userId: { type: String, required: true },
  topic: { type: String },
  score: { type: Number, required: true },
  totalQuestions: { type: Number },
  avgElapsedTimeMs: { type: Number, default: 0 },
  questionLogs: { type: [questionLogSchema], default: [] },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('History', historySchema);
