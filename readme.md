# LexiVT — Hệ thống Phân tích Hành vi Người học và Gợi ý Lộ trình Học (EdTech XAI)

> **Đồ án 4 (Nâng cấp từ Đồ án 3)** — Hệ sinh thái học tiếng Anh cá nhân hóa kết hợp **Predictive AI (XGBoost)**, **Explainable AI (SHAP & Counterfactual What-If)** và **Generative AI (Google Gemini 2.5 Flash / Groq Llama 3.1)** trên 4 phân hệ: **App Android**, **Backend API**, **Web Admin** và **Predictive AI Microservice**.

---

## 1. Giới thiệu & Quá trình phát triển (Đồ án 3 → Đồ án 4)

**LexiVT** khởi nguồn từ **Đồ án 3** (ứng dụng di động học từ vựng và luyện trắc nghiệm tiếng Anh TOEIC/IELTS tích hợp Gamification và Chatbot AI). Trong **Đồ án 4**, hệ thống được nâng cấp toàn diện thành nền tảng **EdTech XAI** có khả năng ghi nhận hành vi chi tiết từng câu hỏi, dự đoán xác suất làm đúng/nguy cơ quên kiến thức bằng **XGBoost** không rò rỉ dữ liệu, và giải thích minh bạch lý do bằng **SHAP (SHapley Additive exPlanations)** kết hợp **Counterfactual Explanations ("What-If")**.

| Hạng mục | Đồ án 3 (Phiên bản nền tảng) | Đồ án 4 (EdTech XAI — Hiện tại) |
| :--- | :--- | :--- |
| **Lõi học tập** | Flashcard 5 chủ đề TOEIC, Quiz, Streak, XP, Bảng xếp hạng | Kế thừa toàn bộ + Onboarding 3 bước, Mục tiêu ngày/tuần, Luyện nghe Dictation, Chấm phát âm giọng nói, Thống kê theo Tuần/Tháng |
| **Kiến trúc AI** | Chatbot & sinh Quiz/Roadmap bằng Gemini | Tách bạch **2 cụm AI**: **Generative AI** (Gemini 2.5 Flash / Groq Llama 3.1) & **Predictive XAI** (FastAPI + 2 mô hình XGBoost chuẩn hóa trên EdNet KT1) |
| **Khả năng giải thích (XAI)** | Chưa có (hộp đen) | **SHAP TreeExplainer**, Bệnh án học tập (Nhận xét gia sư + What-If + biểu đồ SHAP), Radar < 2 giây, Thử thách x3 XP, Sổ tay Tiên tri (XGBoost + Ebbinghaus), Chat AI tích hợp hồ sơ XAI |
| **Quản trị & Triển khai** | React Admin cơ bản + Node.js/MongoDB | Bổ sung **Global XAI Dashboard (`/xai-analytics`)** phát hiện học viên rủi ro sớm (At-Risk) + xuất CSV, tự động làm mới token Admin, đóng gói **Docker** cho Predictive AI |

---

## 2. Kiến trúc hệ thống

Hệ thống được thiết kế theo mô hình **4 phân hệ tách biệt (Microservice-oriented)**:

1. **App Android (`App_Android/` — Kotlin & XML ViewBinding)**: Ứng dụng học viên (`com.example.doan3`) hỗ trợ học từ vựng, Flashcard (đọc chậm 0.7x), chấm phát âm (`SpeechRecognizer`), Luyện nghe Dictation, Quiz có Radar XAI & Báo cáo năng lực, Lộ trình AI 4 chặng, Sổ tay Tiên tri, Thống kê & Kỷ niệm, và Gia sư Chat AI.
2. **Backend API Gateway (`Backend_API/` — Node.js, Express, MongoDB, cổng `8080`)**: Xử lý xác thực JWT (Access/Refresh Token), phân quyền Admin (`requireAdmin`), chống IDOR, quản lý dữ liệu người dùng/từ vựng/lịch sử hành vi từng câu (`questionLogs`), điều phối cụm **Generative AI** (`aiService.js`) và kết nối cụm **Predictive XAI** (`xaiService.js` có retry và chống `socket hang up`).
3. **Predictive AI & XAI Microservice (`Predictive_AI/` — Python 3.11, FastAPI, XGBoost, SHAP, Docker, cổng `8000`)**: Nạp 2 mô hình XGBoost chuyên biệt (**Post-Quiz 7 biến** và **Pre-Quiz 6 biến**), cung cấp API dự đoán xác suất đúng, giải thích SHAP cục bộ/toàn cục, gợi ý phản thực What-If, đường cong quên Ebbinghaus và phân tích rủi ro hàng loạt cho Admin.
4. **Web Admin Dashboard (`Web_Admin/lexivt-admin/` — React, Vite, Tailwind CSS)**: Cổng quản trị tích hợp tự động refresh token (`adminApi.js`), thống kê tổng quan, CRUD kho từ vựng, quản lý người dùng và **Global XAI Analytics (`/xai-analytics`)**.

