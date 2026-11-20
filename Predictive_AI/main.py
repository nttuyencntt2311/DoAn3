import os
import math
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import xgboost as xgb
import shap
import pandas as pd
import numpy as np

app = FastAPI(title="EdNet XAI Prediction Service (Bản Chốt Chuẩn Hóa)")

# ==========================================
# 1. LOAD MÔ HÌNH XGBOOST VÀ SHAP EXPLAINER
# ==========================================
MODEL_PATH = os.getenv("MODEL_PATH", "ednet_xgboost_model_final.json")
PREQUIZ_MODEL_PATH = os.getenv("PREQUIZ_MODEL_PATH", "ednet_xgboost_prequiz_model.json")

# Giá trị trung vị thời gian suy nghĩ trên EdNet KT1 (~23 giây) để tránh OOD khi thiếu dữ liệu
DEFAULT_ELAPSED_TIME_MS = 23000.0

booster = xgb.Booster()
explainer = None

prequiz_booster = None
prequiz_explainer = None

try:
    booster.load_model(MODEL_PATH)
    explainer = shap.TreeExplainer(booster)
    print("✅ Đã load Model XGBoost chính và SHAP thành công!", flush=True)
except Exception as e:
    raise RuntimeError(f"❌ Lỗi load model hoặc khởi tạo SHAP: {e}")

if os.path.exists(PREQUIZ_MODEL_PATH):
    try:
        prequiz_booster = xgb.Booster()
        prequiz_booster.load_model(PREQUIZ_MODEL_PATH)
        prequiz_explainer = shap.TreeExplainer(prequiz_booster)
        print("✅ Đã load Model Pre-Quiz (6 đặc trưng) thành công!", flush=True)
    except Exception as e:
        print(f"⚠️ Không thể load Pre-Quiz model, sẽ dùng fallback an toàn: {e}", flush=True)
        prequiz_booster = None
        prequiz_explainer = None

# ==========================================
# 2. SCHEMAS REQUEST
# ==========================================
class PredictRequest(BaseModel):
    user_history_acc: float = Field(0.5, ge=0.0, le=1.0)
    user_questions_attempted: int = Field(0, ge=0)
    question_difficulty: float = Field(0.5, ge=0.0, le=1.0)
    user_avg_elapsed_time: float = Field(DEFAULT_ELAPSED_TIME_MS, ge=0.0)
    rolling_acc_5: float = Field(0.5, ge=0.0, le=1.0)
    time_diff: float = Field(0.0, ge=0.0)
    elapsed_time: float = Field(DEFAULT_ELAPSED_TIME_MS, ge=0.0)


class PreQuizRequest(BaseModel):
    user_history_acc: float = Field(0.5, ge=0.0, le=1.0)
    user_questions_attempted: int = Field(0, ge=0)
    question_difficulty: float = Field(0.5, ge=0.0, le=1.0)
    user_avg_elapsed_time: float = Field(DEFAULT_ELAPSED_TIME_MS, ge=0.0)
    rolling_acc_5: float = Field(0.5, ge=0.0, le=1.0)
    time_diff: float = Field(0.0, ge=0.0)


class ForgettingRequest(BaseModel):
    user_history_acc: float = Field(0.5, ge=0.0, le=1.0)
    user_questions_attempted: int = Field(0, ge=0)
    proficiency: float = Field(0.0, ge=0.0, le=100.0)
    mistake_count: int = Field(0, ge=0)
    review_count: int = Field(1, ge=0)
    days_since_review: float = Field(0.0, ge=0.0)
    user_avg_elapsed_time: float = Field(DEFAULT_ELAPSED_TIME_MS, ge=0.0)


