const mongoose = require('mongoose');

const roadmapStepSchema = new mongoose.Schema({
    id: { type: String, required: true },
    title: { type: String, required: true },
    description: { type: String, default: "" },
    xaiReason: { type: String, default: "" },
    status: { type: String, enum: ['COMPLETED', 'CURRENT', 'LOCKED'], default: 'LOCKED' },
    currentProgress: { type: Number, default: 0 },
    maxProgress: { type: Number, default: 0 }
});

const roadmapSchema = new mongoose.Schema({
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, unique: true },
    overallProgressPercent: { type: Number, default: 0 },
    motivationMessage: { type: String, default: "" },
    steps: [roadmapStepSchema]
}, { timestamps: true });

module.exports = mongoose.model('Roadmap', roadmapSchema);