**Luồng xử lý XAI tiêu biểu (khi học viên nộp bài Quiz/Dictation):**
`Android App (Thu thập điểm, từ sai, log thời gian từng câu)` → `POST /api/quiz/submit (Node.js)` → `Lưu MongoDB (History, User, vocabProgress) & tự tra nghĩa + IPA cho từ sai` → `POST /predict-weakness (FastAPI Docker)` → `XGBoost + SHAP + Counterfactual What-If` → `Gemini SHAP-to-Text (Nhận xét gia sư có kiểm soát)` → `Hiển thị Báo cáo XAI & Tổng kết trên Android`.

---

## 3. Tính năng chính

### Phân hệ Học viên (`App_Android`)
- **Tài khoản, Onboarding & Cài đặt**: Đăng ký, đăng nhập JWT, quên mật khẩu OTP 6 số, đăng nhập Google, Onboarding 3 bước chọn mục tiêu ngày, đổi mật khẩu, đổi avatar, chuyển ngôn ngữ (`AppCompatDelegate`), nhắc học hằng ngày lúc 20:00 (`AlarmManager` + `BootCompletedReceiver` tự khôi phục sau khi khởi động lại máy).
- **Trang chủ & Gamification**: Thẻ **Mục tiêu hôm nay** (tự cộng phút học sau Quiz/Dictation, chạm để chỉnh 5–30 phút/ngày), chuỗi Streak 7 ngày (`T2–CN`), XP/Level lũy tiến, Bảng xếp hạng Top 10 XP, tab **Kỷ niệm** (huy hiệu & dòng thời gian thành tích) và biểu đồ **Thống kê** hỗ trợ lọc **Tuần này / Tuần trước / Tháng này**.
- **Kho Từ vựng & Kỹ năng toàn diện**: 500 từ vựng TOEIC theo 5 chủ đề (*Hợp đồng & Kinh doanh*, *Môi trường văn phòng*, *Hệ thống thông tin & Công nghệ*, *Kế toán & Tài chính*, *Dịch vụ & Khách hàng*), thêm từ vựng cá nhân đưa thẳng vào vòng lặp XAI, Flashcard (đọc thường 1.0x & đọc chậm 0.7x), tra từ điển & **chấm phát âm bằng giọng nói** (`SpeechRecognizer`), **Luyện nghe chép chính tả (Dictation)** đồng bộ XP/XAI, và **Tạo Quiz AI** từ văn bản hoặc tệp `.txt`.
- **Lộ trình học AI cá nhân hóa (`RoadmapFragment`)**: Tạo lộ trình 4 chặng bám sát điểm yếu XAI, lưu & chuyển đổi giữa nhiều lộ trình, hiển thị **Mục tiêu tuần** theo dữ liệu thật từ MongoDB + nhận thưởng XP, tự động mở khóa chặng tiếp theo khi đạt ≥ 50% (kèm nút ôn lại chặng đã hoàn thành và hộp thoại xác nhận khi thoát bài).
- **Bộ tính năng Explainable AI (Trọng tâm Đồ án 4)**:
  - **Thử thách XAI (x3 XP)**: Dự đoán xác suất làm đúng trước khi làm bài bằng mô hình **Pre-Quiz 6 biến**, gắn cảnh báo và thưởng x3 XP cho câu khó.
  - **Radar Thời gian thực (< 2 giây)**: Phát hiện học viên bấm chọn quá vội và có nguy cơ sai để nhắc suy nghĩ lại.
  - **Bệnh án Học tập (Post-Mortem XAI)**: Thiết kế 2 tầng — đưa **Nhận xét gia sư AI** & **Gợi ý hành động What-If** lên đầu bằng ngôn ngữ tự nhiên cho người học, đồng thời giữ nguyên **Biểu đồ đóng góp SHAP kỹ thuật** trong mục thu gọn để phục vụ tra cứu/bảo vệ.
  - **Sổ tay Tiên tri (Predictive Flashcards)**: Kết hợp mô hình **XGBoost Pre-Quiz + Đường cong quên Ebbinghaus** xếp hạng từ sắp quên; tự động tra phiên âm IPA & nghĩa tiếng Việt cho từ sai và cho phép ôn ngay bằng Flashcard hoặc Quiz.
  - **Gia sư Chat AI tích hợp XAI**: Hiểu tốt cả tiếng Việt không dấu (ngữ pháp, tra từ, tư vấn học tập), tự lồng từ vựng học viên hay sai vào câu ví dụ, hiển thị Markdown (Markwon), có trạng thái *"đang suy nghĩ"* và lưu nhiều phiên chat.