FEATURE_NAMES_VI = {
    "user_history_acc": "Tỉ lệ làm đúng lịch sử",
    "user_questions_attempted": "Tổng số câu hỏi đã luyện tập",
    "question_difficulty": "Độ khó câu hỏi (0: Dễ → 1: Khó)",
    "user_avg_elapsed_time": "Tốc độ suy nghĩ trung bình (ms)",
    "rolling_acc_5": "Phong độ 5 câu gần nhất",
    "log_time_diff": "Khoảng cách giữa các phiên học",
    "elapsed_time": "Thời gian suy nghĩ bài này (ms)",
}


def _safe_base_value(exp_obj) -> float:
    val = getattr(exp_obj, "expected_value", 0.0)
    if isinstance(val, (list, np.ndarray)):
        return float(val[0])
    return float(val)


def _format_shap_response(active_booster, active_explainer, input_df: pd.DataFrame):
    dmatrix = xgb.DMatrix(input_df)
    prob_correct = float(active_booster.predict(dmatrix)[0])
    predicted_accuracy = round(prob_correct * 100.0, 1)

    shap_values = active_explainer.shap_values(input_df)[0]
    base_val = round(_safe_base_value(active_explainer), 4)

    explanations = []
    for name, val, score in zip(input_df.columns, input_df.iloc[0], shap_values):
        explanations.append({
            "feature": name,
            "vi_description": FEATURE_NAMES_VI.get(name, name),
            "value": round(float(val), 2),
            "shap_score": round(float(score), 4),
        })

    explanations.sort(key=lambda x: abs(x["shap_score"]), reverse=True)
    top_reasons = explanations[:3]

    negative_factors = [
        f"{item['vi_description']} (SHAP: {item['shap_score']:+.2f} — Ảnh hưởng {'Cao' if abs(item['shap_score']) > 0.35 else 'Vừa'})"
        for item in top_reasons if item["shap_score"] < 0
    ]
    positive_factors = [
        f"{item['vi_description']} (SHAP: {item['shap_score']:+.2f} — Ảnh hưởng {'Cao' if abs(item['shap_score']) > 0.35 else 'Vừa'})"
        for item in top_reasons if item["shap_score"] >= 0
    ]

    return {
        "predicted_accuracy": predicted_accuracy,
        "is_struggling": predicted_accuracy < 50.0,
        "xai_explanation": {
            "base_value": base_val,
            "negative_factors": negative_factors,
            "positive_factors": positive_factors,
            "detailed_shap": explanations,
        },
    }


@app.get("/")
def home():
    return {
        "status": "AI Service (Bản Chốt Chuẩn Hóa) đang chạy bình thường!",
        "has_prequiz_model": prequiz_booster is not None,
    }


# ==========================================
# 3. ENDPOINT DỰ ĐOÁN & GIẢI THÍCH SAU LÀM BÀI (7 BIẾN)
# ==========================================
@app.post("/predict-weakness")
def predict_weakness(data: PredictRequest):
    if booster is None or explainer is None:
        raise HTTPException(status_code=500, detail="Model hoặc Explainer chưa được khởi tạo.")

    try:
        # Bảo vệ OOD: nếu client truyền 0 ms -> thay bằng trung vị EdNet để không lệch phân phối
        safe_avg_time = data.user_avg_elapsed_time if data.user_avg_elapsed_time > 0 else DEFAULT_ELAPSED_TIME_MS
        safe_elapsed = data.elapsed_time if data.elapsed_time > 0 else safe_avg_time
        log_time_diff_val = float(np.log1p(max(0.0, data.time_diff)))

        input_dict = {
            "user_history_acc": [data.user_history_acc],
            "user_questions_attempted": [data.user_questions_attempted],
            "question_difficulty": [data.question_difficulty],
            "rolling_acc_5": [data.rolling_acc_5],
            "log_time_diff": [log_time_diff_val],
            "elapsed_time": [safe_elapsed],
            "user_avg_elapsed_time": [safe_avg_time],
        }

        input_df = pd.DataFrame(input_dict)[booster.feature_names]
        result = _format_shap_response(booster, explainer, input_df)
        result["counterfactual"] = _generate_counterfactual(booster, input_df, result["predicted_accuracy"] / 100.0)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")


