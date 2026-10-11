from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import xgboost as xgb
import shap
import pandas as pd
import numpy as np

app = FastAPI(title="EdNet XAI Prediction Service (Bản Chốt)")

# ==========================================
# 1. LOAD MÔ HÌNH XGBOOST VÀ SHAP EXPLAINER
# ==========================================
MODEL_PATH = "ednet_xgboost_model_final.json" # Đổi tên file theo bản chốt
booster = xgb.Booster()
explainer = None

try:
    booster.load_model(MODEL_PATH)
    explainer = shap.TreeExplainer(booster)
    print("✅ Đã load Model XGBoost (Bản Chốt) và SHAP thành công!", flush=True)
except Exception as e:
    raise RuntimeError(f"❌ Lỗi load model hoặc khởi tạo SHAP: {e}")

# ==========================================
# 2. ĐỊNH NGHĨA DỮ LIỆU TỪ FRONTEND/NODE.JS
# ==========================================
class PredictRequest(BaseModel):
    # Các biến chốt
    user_history_acc: float       
    user_questions_attempted: int 
    question_difficulty: float    
    user_avg_elapsed_time: float = 0.0 
    rolling_acc_5: float = 0.5         
    time_diff: float = 0.0             
    elapsed_time: float = 0.0          
    # Đã xóa elapsed_ratio và các biến gây nhiễu khác

# Dictionary dịch tên đặc trưng sang tiếng Việt cho SHAP
FEATURE_NAMES_VI = {
    'user_history_acc': 'Tỉ lệ làm đúng trong quá khứ',
    'user_questions_attempted': 'Tổng số câu hỏi đã làm',
    'question_difficulty': 'Độ khó trung bình của câu hỏi',
    'user_avg_elapsed_time': 'Tốc độ suy nghĩ trung bình (ms)',
    'rolling_acc_5': 'Phong độ 5 câu gần nhất',
    'log_time_diff': 'Mật độ học tập (Khoảng cách giữa các phiên)', # Đổi tên thành log_time_diff
    'elapsed_time': 'Thời gian suy nghĩ bài này (ms)'
}

@app.get("/")
def home():
    return {"status": "AI Service (Bản Chốt) đang chạy bình thường!"}

# ==========================================
# 3. ENDPOINT DỰ ĐOÁN & GIẢI THÍCH LÝ DO
# ==========================================
@app.post("/predict-weakness")
def predict_weakness(data: PredictRequest):
    if booster is None or explainer is None:
        raise HTTPException(status_code=500, detail="Model hoặc Explainer chưa được khởi tạo thành công.")

    try:
        model_features = booster.feature_names
        
        # Tiền xử lý time_diff sang log_time_diff giống y chang lúc train
        log_time_diff_val = np.log1p(max(0, data.time_diff))

        input_dict = {
            'user_history_acc': [data.user_history_acc],
            'user_questions_attempted': [data.user_questions_attempted],
            'question_difficulty': [data.question_difficulty],
            'rolling_acc_5': [data.rolling_acc_5],
            'log_time_diff': [log_time_diff_val], # Truyền biến đã log
            'elapsed_time': [data.elapsed_time],
            'user_avg_elapsed_time': [data.user_avg_elapsed_time]
        }
        
        input_df = pd.DataFrame(input_dict)
        input_df = input_df[model_features] # Đảm bảo đúng thứ tự

        # --- A. DỰ ĐOÁN ---
        dmatrix = xgb.DMatrix(input_df)
        prob_correct = booster.predict(dmatrix)[0]
        predicted_accuracy = round(float(prob_correct) * 100, 1)

        # --- B. SHAP ---
        shap_values = explainer.shap_values(input_df)[0]
        feature_names = input_df.columns

        explanations = []
        for name, val, score in zip(feature_names, input_df.iloc[0], shap_values):
            explanations.append({
                "feature": name,
                "vi_description": FEATURE_NAMES_VI.get(name, name),
                "value": round(float(val), 2),
                "shap_score": float(score)
            })

        explanations.sort(key=lambda x: abs(x['shap_score']), reverse=True)
        top_reasons = explanations[:3]

        negative_factors = [
            f"{item['vi_description']} (Mức ảnh hưởng: {'Cao' if abs(item['shap_score']) > 0.5 else 'Vừa'})" 
            for item in top_reasons if item['shap_score'] < 0
        ]
        
        positive_factors = [
            f"{item['vi_description']} (Mức ảnh hưởng: {'Cao' if abs(item['shap_score']) > 0.5 else 'Vừa'})" 
            for item in top_reasons if item['shap_score'] >= 0
        ]

        return {
            "predicted_accuracy": predicted_accuracy,
            "is_struggling": predicted_accuracy < 50.0,
            "xai_explanation": {
                "negative_factors": negative_factors,
                "positive_factors": positive_factors,
                "detailed_shap": explanations 
            }
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")