### Phân hệ Quản trị (`Web_Admin/lexivt-admin`)
- **Xác thực Admin an toàn**: Đăng nhập qua `POST /api/auth/admin-login`, tự động làm mới Access Token khi hết hạn (`401` auto-refresh qua `adminApi.js`).
- **Dashboard tổng quan**: Thống kê tổng số từ vựng, tổng số học viên và hoạt động đăng ký gần đây.
- **Quản lý Từ vựng**: Tìm kiếm, thêm mới, chỉnh sửa và xóa từ vựng theo 5 chủ đề TOEIC.
- **Quản lý Học viên**: Tìm kiếm, lọc theo trạng thái (`Active`/`Banned`), xem hồ sơ XP/Streak/từ đã thuộc và khóa/mở khóa tài khoản.
- **Phân tích Hành vi & Global XAI (`/xai-analytics`)**: Hiển thị biểu đồ tầm quan trọng đặc trưng toàn cục (**Global XGBoost Gain**), danh sách **Học viên cần hỗ trợ sớm (At-Risk Students)** kèm nguyên nhân SHAP hàng đầu, bộ lọc theo mức rủi ro (`Cao / Trung bình / Thấp`), tìm kiếm và nút **Xuất báo cáo CSV** (UTF-8).

---

## 4. Mô hình Machine Learning & Explainable AI (EdNet KT1)

Pipeline huấn luyện chống rò rỉ dữ liệu nằm trong `ml-training/kt1_fixed.ipynb`, huấn luyện trên tập dữ liệu thực tế **EdNet KT1**:

- **Quy mô dữ liệu**: 1.000 học viên (`KT1-1000us.zip`), tổng cộng **1.427.687** lượt tương tác làm bài.
- **Chuẩn hóa khoa học dữ liệu (Leak-Free Pipeline)**:
  1. **User-level Group Split**: Chia Train / Val / Test (70% / 15% / 15%) bằng `GroupShuffleSplit` theo `user_id` — đảm bảo 100% không trùng học viên giữa 3 tập.
  2. **Chống Target Leakage & Chuẩn hóa chiều độ khó**: Tính `question_difficulty = 1 - pass_rate_smooth` (làm mượt Bayesian `m = 10`) **chỉ trên tập Train** (`0.0`: Rất dễ → `1.0`: Rất khó) giúp chiều tác động SHAP nhất quán với trực giác sư phạm.
  3. **Tách 2 mô hình chuyên biệt chống lỗi OOD (`elapsed_time = 0`)**:
     - **Post-Quiz Full Model (`ednet_xgboost_model_final.json` — 7 đặc trưng)**: Dùng cho Bệnh án Học tập & Counterfactual What-If sau khi có thời gian làm bài thực tế.
     - **Pre-Quiz Model (`ednet_xgboost_prequiz_model.json` — 6 đặc trưng, bỏ `elapsed_time`)**: Dùng cho Thử thách XAI trước bài làm và Sổ tay Tiên tri.

