# LexiVT — Hệ thống Phân tích Hành vi Người học và Gợi ý Lộ trình Học (EdTech XAI)

> **Đồ án 4 (Nâng cấp từ Đồ án 3)** — Hệ sinh thái học tiếng Anh cá nhân hóa kết hợp **Predictive AI (XGBoost)**, **Explainable AI (SHAP)** và **Generative AI (Google Gemini / Groq Llama 3)** trên nền tảng Mobile Android, Web Admin và Microservice AI.

---

## 1. Giới thiệu & Quá trình phát triển (Đồ án 3 → Đồ án 4)

**LexiVT** khởi nguồn từ **Đồ án 3** (ứng dụng di động học từ vựng và luyện trắc nghiệm tiếng Anh TOEIC/IELTS tích hợp Gamification và Chatbot AI). Trong **Đồ án 4**, hệ thống được nâng cấp toàn diện thành nền tảng **EdTech XAI** có khả năng phân tích hành vi làm bài theo thời gian thực, dự đoán xác suất làm đúng/nguy cơ quên kiến thức và giải thích minh bạch lý do bằng **SHAP (SHapley Additive exPlanations)**.

| Hạng mục | Đồ án 3 (Phiên bản nền tảng) | Đồ án 4 (EdTech XAI — Hiện tại) |
| :--- | :--- | :--- |
| **Lõi học tập** | Flashcard từ vựng 5 chủ đề TOEIC, Quiz, Streak, XP, Bảng xếp hạng | Kế thừa toàn bộ + đồng bộ tiến độ cá nhân hóa theo từng học viên |
| **Trí tuệ nhân tạo** | Chatbot gia sư & sinh Quiz/Roadmap bằng Gemini | Bổ sung **Microservice Predictive AI (XGBoost)** huấn luyện trên **EdNet KT1** |
| **Khả năng giải thích (XAI)** | Chưa có (hộp đen) | Tích hợp **SHAP TreeExplainer**, Bệnh án học tập (Post-Mortem), Radar thời gian thực, Thử thách XAI (x3 XP), Sổ tay Tiên tri |
| **Kiến trúc triển khai** | Android + Node.js/MongoDB + React Admin | Mở rộng thêm **Python FastAPI Microservice** đóng gói bằng **Docker** |

---

## 2. Kiến trúc hệ thống

Hệ thống được thiết kế theo mô hình **4 thành phần tách biệt (Microservice-oriented)**:

1. **Mobile App (Android — Kotlin & XML)**: Giao diện học viên (`com.example.doan3`), quản lý học từ vựng (TTS phát âm), làm bài Quiz có Radar XAI thời gian thực, xem lộ trình cá nhân hóa, Sổ tay Tiên tri, Bệnh án học tập và trò chuyện với Trợ lý AI.
2. **Backend API Gateway (Node.js — Express & MongoDB)**: Xử lý xác thực JWT (Access/Refresh Token), quản lý dữ liệu người dùng, từ vựng, lịch sử học tập, Gamification (XP, Level, Streak), điều phối gọi **Gemini / Groq** và chuyển tiếp dữ liệu hành vi sang dịch vụ XAI.
3. **Predictive AI & XAI Microservice (Python — FastAPI, XGBoost, SHAP)**: Đóng gói Docker, nạp mô hình `ednet_xgboost_model_final.json`, nhận 7 đặc trưng hành vi học tập để dự đoán xác suất trả lời đúng (`predicted_accuracy`) và tính điểm đóng góp SHAP (`detailed_shap`, `positive_factors`, `negative_factors`).
4. **Admin Web Dashboard (React, Vite, Tailwind CSS)**: Cổng quản trị cho phép theo dõi tổng quan hệ thống, quản lý kho từ vựng theo 5 chủ đề TOEIC và quản lý tài khoản học viên.

**Luồng xử lý XAI tiêu biểu (khi học viên nộp bài Quiz):**
`Android App (Thu thập điểm, từ sai, thời gian làm bài)` → `POST /api/quiz/submit (Node.js)` → `Lưu MongoDB (History, User, vocabProgress)` → `POST /predict-weakness (FastAPI Docker)` → `XGBoost + SHAP TreeExplainer` → `Trả Bệnh án học tập XAI về Android BottomSheet`.

---

## 3. Tính năng chính