# ==========================================
# 4. ENDPOINT DỰ ĐOÁN TRƯỚC KHI LÀM BÀI (PRE-QUIZ — KHÔNG OOD)
# ==========================================
@app.post("/predict-prequiz")
def predict_prequiz(data: PreQuizRequest):
    try:
        safe_avg_time = data.user_avg_elapsed_time if data.user_avg_elapsed_time > 0 else DEFAULT_ELAPSED_TIME_MS
        log_time_diff_val = float(np.log1p(max(0.0, data.time_diff)))

        if prequiz_booster is not None and prequiz_explainer is not None:
            input_dict = {
                "user_history_acc": [data.user_history_acc],
                "user_questions_attempted": [data.user_questions_attempted],
                "question_difficulty": [data.question_difficulty],
                "rolling_acc_5": [data.rolling_acc_5],
                "user_avg_elapsed_time": [safe_avg_time],
                "log_time_diff": [log_time_diff_val],
            }
            input_df = pd.DataFrame(input_dict)[prequiz_booster.feature_names]
            return _format_shap_response(prequiz_booster, prequiz_explainer, input_df)

        # Fallback an toàn nếu chưa retrain file prequiz_model: dùng safe_avg_time thay vì 0
        input_dict = {
            "user_history_acc": [data.user_history_acc],
            "user_questions_attempted": [data.user_questions_attempted],
            "question_difficulty": [data.question_difficulty],
            "rolling_acc_5": [data.rolling_acc_5],
            "log_time_diff": [log_time_diff_val],
            "elapsed_time": [safe_avg_time],
            "user_avg_elapsed_time": [safe_avg_time],
        }
        input_df = pd.DataFrame(input_dict)[booster.feature_names]
        return _format_shap_response(booster, explainer, input_df)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi Pre-Quiz XAI: {str(e)}")


# ==========================================
# 5. ENDPOINT SỔ TAY TIÊN TRI (XGBOOST + SHAP + EBBINGHAUS DECAY)
# ==========================================
@app.post("/predict-forgetting")
def predict_forgetting(data: ForgettingRequest):
    try:
        safe_avg_time = data.user_avg_elapsed_time if data.user_avg_elapsed_time > 0 else DEFAULT_ELAPSED_TIME_MS
        time_diff_sec = max(0.0, data.days_since_review * 86400.0)
        log_time_diff_val = float(np.log1p(time_diff_sec))

        # Ước lượng độ khó cá nhân của từ dựa trên số lần sai và độ thành thạo
        word_difficulty = min(0.95, max(0.15, (1.0 - data.proficiency / 100.0) * 0.7 + min(data.mistake_count, 5) * 0.06))
        rolling_est = max(0.05, min(0.95, data.proficiency / 100.0))

        input_dict = {
            "user_history_acc": [data.user_history_acc],
            "user_questions_attempted": [data.user_questions_attempted],
            "question_difficulty": [word_difficulty],
            "rolling_acc_5": [rolling_est],
            "log_time_diff": [log_time_diff_val],
            "elapsed_time": [safe_avg_time],
            "user_avg_elapsed_time": [safe_avg_time],
        }
        input_df = pd.DataFrame(input_dict)[booster.feature_names]
        shap_res = _format_shap_response(booster, explainer, input_df)

        # Kết hợp thêm đường cong quên Ebbinghaus R = exp(-t / S)
        # Độ bền trí nhớ S (ngày) tăng theo review_count & proficiency, giảm theo mistake_count
        stability_days = max(0.8, (1.5 + data.review_count * 1.2 + (data.proficiency / 25.0)) / (1.0 + data.mistake_count * 0.5))
        ebbinghaus_retention = math.exp(-data.days_since_review / stability_days)

        # Tổng hợp xác suất nhớ (60% XGBoost EdNet + 40% Ebbinghaus Spaced Repetition)
        xgb_prob = shap_res["predicted_accuracy"] / 100.0
        combined_retention = max(0.05, min(0.98, 0.6 * xgb_prob + 0.4 * ebbinghaus_retention))
        forgetting_risk = round((1.0 - combined_retention) * 100.0, 1)

        reasons = []
        if data.mistake_count > 0:
            reasons.append(f"từng nhầm {data.mistake_count} lần")
        if data.days_since_review >= 1.0:
            reasons.append(f"đã {data.days_since_review:.1f} ngày chưa ôn")
        if data.proficiency < 50:
            reasons.append(f"độ thành thạo thấp ({int(data.proficiency)}%)")
        if not reasons:
            reasons.append("cần củng cố để giữ trí nhớ dài hạn")

        shap_res["predicted_accuracy"] = round(combined_retention * 100.0, 1)
        shap_res["forgetting_risk_percent"] = forgetting_risk
        shap_res["is_struggling"] = combined_retention < 0.65
        shap_res["xai_reason"] = f"Nguy cơ quên {forgetting_risk}% vì " + " và ".join(reasons) + "."
        return shap_res
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi Sổ tay Tiên tri XAI: {str(e)}")


