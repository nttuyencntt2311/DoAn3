const axios = require("axios");
const http = require("http");
const https = require("https");

const PYTHON_AI_URL = process.env.PYTHON_AI_URL || "http://127.0.0.1:8000";
const DEFAULT_AVG_TIME_MS = 23000.0;

// Tắt keep-alive để tránh lỗi "socket hang up" khi Uvicorn/FastAPI đóng socket nhàn rỗi
const xaiClient = axios.create({
  baseURL: PYTHON_AI_URL,
  timeout: 8000,
  headers: {
    Connection: "close",
  },
  httpAgent: new http.Agent({ keepAlive: false }),
  httpsAgent: new https.Agent({ keepAlive: false }),
});

async function postWithRetry(endpoint, payload, timeoutMs = 8000) {
  let lastError = null;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const response = await xaiClient.post(endpoint, payload, { timeout: timeoutMs });
      return response.data;
    } catch (error) {
      lastError = error;
      const msg = (error.message || "").toLowerCase();
      const isTransientSocket =
        msg.includes("socket hang up") ||
        error.code === "ECONNRESET" ||
        error.code === "EPIPE";
      if (!isTransientSocket || attempt === 1) {
        break;
      }
      await new Promise((resolve) => setTimeout(resolve, 150));
    }
  }
  console.error(`Lỗi gọi Python API (${endpoint}):`, lastError?.message || "Unknown error");
  return null;
}

async function getWeaknessPrediction(studentData) {
  const safeAvgTime =
    studentData.avgTime && studentData.avgTime > 0
      ? studentData.avgTime
      : DEFAULT_AVG_TIME_MS;
  const safeElapsed =
    studentData.elapsedTime && studentData.elapsedTime > 0
      ? studentData.elapsedTime
      : safeAvgTime;

  return postWithRetry("/predict-weakness", {
    user_history_acc: studentData.historyAcc ?? 0.5,
    user_questions_attempted: studentData.totalQuestions ?? 0,
    question_difficulty: studentData.questionDifficulty ?? 0.5,
    user_avg_elapsed_time: safeAvgTime,
    rolling_acc_5: studentData.rollingAcc5 ?? 0.5,
    time_diff: Math.max(0, studentData.timeDiff ?? 0.0),
    elapsed_time: safeElapsed,
  });
}

async function getPreQuizPrediction(studentData) {
  const safeAvgTime =
    studentData.avgTime && studentData.avgTime > 0
      ? studentData.avgTime
      : DEFAULT_AVG_TIME_MS;

  return postWithRetry("/predict-prequiz", {
    user_history_acc: studentData.historyAcc ?? 0.5,
    user_questions_attempted: studentData.totalQuestions ?? 0,
    question_difficulty: studentData.questionDifficulty ?? 0.5,
    user_avg_elapsed_time: safeAvgTime,
    rolling_acc_5: studentData.rollingAcc5 ?? 0.5,
    time_diff: Math.max(0, studentData.timeDiff ?? 0.0),
  });
}

async function getForgettingPrediction(cardData) {
  return postWithRetry("/predict-forgetting", {
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
}

async function getGlobalXai() {
  try {
    const response = await xaiClient.get("/global-xai", { timeout: 5000 });
    return response.data;
  } catch (error) {
    console.error("Lỗi gọi Python API (/global-xai):", error.message);
    return null;
  }
}

async function getBatchRiskAnalysis(students) {
  return postWithRetry("/batch-risk-analysis", { students }, 10000);
}

module.exports = {
  getWeaknessPrediction,
  getPreQuizPrediction,
  getForgettingPrediction,
  getGlobalXai,
  getBatchRiskAnalysis,
};
