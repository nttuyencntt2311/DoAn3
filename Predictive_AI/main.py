from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import xgboost as xgb
import shap
import pandas as pd
import numpy as np

app = FastAPI(title="EdNet XAI Prediction Service")

# 1. Load mô hình XGBoost và SHAP Explainer
MODEL_PATH = "ednet_xgboost_model.json"
# Bỏ hoàn toàn model = xgb.XGBClassifier()
booster = xgb.Booster()
explainer = None

try:
    # Tải trực tiếp vào Booster nguyên thủy
    booster.load_model(MODEL_PATH)
    explainer = shap.TreeExplainer(booster)
    print("✅ Đã load Model XGBoost và SHAP Explainer thành công!", flush=True)
except Exception as e:
    raise RuntimeError(f"❌ Lỗi load model hoặc khởi tạo SHAP: {e}")

# 2. Định nghĩa cấu trúc Dữ liệu gửi từ Node.js sang
class PredictRequest(BaseModel):
    user_history_acc: float       
    user_questions_attempted: int 
    question_difficulty: float    
    user_avg_elapsed_time: float = 0.0 

# Dictionary dịch tên đặc trưng sang tiếng Việt
FEATURE_NAMES_VI = {
    'user_history_acc': 'Tỉ lệ trả lời đúng trong quá khứ',
    'user_questions_attempted': 'Số lượng câu hỏi đã luyện tập',
    'question_difficulty': 'Độ dễ trung bình của câu hỏi',
    'user_avg_elapsed_time': 'Thời gian suy nghĩ trung bình'
}

@app.get("/")
def home():
    return {"status": "AI Service đang chạy bình thường!"}

# 3. Endpoint nhận request từ Backend Node.js
@app.post("/predict-weakness")
def predict_weakness(data: PredictRequest):
    if booster is None or explainer is None:
        raise HTTPException(status_code=500, detail="Model hoặc Explainer chưa được khởi tạo thành công.")

    try:
        input_dict = {
            'user_history_acc': [data.user_history_acc],
            'user_questions_attempted': [data.user_questions_attempted],
            'question_difficulty': [data.question_difficulty]
        }
        
        if 'user_avg_elapsed_time' in booster.feature_names:
            input_dict['user_avg_elapsed_time'] = [data.user_avg_elapsed_time]

        input_df = pd.DataFrame(input_dict)

        # A. Dự đoán xác suất bằng Booster gốc và DMatrix
        dmatrix = xgb.DMatrix(input_df)
        prob_correct = booster.predict(dmatrix)[0]
        predicted_accuracy = round(float(prob_correct) * 100, 1)

        # B. Dùng SHAP tính điểm giải thích (XAI)
        shap_values = explainer.shap_values(input_df)[0]
        feature_names = input_df.columns

        explanations = []
        for name, val, score in zip(feature_names, input_df.iloc[0], shap_values):
            explanations.append({
                "feature": name,
                "vi_description": FEATURE_NAMES_VI.get(name, name),
                "value": float(val),
                "shap_score": float(score)
            })

        explanations.sort(key=lambda x: abs(x['shap_score']), reverse=True)
        top_reasons = explanations[:2]

        negative_factors = [
            f"{item['vi_description']} (Mức: {item['value']})" 
            for item in top_reasons if item['shap_score'] < 0
        ]
        
        positive_factors = [
            f"{item['vi_description']} (Mức: {item['value']})" 
            for item in top_reasons if item['shap_score'] >= 0
        ]

        # C. Trả kết quả
        return {
            "predicted_accuracy": predicted_accuracy,
            "is_struggling": predicted_accuracy < 50.0,
            "xai_explanation": {
                "negative_factors": negative_factors,
                "positive_factors": positive_factors
            }
        }

    except Exception as e:
        # Lỗi bên trong API thì trả về HTTP 500
        raise HTTPException(status_code=500, detail=str(e))