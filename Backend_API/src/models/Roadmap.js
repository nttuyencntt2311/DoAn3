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
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    topic: { type: String, default: "Lộ trình cá nhân" },
    target: { type: String, default: "" },
    currentLevel: { type: String, default: "" },
    isActive: { type: Boolean, default: true },
    overallProgressPercent: { type: Number, default: 0 },
    motivationMessage: { type: String, default: "" },
    steps: [roadmapStepSchema]
}, { timestamps: true });

roadmapSchema.index({ userId: 1, isActive: 1 });

module.exports = mongoose.model('Roadmap', roadmapSchema);