### Phân hệ Học viên (Android App)
- **Xác thực & Hồ sơ cá nhân**: Đăng ký, đăng nhập JWT, đổi mật khẩu, quản lý thông tin cá nhân, bảng xếp hạng Top 10 XP.
- **Kho Từ vựng & Flashcard thông minh**: 500 từ vựng TOEIC chia theo 5 chủ đề (*Hợp đồng & Kinh doanh*, *Môi trường văn phòng*, *Hệ thống thông tin & Công nghệ*, *Kế toán & Tài chính*, *Dịch vụ & Khách hàng*), tích hợp Text-to-Speech, tra cứu tự động qua Dictionary API và chấm điểm thành thạo (`proficiency`).
- **Lộ trình học cá nhân hóa (AI Roadmap)**: Tạo lộ trình theo mục tiêu, trình độ, thời gian rảnh và kỳ vọng cá nhân; mở khóa dần các chặng học và nhận thưởng mục tiêu tuần.
- **Trợ lý AI & Tạo Quiz tùy chỉnh**: Chat gia sư tiếng Anh nhiều phiên (hỗ trợ Fallback tự động từ **Google Gemini** sang **Groq Llama 3** khi quá tải) và tạo bộ Quiz từ văn bản hoặc tệp tài liệu.
- **Bộ tính năng XAI (Trọng tâm Đồ án 4)**:
  - **Thử thách XAI (High-Risk Challenge)**: Dự đoán xác suất làm đúng trước từng câu hỏi; gắn cảnh báo đỏ và thưởng **x3 XP** cho các câu vượt ngưỡng khó.
  - **Radar Thời gian thực (Pacing Alert)**: Phát hiện học viên trả lời quá nhanh (< 2 giây) và có dấu hiệu chọn sai để cảnh báo và cho cơ hội suy nghĩ lại.
  - **Bệnh án Học tập (Post-Mortem XAI Report)**: Hiển thị mức độ thành thạo dự kiến, các nhân tố tích cực (ưu điểm) và nhân tố rủi ro (hạn chế) kèm danh sách từ vựng hay nhầm lẫn ngay sau khi nộp bài.
  - **Sổ tay Tiên tri (Predictive Flashcards)**: Tự động tổng hợp các từ vựng có nguy cơ quên cao ngay trên màn hình Trang chủ để học viên ôn tập kịp thời.

### Phân hệ Quản trị (React Admin)
- **Dashboard tổng quan**: Thống kê tổng số từ vựng, tổng số học viên và hoạt động đăng ký gần đây.
- **Quản lý Từ vựng**: Tìm kiếm, thêm mới, chỉnh sửa và xóa từ vựng theo 5 chủ đề TOEIC.
- **Quản lý Học viên**: Tìm kiếm, lọc theo trạng thái (`Active`/`Banned`), xem chi tiết XP/Streak/từ đã thuộc và khóa/mở khóa tài khoản.

---

## 4. Mô hình Machine Learning & Explainable AI (EdNet KT1)

Mô hình dự đoán hành vi học tập được huấn luyện trong notebook `kt1.ipynb` trên tập dữ liệu thực tế **EdNet KT1**:

- **Quy mô dữ liệu**: 1.000 học viên (`KT1-1000us.zip`), tổng cộng **1.427.687** lượt tương tác làm bài.
- **7 đặc trưng đầu vào (bản chốt đã loại bỏ biến đa cộng tuyến)**:

| Biến đặc trưng | Ý nghĩa (Tiếng Việt) | Tiền xử lý |
| :--- | :--- | :--- |
| `user_history_acc` | Tỉ lệ làm đúng trong quá khứ | `shift(1).expanding().mean()` |
| `user_questions_attempted` | Tổng số câu hỏi đã làm | `cumcount()` theo từng học viên |
| `question_difficulty` | Tỉ lệ làm đúng trung bình của câu hỏi | Trung bình `is_correct` theo câu hỏi |
| `rolling_acc_5` | Phong độ 5 câu gần nhất | `shift(1).rolling(5).mean()` |
| `elapsed_time` | Thời gian suy nghĩ bài hiện tại (ms) | Điền khuyết bằng giá trị trung bình |
| `user_avg_elapsed_time` | Tốc độ suy nghĩ trung bình (ms) | `shift(1).expanding().mean()` |
| `log_time_diff` | Mật độ học tập (khoảng cách giữa các phiên) | `np.log1p(max(0, time_diff))` |