| Biến đặc trưng | Ý nghĩa (Tiếng Việt) | Tiền xử lý nhân quả (Strictly Causal) |
| :--- | :--- | :--- |
| `user_history_acc` | Tỉ lệ làm đúng lịch sử | `shift(1).expanding().mean()`, điền khuyết `0.5` |
| `user_questions_attempted` | Tổng số câu hỏi đã luyện tập | `cumcount()` theo từng học viên |
| `question_difficulty` | Độ khó câu hỏi (`0`: Dễ → `1`: Khó) | `1 - pass_rate` có làm mượt Bayesian chỉ trên tập Train |
| `rolling_acc_5` | Phong độ 5 câu gần nhất | `shift(1).rolling(5).mean()`, điền khuyết `0.5` |
| `elapsed_time` *(chỉ Post-Quiz)* | Thời gian suy nghĩ bài này (ms) | Cắt outlier `[0, 300000]`, điền khuyết trung vị Train |
| `user_avg_elapsed_time` | Tốc độ suy nghĩ trung bình (ms) | `shift(1).expanding().mean()`, điền khuyết trung vị Train |
| `log_time_diff` | Khoảng cách giữa các phiên học | `np.log1p(max(0, time_diff_seconds))` |

- **Kết quả đánh giá trên tập Test (không trùng User)**:
  - **Post-Quiz Model (7 biến — `ednet_xgboost_model_final.json`)**: Test ROC-AUC = **0.7523**, Accuracy = **0.6717**, LogLoss = **0.5923**.
  - **Pre-Quiz Model (6 biến — `ednet_xgboost_prequiz_model.json`)**: Test ROC-AUC = **0.7476**.

---

## 5. Công nghệ sử dụng

| Thành phần | Công nghệ & Thư viện chính |
| :--- | :--- |
| **App Android** | Kotlin, Android SDK, ViewBinding, Retrofit2, OkHttp3, Gson, Coroutines, LiveData/ViewModel, Markwon, TextToSpeech, SpeechRecognizer, AlarmManager |
| **Backend API** | Node.js, Express.js, MongoDB (Mongoose), JWT (`jsonwebtoken`), `bcryptjs`, Axios, `@google/generative-ai`, `groq-sdk`, `nodemailer` |
| **Predictive AI** | Python 3.11, FastAPI 0.115, Uvicorn 0.30, XGBoost 2.0.3, SHAP 0.46, Pandas 2.2, NumPy 1.26, Scikit-learn 1.5.2, Docker |
| **Web Admin** | React, Vite, React Router DOM, Tailwind CSS, Lucide Icons |
| **Huấn luyện ML** | Google Colab, Jupyter Notebook (`kt1_fixed.ipynb`), Matplotlib, Seaborn |

---

## 6. Cấu trúc thư mục dự án