# ==========================================
# 6. GIẢI THÍCH PHẢN THỰC (COUNTERFACTUAL "WHAT-IF")
# ==========================================
def _generate_counterfactual(active_booster, base_df: pd.DataFrame, current_prob: float):
    """
    Tìm kiếm thay đổi hành vi tối thiểu mà học viên có thể kiểm soát
    (tốc độ suy nghĩ, phong độ 5 câu gần nhất, đều đặn ôn tập) để nâng xác suất làm đúng.
    """
    row = base_df.iloc[0].to_dict()
    best_prob = current_prob
    best_changes = []

    # Lưới các kịch bản hành vi khả thi (actionable grid)
    elapsed_candidates = [
        ("giữ nguyên tốc độ", float(row["elapsed_time"]), float(row["user_avg_elapsed_time"])),
        ("dành thêm ~6 giây suy nghĩ mỗi câu", min(65000.0, float(row["elapsed_time"]) + 6000.0), min(65000.0, float(row["user_avg_elapsed_time"]) + 4000.0)),
        ("dành thêm ~12 giây đọc kỹ đề", min(75000.0, float(row["elapsed_time"]) + 12000.0), min(75000.0, float(row["user_avg_elapsed_time"]) + 8000.0)),
    ]
    rolling_candidates = [
        float(row["rolling_acc_5"]),
        min(0.90, max(float(row["rolling_acc_5"]) + 0.2, 0.70)),
        min(0.95, max(float(row["rolling_acc_5"]) + 0.35, 0.85)),
    ]
    log_td_candidates = [
        float(row["log_time_diff"]),
        min(float(row["log_time_diff"]), float(np.log1p(3600.0))),  # Học đều hơn (cách nhau <= 1 giờ / hàng ngày)
    ]

    for elapsed_label, e_val, avg_e_val in elapsed_candidates:
        for r_val in rolling_candidates:
            for td_val in log_td_candidates:
                cand = dict(row)
                cand["elapsed_time"] = e_val
                cand["user_avg_elapsed_time"] = avg_e_val
                cand["rolling_acc_5"] = r_val
                cand["log_time_diff"] = td_val
                cand["user_history_acc"] = min(0.95, float(row["user_history_acc"]) + max(0.0, (r_val - float(row["rolling_acc_5"])) * 0.4))

                cand_df = pd.DataFrame([cand])[active_booster.feature_names]
                prob = float(active_booster.predict(xgb.DMatrix(cand_df))[0])

                changes = []
                if e_val > float(row["elapsed_time"]) + 1000:
                    changes.append({
                        "feature": "elapsed_time",
                        "action_vi": elapsed_label,
                        "from_value": round(float(row["elapsed_time"]) / 1000.0, 1),
                        "to_value": round(e_val / 1000.0, 1),
                        "unit": "giây",
                    })
                if r_val > float(row["rolling_acc_5"]) + 0.05:
                    changes.append({
                        "feature": "rolling_acc_5",
                        "action_vi": f"ôn lại từ sai để nâng phong độ 5 câu gần nhất từ {int(row['rolling_acc_5']*100)}% lên {int(r_val*100)}%",
                        "from_value": round(float(row["rolling_acc_5"]) * 100, 0),
                        "to_value": round(r_val * 100, 0),
                        "unit": "%",
                    })
                if td_val < float(row["log_time_diff"]) - 0.5:
                    changes.append({
                        "feature": "log_time_diff",
                        "action_vi": "duy trì học đều đặn hơn (tránh nghỉ ngắt quãng quá lâu)",
                        "from_value": round(float(row["log_time_diff"]), 2),
                        "to_value": round(td_val, 2),
                        "unit": "log(s)",
                    })

                if changes and prob > best_prob + 0.01:
                    best_prob = prob
                    best_changes = changes

    cur_pct = round(current_prob * 100.0, 1)
    tgt_pct = round(best_prob * 100.0, 1)
    delta_pct = round(tgt_pct - cur_pct, 1)

    if best_changes and delta_pct > 0:
        actions_text = " và ".join([c["action_vi"] for c in best_changes])
        summary_vi = (
            f"Nếu bạn {actions_text}, xác suất làm đúng dự kiến sẽ tăng từ "
            f"{cur_pct}% lên {tgt_pct}% (+{delta_pct}%)."
        )
    else:
        summary_vi = (
            f"Phong độ hiện tại đang ở mức tốt ({cur_pct}%). Hãy duy trì tốc độ suy nghĩ ổn định "
            f"và tiếp tục luyện tập nâng độ khó!"
        )

    return {
        "current_accuracy": cur_pct,
        "target_accuracy": tgt_pct,
        "delta_percent": delta_pct,
        "changes": best_changes,
        "summary_vi": summary_vi,
    }


