const axios = require("axios");

const PYTHON_AI_URL = process.env.PYTHON_AI_URL || "http://127.0.0.1:8000";
const DEFAULT_AVG_TIME_MS = 23000.0;

async function getWeaknessPrediction(studentData) {
  try {
    const safeAvgTime =
      studentData.avgTime && studentData.avgTime > 0
        ? studentData.avgTime
        : DEFAULT_AVG_TIME_MS;
    const safeElapsed =
      studentData.elapsedTime && studentData.elapsedTime > 0
        ? studentData.elapsedTime
        : safeAvgTime;

    const response = await axios.post(`${PYTHON_AI_URL}/predict-weakness`, {
      user_history_acc: studentData.historyAcc ?? 0.5,
      user_questions_attempted: studentData.totalQuestions ?? 0,
      question_difficulty: studentData.questionDifficulty ?? 0.5,
      user_avg_elapsed_time: safeAvgTime,
      rolling_acc_5: studentData.rollingAcc5 ?? 0.5,
      time_diff: Math.max(0, studentData.timeDiff ?? 0.0),
      elapsed_time: safeElapsed,
    });
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/predict-weakness):", error.message);
    return null;
  }
}

async function getPreQuizPrediction(studentData) {
  try {
    const safeAvgTime =
      studentData.avgTime && studentData.avgTime > 0
        ? studentData.avgTime
        : DEFAULT_AVG_TIME_MS;

    const response = await axios.post(`${PYTHON_AI_URL}/predict-prequiz`, {
      user_history_acc: studentData.historyAcc ?? 0.5,
      user_questions_attempted: studentData.totalQuestions ?? 0,
      question_difficulty: studentData.questionDifficulty ?? 0.5,
      user_avg_elapsed_time: safeAvgTime,
      rolling_acc_5: studentData.rollingAcc5 ?? 0.5,
      time_diff: Math.max(0, studentData.timeDiff ?? 0.0),
    });
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/predict-prequiz):", error.message);
    return null;
  }
}

async function getForgettingPrediction(cardData) {
  try {
    const response = await axios.post(`${PYTHON_AI_URL}/predict-forgetting`, {
      user_history_acc: cardData.historyAcc ?? 0.5,
      user_questions_attempted: cardData.totalQuestions ?? 0,
      proficiency: cardData.proficiency ?? 0,
      mistake_count: cardData.mistakeCount ?? 0,
      review_count: cardData.reviewCount ?? 1,
      days_since_review: Math.max(0, cardData.daysSinceReview ?? 0),
      user_avg_elapsed_time:
        cardData.avgTime && cardData.avgTime > 0
          ? cardData.avgTime
          : DEFAULT_AVG_TIME_MS,
    });
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/predict-forgetting):", error.message);
    return null;
  }
}


async function getGlobalXai() {
  try {
    const response = await axios.get(`${PYTHON_AI_URL}/global-xai`, { timeout: 5000 });
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/global-xai):", error.message);
    return null;
  }
}

async function getBatchRiskAnalysis(students) {
  try {
    const response = await axios.post(
      `${PYTHON_AI_URL}/batch-risk-analysis`,
      { students },
      { timeout: 10000 }
    );
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/batch-risk-analysis):", error.message);
    return null;
  }
}

module.exports = {
  getWeaknessPrediction,
  getPreQuizPrediction,
  getForgettingPrediction,
  getGlobalXai,
  getBatchRiskAnalysis,
};