```text
DoAn3/
├── App_Android/                          # Ứng dụng di động Android (Kotlin + XML)
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/doan3/
│       │   ├── MainActivity.kt
│       │   └── ui/
│       │       ├── home/                 # HomeFragment (Mục tiêu ngày, Streak 7 ngày, Sổ tay Tiên tri)
│       │       ├── roadmap/              # RoadmapFragment, IeltsQuizActivity, QuizSummaryActivity, DictationActivity
│       │       ├── vocabulary/           # VocabularyFragment, WordDetailFragment (TTS + SpeechRecognizer)
│       │       ├── ai/                   # ChatAIActivity, ChatAdapter (Markwon + typing indicator)
│       │       ├── statistics/           # StatisticsFragment (Tuần/Tháng, What-If Simulator, Kỷ niệm)
│       │       ├── learning/             # LearningFragment (5 chủ đề TOEIC & Quiz tự tạo)
│       │       └── profile/              # ApiService, TokenAuthenticator, Onboarding, BootCompletedReceiver, HoSo, TaiKhoan, KhamPha, TaoQuiz
│       └── res/layout/                   # 34 tệp giao diện XML
├── Backend_API/                          # Node.js Express API Server (cổng 8080)
│   └── src/
│       ├── server.js
│       ├── config/db.js
│       ├── middleware/                   # authMiddleware.js, requireAdmin.js
│       ├── models/                       # User, Roadmap, Vocabulary, History, ChatSession, ChatMessage
│       ├── routes/                       # authRoutes, quizRoutes, vocabRoutes, statsRoutes, aiRoutes, userRoutes
│       ├── controllers/                  # aiController, vocabController, userController
│       ├── service/                      # aiService (Gemini/Groq), xaiService (FastAPI), streakService
│       └── scripts/                      # createAdmin.js, fixCorruptedStats.js
├── Web_Admin/lexivt-admin/               # Giao diện quản trị React + Vite + Tailwind
│   └── src/
│       ├── main.jsx, App.jsx
│       ├── utils/adminApi.js             # Tự động refresh token khi gặp 401
│       ├── layouts/AdminLayout.jsx
│       └── pages/                        # Login, Dashboard, Vocabulary, Users, XaiAnalytics
├── Predictive_AI/                        # Microservice dự đoán & XAI (Python FastAPI + Docker, cổng 8000)
│   ├── main.py
│   ├── ednet_xgboost_model_final.json    # Post-Quiz 7 đặc trưng
│   ├── ednet_xgboost_prequiz_model.json  # Pre-Quiz 6 đặc trưng
│   ├── requirements.txt
│   └── Dockerfile
└── ml-training/                          # Notebook huấn luyện không rò rỉ dữ liệu
    └── kt1_fixed.ipynb
```

---

## 7. Danh sách API chính

### Backend Node.js (`/api` — Cổng `8080`)
| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register`, `/api/auth/login`, `/api/auth/admin-login` | Đăng ký, đăng nhập học viên & đăng nhập quản trị viên |
| `POST` | `/api/auth/google-login`, `/api/auth/forgot-password`, `/api/auth/reset-password` | Đăng nhập Google (chặn Admin), khôi phục mật khẩu bằng OTP |
| `POST` | `/api/auth/refresh-token`, `/api/auth/logout` | Làm mới Access Token và thu hồi Refresh Token |
| `GET` / `PUT` | `/api/auth/profile`, `/api/auth/stats`, `/api/auth/change-password` | Hồ sơ cá nhân, chỉ số Gamification và đổi mật khẩu |
| `GET` / `POST` | `/api/vocabularies`, `/api/vocabularies/personal`, `/api/vocabularies/seed` | Danh sách từ vựng cá nhân hóa, thêm từ cá nhân, seed 500 từ TOEIC |
| `PUT` | `/api/vocabularies/:id/proficiency` | Cập nhật độ thành thạo, `lastReviewedDate`, `reviewCount` & Streak |
| `GET` | `/api/vocabularies/predict-forgetting`, `/api/vocabularies/stats` | Lấy danh sách **Sổ tay Tiên tri XAI** và thống kê từ vựng |
| `POST` / `GET` | `/api/vocabularies/custom-quiz`, `/api/vocabularies/custom-categories` | Tạo và lấy danh sách bộ Quiz AI từ văn bản/tài liệu |
| `POST` | `/api/quiz/submit` | Nộp kết quả Quiz/Dictation, tự tra nghĩa + IPA từ sai, trả **Bệnh án XAI** |
| `POST` / `GET` / `PUT` | `/api/ai/generate-roadmap`, `/api/ai/my-roadmap`, `/api/ai/my-roadmaps/:id/activate` | Sinh, lấy (kèm tiến độ tuần) và chuyển đổi lộ trình AI |
| `GET` / `POST` | `/api/ai/generate-quiz`, `/api/ai/roadmap/complete-step`, `/api/ai/claim-weekly-reward` | Sinh Quiz theo chặng, mở khóa chặng kế tiếp & nhận thưởng tuần |
| `POST` / `GET` | `/api/ai/session`, `/api/ai/chat`, `/api/ai/session/:id/messages`, `/api/ai/sessions/user/:id` | Chat AI tích hợp ngữ cảnh XAI (có bảo vệ chống IDOR) |
| `GET` | `/api/stats/learning?range=this_week\|last_week\|month` | Thống kê học tập theo Tuần này / Tuần trước / Tháng này (UTC+7) |
| `GET` | `/api/stats/dashboard`, `/api/stats/xai-analytics` | Tổng quan Admin & Global XAI Analytics (yêu cầu quyền Admin) |
| `GET` / `PUT` | `/api/users`, `/api/users/:id/status`, `/api/users/leaderboard` | Quản lý học viên (Admin) và bảng xếp hạng Top 10 XP |

### Predictive AI Microservice (FastAPI — Cổng `8000`)
| Phương thức | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/` | Kiểm tra trạng thái hoạt động & tình trạng nạp 2 mô hình XGBoost |
| `POST` | `/predict-weakness` | Dự đoán xác suất đúng (7 biến), trả SHAP chi tiết & Counterfactual What-If |
| `POST` | `/predict-prequiz` | Dự đoán độ khó trước khi làm bài (6 biến Pre-Quiz, tránh OOD) |
| `POST` | `/predict-forgetting` | Dự đoán nguy cơ quên từ vựng (Pre-Quiz XGBoost + Ebbinghaus Decay) |
| `POST` | `/counterfactual` | Sinh kịch bản hành vi thay thế ("What-If") để cải thiện xác suất đúng |
| `GET` | `/global-xai` | Trả về độ quan trọng đặc trưng toàn cục (XGBoost Gain & Weight) |
| `POST` | `/batch-risk-analysis` | Đánh giá rủi ro hàng loạt học viên và nguyên nhân SHAP hàng đầu cho Admin |