- **Thuật toán & Siêu tham số (Bản chốt)**: `XGBClassifier` (`n_estimators=1200`, `learning_rate=0.02`, `max_depth=6`, `min_child_weight=4`, `subsample=0.85`, `colsample_bytree=0.8`, `gamma=0.5`, cân bằng lớp `scale_pos_weight`, `early_stopping_rounds=80`).
- **Kết quả đánh giá**:
  - Bản cơ sở (4 biến): **ROC-AUC = 0.7590** (`ednet_xgboost_model.json`)
  - Bản chốt tối ưu (7 biến): **ROC-AUC = 0.7736** (`ednet_xgboost_model_final.json`)
- **Giải thích mô hình**: Sử dụng `shap.TreeExplainer` để tính giá trị SHAP cục bộ cho từng lượt dự đoán, xếp hạng Top 3 đặc trưng tác động mạnh nhất và tách thành `positive_factors` (SHAP ≥ 0) và `negative_factors` (SHAP < 0).

---

## 5. Công nghệ sử dụng

| Thành phần | Công nghệ & Thư viện chính |
| :--- | :--- |
| **Mobile App** | Kotlin, Android SDK, ViewBinding, Retrofit2, OkHttp3, Gson, Coroutines, LiveData/ViewModel, TextToSpeech |
| **Backend API** | Node.js, Express.js, MongoDB (Mongoose), JWT (`jsonwebtoken`), `bcryptjs`, Axios, `@google/generative-ai`, `groq-sdk` |
| **AI / XAI Service** | Python 3.11, FastAPI, Uvicorn, XGBoost 2.0.3, SHAP, Pandas, NumPy, Scikit-learn, Docker |
| **Web Admin** | React, React Router DOM, Tailwind CSS, Lucide Icons |
| **Huấn luyện ML** | Google Colab, Jupyter Notebook, Matplotlib, Seaborn |

---

## 6. Cấu trúc thư mục dự án

```text
EdTech-XAI-LexiVT/
├── android-app/                 # Ứng dụng di động Android (Kotlin + XML Layouts)
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── home/                # Trang chủ, Sổ tay Tiên tri, Tạo Quiz
│   │   ├── roadmap/             # Lộ trình AI, IeltsQuizActivity (XAI Radar & Post-Mortem)
│   │   ├── vocabulary/          # Danh sách từ vựng, Flashcard, WordDetail
│   │   ├── ai/                  # ChatAIActivity & lịch sử hội thoại
│   │   ├── statistics/          # Biểu đồ thống kê 7 ngày & thành tích
│   │   ├── learning/            # Màn hình tổng quan học tập
│   │   └── profile/             # Đăng nhập, Đăng ký, Hồ sơ, ApiService, TokenAuthenticator
│   └── res/layout/              # 31 tệp giao diện XML
├── backend/                     # Node.js Express API Server
│   ├── server.js
│   ├── config/db.js
│   ├── middleware/authMiddleware.js
│   ├── models/                  # User, Roadmap, Vocabulary, History, ChatSession, ChatMessage
│   ├── routes/                  # auth, quiz, vocab, stats, ai, user, roadmap
│   ├── controllers/             # aiController, vocabController, roadmapController, userController, statsController
│   └── service/                 # aiService (Gemini/Groq), xaiService (FastAPI), streakService
├── admin-web/                   # Giao diện quản trị React + Tailwind
│   ├── main.jsx, App.jsx
│   ├── layouts/AdminLayout.jsx
│   └── pages/                   # Login, Dashboard, Vocabulary, Users
├── predictive-ai/               # Microservice dự đoán & XAI (Python FastAPI + Docker)
│   ├── main.py
│   ├── ednet_xgboost_model_final.json
│   ├── requirements.txt
│   └── Dockerfile
└── ml-training/                 # Dữ liệu huấn luyện & biểu đồ đánh giá EdNet KT1
    ├── kt1.ipynb
    └── charts/                  # Feature Importance, ROC Curve, Correlation Heatmap, Prediction Distribution
```

---

## 7. Danh sách API chính