@app.post("/counterfactual")
def counterfactual_endpoint(data: PredictRequest):
    if booster is None:
        raise HTTPException(status_code=500, detail="Model chưa được khởi tạo.")
    safe_avg_time = data.user_avg_elapsed_time if data.user_avg_elapsed_time > 0 else DEFAULT_ELAPSED_TIME_MS
    safe_elapsed = data.elapsed_time if data.elapsed_time > 0 else safe_avg_time
    log_time_diff_val = float(np.log1p(max(0.0, data.time_diff)))
    input_df = pd.DataFrame({
        "user_history_acc": [data.user_history_acc],
        "user_questions_attempted": [data.user_questions_attempted],
        "question_difficulty": [data.question_difficulty],
        "rolling_acc_5": [data.rolling_acc_5],
        "log_time_diff": [log_time_diff_val],
        "elapsed_time": [safe_elapsed],
        "user_avg_elapsed_time": [safe_avg_time],
    })[booster.feature_names]
    cur_prob = float(booster.predict(xgb.DMatrix(input_df))[0])
    return _generate_counterfactual(booster, input_df, cur_prob)


# ==========================================
# 7. GLOBAL XAI & BATCH RISK ANALYSIS (CHO ADMIN WEB DASHBOARD)
# ==========================================
@app.get("/global-xai")
def global_xai():
    if booster is None:
        raise HTTPException(status_code=500, detail="Model chưa được khởi tạo.")
    gain_scores = booster.get_score(importance_type="gain")
    weight_scores = booster.get_score(importance_type="weight")
    total_gain = sum(gain_scores.values()) or 1.0

    features_summary = []
    for feat in booster.feature_names:
        g = float(gain_scores.get(feat, 0.0))
        w = float(weight_scores.get(feat, 0.0))
        features_summary.append({
            "feature": feat,
            "vi_description": FEATURE_NAMES_VI.get(feat, feat),
            "importance_percent": round((g / total_gain) * 100.0, 1),
            "gain": round(g, 2),
            "weight": int(w),
        })

    features_summary.sort(key=lambda x: x["importance_percent"], reverse=True)
    return {
        "model_features_count": len(booster.feature_names),
        "global_importance": features_summary,
    }