---

## 8. Hướng dẫn cài đặt và khởi chạy

### Bước 1: Khởi chạy Predictive AI Microservice (Cổng `8000`)
```bash
cd Predictive_AI
# Cách 1: Chạy bằng Docker (build lần đầu)
docker build -t lexivt-predictive-ai . && docker run --rm -p 8000:8000 lexivt-predictive-ai
# (Các lần sau chỉ cần: docker run --rm -p 8000:8000 lexivt-predictive-ai)

# Cách 2: Chạy trực tiếp bằng môi trường ảo Python 3.11
source predictive_env/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Bước 2: Khởi chạy Backend Node.js (Cổng `8080`)
Tạo tệp `.env` trong thư mục `Backend_API/`:
```env
PORT=8080
MONGODB_CONNECTIONSTRING_STRING=mongodb+srv://<user>:<password>@<cluster>/<db>
JWT_SECRET=<chuoi_bi_mat_jwt>
REFRESH_SECRET=<chuoi_bi_mat_refresh>
GEMINI_API_KEY=<google_gemini_api_key>
GROQ_API_KEY=<groq_api_key>
PYTHON_AI_URL=http://127.0.0.1:8000
```
Khởi chạy server (và tạo tài khoản Admin lần đầu nếu chưa có):
```bash
cd Backend_API
npm install
node src/scripts/createAdmin.js
node src/server.js
```

### Bước 3: Khởi chạy Web Admin (React + Vite)
```bash
cd Web_Admin/lexivt-admin
npm install
npm run dev
```

### Bước 4: Khởi chạy ứng dụng Android
1. Mở thư mục `App_Android/` trong **Android Studio**.
2. Kiểm tra `BASE_URL` trong `App_Android/app/src/main/java/com/example/doan3/ui/profile/ApiService.kt` (mặc định `http://10.0.2.2:8080/` cho Android Emulator; đổi `USE_PHYSICAL_DEVICE = true` và cập nhật IP LAN khi chạy trên điện thoại thật).
3. Nhấn **Run** để biên dịch và trải nghiệm.

---

## 9. Nhóm thực hiện

- **Đề tài**: Hệ thống Phân tích Hành vi Người học và Gợi ý Lộ trình Học (EdTech XAI) — Đồ án 4
- **Thành viên thực hiện**:
  - **Nguyễn Thái Tuyền** — MSSV: `CNTT2311002`
  - **Phan Lê Tú Văn** — MSSV: `CNTT2311008`