### Backend Node.js (`/api`)
| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register`, `/api/auth/login` | Đăng ký, đăng nhập và cấp cặp Access/Refresh Token |
| `POST` | `/api/auth/refresh-token`, `/api/auth/logout` | Làm mới Access Token và đăng xuất |
| `GET` / `PUT` | `/api/auth/profile`, `/api/auth/stats` | Lấy/cập nhật hồ sơ và chỉ số Gamification |
| `GET` / `POST` | `/api/vocabularies`, `/api/vocabularies/seed` | Lấy danh sách từ vựng cá nhân hóa & khởi tạo 500 từ TOEIC |
| `PUT` | `/api/vocabularies/:id/proficiency` | Cập nhật điểm thành thạo Flashcard và đồng bộ số từ đã thuộc |
| `GET` | `/api/vocabularies/predict-forgetting` | Lấy danh sách từ vựng cho **Sổ tay Tiên tri** |
| `POST` | `/api/quiz/submit` | Nộp kết quả Quiz, lưu từ sai, tính Streak và trả **Bệnh án XAI** |
| `POST` / `GET` | `/api/ai/generate-roadmap`, `/api/ai/my-roadmap` | Sinh và lấy lộ trình học cá nhân hóa |
| `GET` | `/api/ai/generate-quiz` | Sinh bộ câu hỏi trắc nghiệm theo chặng kèm dự đoán rủi ro XAI |
| `POST` | `/api/ai/chat`, `/api/ai/session` | Chat gia sư AI có lưu lịch sử hội thoại |
| `POST` | `/api/ai/weakness`, `/api/ai/post-mortem` | Gọi phân tích điểm yếu và bệnh án học tập XAI |
| `GET` | `/api/stats/learning`, `/api/stats/dashboard` | Thống kê 7 ngày cho Mobile và tổng quan cho Admin |
| `GET` / `PUT` | `/api/users`, `/api/users/:id/status`, `/api/users/leaderboard` | Quản lý học viên và bảng xếp hạng Top 10 XP |

### Predictive AI Microservice (FastAPI — Cổng `8000`)
| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/` | Kiểm tra trạng thái hoạt động của dịch vụ AI |
| `POST` | `/predict-weakness` | Nhận 7 đặc trưng hành vi, trả về `predicted_accuracy`, `is_struggling` và `xai_explanation` (SHAP) |

---

## 8. Hướng dẫn cài đặt và khởi chạy

### Bước 1: Khởi chạy Predictive AI Microservice (Python / Docker)
```bash
cd predictive-ai
# Cách 1: Chạy bằng Docker
docker build -t lexivt-xai-service .
docker run -p 8000:8000 lexivt-xai-service

# Cách 2: Chạy trực tiếp bằng Python 3.11
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### Bước 2: Khởi chạy Backend Node.js
Tạo tệp `.env` trong thư mục `backend/`:
```env
PORT=8080
MONGODB_CONNECTIONSTRING_STRING=mongodb+srv://<user>:<password>@<cluster>/<db>
JWT_SECRET=<chuoi_bi_mat_jwt>
REFRESH_SECRET=<chuoi_bi_mat_refresh>
GEMINI_API_KEY=<google_gemini_api_key>
GROQ_API_KEY=<groq_api_key>
```
Sau đó chạy server:
```bash
cd backend
npm install
node server.js
```

### Bước 3: Khởi chạy Web Admin (React)
```bash
cd admin-web
npm install
npm run dev
```
*(Lưu ý: Đồng bộ cổng Backend trong các lời gọi `fetch` giữa Admin Web và `PORT` trong `.env`.)*

### Bước 4: Khởi chạy ứng dụng Android
1. Mở thư mục dự án Android trong **Android Studio**.
2. Kiểm tra `BASE_URL` trong `ui/profile/ApiService.kt` (mặc định `http://10.0.2.2:8080/` cho máy ảo Android Emulator).
3. Build và chạy trên Emulator hoặc thiết bị thật.

---

## 9. Nhóm thực hiện

- **Đề tài**: Hệ thống Phân tích Hành vi Người học và Gợi ý Lộ trình Học (EdTech XAI) — Đồ án 4
- **Thành viên thực hiện**: `[Họ và tên: Nguyễn Thái Tuyền — MSSV: CNTT2311002]` `[Họ và tên: Phan Lê Tú Văn - MSSV: CNTT2311008]`
