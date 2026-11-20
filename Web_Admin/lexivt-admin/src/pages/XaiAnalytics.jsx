import { useState, useEffect } from "react";
import { BrainCircuit, AlertTriangle, Users, TrendingUp, RefreshCw } from "lucide-react";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export default function XaiAnalytics() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchXaiAnalytics();
  }, []);

  const fetchXaiAnalytics = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE_URL}/api/stats/xai-analytics`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("adminToken")}`,
        },
      });
      if (!response.ok) {
        throw new Error(`Lỗi máy chủ (${response.status})`);
      }
      const json = await response.json();
      setData(json);
    } catch (err) {
      console.error("Lỗi tải XAI Analytics:", err);
      setError("Không thể tải dữ liệu phân tích XAI. Kiểm tra quyền Admin hoặc kết nối Backend.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-gray-500">
        Đang chạy mô hình XGBoost & SHAP tổng hợp dữ liệu học viên...
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 bg-red-50 border border-red-200 rounded-xl text-red-700 flex items-center justify-between">
        <span>{error}</span>
        <button
          onClick={fetchXaiAnalytics}
          className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm hover:bg-red-700"
        >
          Thử lại
        </button>
      </div>
    );
  }

  const importance = data?.globalImportance || [];
  const students = data?.atRiskStudents || [];
  const summary = data?.summary || { studentsAnalyzed: 0, atRiskCount: 0, avgPredictedAccuracy: 0 };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <BrainCircuit className="text-indigo-600" size={28} />
            Phân tích Hành vi & Global XAI (SHAP)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Trạng thái Microservice Predictive AI:{" "}
            <span className={data?.serviceOnline ? "text-green-600 font-semibold" : "text-orange-600 font-semibold"}>
              {data?.serviceOnline ? "Đang hoạt động (Realtime XGBoost + SHAP)" : "Chế độ dự phòng (FastAPI offline)"}
            </span>
          </p>
        </div>
        <button
          onClick={fetchXaiAnalytics}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 text-sm font-medium"
        >
          <RefreshCw size={16} />
          Làm mới phân tích
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-500">Học viên đã phân tích</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">{summary.studentsAnalyzed}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
            <Users size={24} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-500">Học viên rủi ro cao (&lt;45%)</p>
            <p className="text-3xl font-bold text-red-600 mt-1">{summary.atRiskCount}</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center">
            <AlertTriangle size={24} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-500">Xác suất làm đúng TB</p>
            <p className="text-3xl font-bold text-emerald-600 mt-1">{summary.avgPredictedAccuracy}%</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <TrendingUp size={24} />
          </div>
        </div>
      </div>

      {/* Global Feature Importance Chart */}
      <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
        <h2 className="text-lg font-bold text-gray-800 mb-1">
          Tầm quan trọng đặc trưng toàn cục (Global XGBoost Gain)
        </h2>
        <p className="text-sm text-gray-500 mb-5">
          Mức độ ảnh hưởng của 7 biến hành vi EdNet lên kết quả dự đoán năng lực học viên.
        </p>

        <div className="space-y-4">
          {importance.map((item) => (
            <div key={item.feature}>
              <div className="flex justify-between text-sm mb-1">
                <span className="font-medium text-gray-700">{item.vi_description}</span>
                <span className="font-semibold text-indigo-600">{item.importance_percent}%</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden">
                <div
                  className="bg-indigo-600 h-3 rounded-full transition-all"
                  style={{ width: `${Math.min(100, Math.max(2, item.importance_percent))}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* At-Risk Students Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-gray-200 bg-gray-50/50">
          <h2 className="text-lg font-bold text-gray-800">
            Danh sách Học viên cần hỗ trợ sớm (At-Risk Students — Xếp theo rủi ro)
          </h2>
        </div>
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-50 text-gray-600 text-sm border-b border-gray-200">
              <th className="p-4 font-medium">Học viên</th>
              <th className="p-4 font-medium">Xác suất dự kiến</th>
              <th className="p-4 font-medium">Mức rủi ro</th>
              <th className="p-4 font-medium">Nguyên nhân SHAP hàng đầu</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {students.length > 0 ? (
              students.map((st) => (
                <tr key={st.user_id} className="hover:bg-gray-50">
                  <td className="p-4">
                    <p className="font-semibold text-gray-800">{st.name}</p>
                    <p className="text-xs text-gray-500">{st.email}</p>
                  </td>
                  <td className="p-4 font-bold text-gray-800">{st.predicted_accuracy}%</td>
                  <td className="p-4">
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-medium ${
                        st.risk_level === "Cao"
                          ? "bg-red-50 text-red-700"
                          : st.risk_level === "Trung bình"
                          ? "bg-amber-50 text-amber-700"
                          : "bg-green-50 text-green-700"
                      }`}
                    >
                      {st.risk_level}
                    </span>
                  </td>
                  <td className="p-4 text-sm text-gray-600">{st.top_risk_driver}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="p-8 text-center text-gray-500">
                  Chưa có dữ liệu phân tích rủi ro học viên.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
