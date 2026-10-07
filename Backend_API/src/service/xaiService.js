// Đường dẫn: src/service/xaiService.js
const axios = require('axios');

async function getWeaknessPrediction(studentData) {
    try {
        const response = await axios.post('http://127.0.0.1:8000/predict-weakness', {
            user_history_acc: studentData.historyAcc, 
            user_questions_attempted: studentData.totalQuestions,
            question_difficulty: studentData.questionDifficulty,
            user_avg_elapsed_time: studentData.avgTime || 0.0
        });
        return response.data; 
    } catch (error) {
        console.error("Lỗi gọi Python API:", error.message);
        return null;
    }
}

module.exports = { getWeaknessPrediction };