class StudentBatchItem(BaseModel):
    user_id: str
    name: str
    email: str = ""
    user_history_acc: float = 0.5
    user_questions_attempted: int = 0
    question_difficulty: float = 0.5
    rolling_acc_5: float = 0.5
    user_avg_elapsed_time: float = DEFAULT_ELAPSED_TIME_MS
    time_diff: float = 0.0
    elapsed_time: float = DEFAULT_ELAPSED_TIME_MS


class BatchRiskRequest(BaseModel):
    students: List[StudentBatchItem]


@app.post("/batch-risk-analysis")
def batch_risk_analysis(payload: BatchRiskRequest):
    if booster is None or explainer is None:
        raise HTTPException(status_code=500, detail="Model chưa sẵn sàng.")
    if not payload.students:
        return {"students_analyzed": 0, "at_risk_count": 0, "avg_predicted_accuracy": 0.0, "students": []}

    rows = []
    for s in payload.students:
        safe_avg = s.user_avg_elapsed_time if s.user_avg_elapsed_time > 0 else DEFAULT_ELAPSED_TIME_MS
        safe_elapsed = s.elapsed_time if s.elapsed_time > 0 else safe_avg
        rows.append({
            "user_history_acc": max(0.0, min(1.0, s.user_history_acc)),
            "user_questions_attempted": max(0, s.user_questions_attempted),
            "question_difficulty": max(0.0, min(1.0, s.question_difficulty)),
            "rolling_acc_5": max(0.0, min(1.0, s.rolling_acc_5)),
            "log_time_diff": float(np.log1p(max(0.0, s.time_diff))),
            "elapsed_time": safe_elapsed,
            "user_avg_elapsed_time": safe_avg,
        })

    df_batch = pd.DataFrame(rows)[booster.feature_names]
    probs = booster.predict(xgb.DMatrix(df_batch))
    shap_matrix = explainer.shap_values(df_batch)

    results = []
    for idx, s in enumerate(payload.students):
        pred_acc = round(float(probs[idx]) * 100.0, 1)
        shaps = shap_matrix[idx]
        # Tìm yếu tố SHAP âm mạnh nhất (nguyên nhân rủi ro hàng đầu)
        feat_pairs = list(zip(df_batch.columns, shaps))
        neg_pairs = sorted([p for p in feat_pairs if p[1] < 0], key=lambda x: x[1])
        if neg_pairs:
            top_feat, top_score = neg_pairs[0]
            top_driver = f"{FEATURE_NAMES_VI.get(top_feat, top_feat)} (SHAP: {top_score:+.2f})"
        else:
            top_driver = "Không phát hiện yếu tố rủi ro đáng kể"

        risk_level = "Cao" if pred_acc < 45.0 else ("Trung bình" if pred_acc < 65.0 else "Thấp")
        results.append({
            "user_id": s.user_id,
            "name": s.name,
            "email": s.email,
            "predicted_accuracy": pred_acc,
            "risk_level": risk_level,
            "top_risk_driver": top_driver,
        })

    results.sort(key=lambda x: x["predicted_accuracy"])
    at_risk_count = sum(1 for r in results if r["risk_level"] == "Cao")
    avg_acc = round(sum(r["predicted_accuracy"] for r in results) / len(results), 1)

    return {
        "students_analyzed": len(results),
        "at_risk_count": at_risk_count,
        "avg_predicted_accuracy": avg_acc,
        "students": results,
    }
