# Dùng bản Python 3.11 gọn nhẹ làm nền
FROM python:3.11-slim

# XGBoost trên Linux thường cần thư viện hệ thống này để chạy
RUN apt-get update && apt-get install -y libgomp1 && rm -rf /var/lib/apt/lists/*

# Tạo thư mục làm việc trong container
WORKDIR /app

# Copy requirements.txt vào trước để cài đặt (giúp tận dụng cache của Docker)
COPY requirements.txt .

# Cài đặt các thư viện
RUN pip install --default-timeout=1000 --no-cache-dir -r requirements.txt

# Copy toàn bộ mã nguồn và model vào container
COPY . .

# Mở port 8000 để bên ngoài có thể gọi API
EXPOSE 8000

# Lệnh chạy ứng dụng khi container khởi động
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]