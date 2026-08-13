import { useState, useEffect } from "react";
import { Users, BookOpen, Activity } from "lucide-react";

export default function Dashboard() {
  // 1. Khởi tạo state để chứa dữ liệu thực từ API
  const [stats, setStats] = useState({
    totalVocab: 0,
    activeUsers: 0,
  });
  const [recentActivities, setRecentActivities] = useState([]);

  // 2. Gọi API khi màn hình vừa load
  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      // Thay URL này bằng endpoint Backend thực tế 
    const response = await fetch("http://localhost:3000/api/stats/dashboard", {
        headers: {
            "Authorization": `Bearer ${localStorage.getItem("adminToken")}`
        }
    });
      
      if (response.ok) {
        const data = await response.json();
        setStats({
          totalVocab: data.totalVocab,
          activeUsers: data.totalUsers,
        });
        setRecentActivities(data.recentActivities);
      }
    } catch (error) {
      console.error("Lỗi khi tải dữ liệu tổng quan:", error);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Tổng quan hệ thống</h1>

      {/* ĐÃ CHỈNH SỬA: Đổi từ md:grid-cols-3 thành md:grid-cols-2 và xóa thẻ AI Quiz */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Thẻ 1: Tổng số từ vựng */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex items-center justify-between hover:shadow-md transition-shadow">
          <div>
            <p className="text-sm font-medium text-gray-500 mb-1">
              Tổng số từ vựng
            </p>
            <p className="text-3xl font-bold text-gray-900">
              {stats.totalVocab.toLocaleString()}
            </p>
          </div>
          <div className="w-14 h-14 bg-blue-50 rounded-full flex items-center justify-center text-blue-600">
            <BookOpen size={28} />
          </div>
        </div>

        {/* Thẻ 2: Học viên hoạt động */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex items-center justify-between hover:shadow-md transition-shadow">
          <div>
            <p className="text-sm font-medium text-gray-500 mb-1">
              Học viên hoạt động
            </p>
            <p className="text-3xl font-bold text-green-600">
              {stats.activeUsers.toLocaleString()}
            </p>
          </div>
          <div className="w-14 h-14 bg-green-50 rounded-full flex items-center justify-center text-green-600">
            <Users size={28} />
          </div>
        </div>

      </div>

      {/* Khu vực Hoạt động gần đây */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden mt-8">
        <div className="p-6 border-b border-gray-200 flex items-center gap-2 bg-gray-50/50">
          <Activity className="text-blue-600" size={20} />
          <h2 className="text-lg font-bold text-gray-800">
            Hoạt động hệ thống gần đây
          </h2>
        </div>
        <div className="p-0">
          {recentActivities.length > 0 ? (
            recentActivities.map((activity, index) => (
              <div
                key={activity.id}
                className={`p-5 flex items-center justify-between hover:bg-gray-50 transition-colors ${
                  index !== recentActivities.length - 1
                    ? "border-b border-gray-100"
                    : ""
                }`}
              >
                <div className="flex items-center gap-4">
                  <div className="w-2.5 h-2.5 rounded-full bg-blue-500 shadow-sm shadow-blue-200"></div>
                  <p className="text-gray-700">
                    <span className="font-semibold text-gray-900">
                      {activity.user}
                    </span>{" "}
                    {activity.action}
                  </p>
                </div>
                <span className="text-sm font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
                  {activity.time}
                </span>
              </div>
            ))
          ) : (
            <div className="p-5 text-gray-500 text-center">Đang tải dữ liệu hoạt động...</div>
          )}
        </div>
      </div>
    </div>
  );
}