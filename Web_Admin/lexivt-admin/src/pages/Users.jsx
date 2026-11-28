import { useState, useEffect } from "react";
import {
  Search,
  Eye,
  ShieldBan,
  ShieldCheck,
  X,
  Trophy,
  Flame,
  BookOpen,
} from "lucide-react";

import { adminFetch } from "../utils/adminApi";

export default function Users() {
  // Khởi tạo mảng rỗng để chờ dữ liệu từ API
  const [users, setUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("All");
  const [selectedUser, setSelectedUser] = useState(null);

  // Gọi API lấy danh sách học viên khi trang vừa load
  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await adminFetch("/api/users");
      if (response.ok) {
        const data = await response.json();
        
        // Đảm bảo dữ liệu nhận được chắc chắn là một mảng
        if (Array.isArray(data)) {
          setUsers(data);
        } else if (data && Array.isArray(data.users)) {
          setUsers(data.users);
        } else {
          setUsers([]); 
        }
      }
    } catch (error) {
      console.error("Lỗi khi tải danh sách người dùng:", error);
      setUsers([]); 
    }
  };
  // Logic lọc (Search + Status)
  const filteredUsers = users.filter((user) => {
    const searchLower = searchTerm.toLowerCase();
    // Thêm fallback || "" đề phòng dữ liệu bị rỗng
    const name = user.name || "";
    const email = user.email || "";
    
    const matchesSearch =
      name.toLowerCase().includes(searchLower) ||
      email.toLowerCase().includes(searchLower);

    // Mặc định tài khoản không có trường status là Active
    const currentStatus = user.status || "Active";
    const matchesStatus =
      filterStatus === "All" || currentStatus === filterStatus;

    return matchesSearch && matchesStatus;
  });

  // Gọi API để cập nhật trạng thái Khóa / Mở khóa
  const handleToggleStatus = async (id, currentStatus) => {
    const status = currentStatus || "Active";
    const action = status === "Active" ? "khóa" : "mở khóa";
    const isConfirm = window.confirm(`Bạn có chắc chắn muốn ${action} tài khoản này?`);

    if (isConfirm) {
      try {
        const newStatus = status === "Active" ? "Banned" : "Active";
        
        // Gọi API cập nhật lên Node.js
        const response = await adminFetch(`/api/users/${id}/status`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: newStatus })
        });

        if (response.ok) {
          // Cập nhật lại giao diện ngay lập tức
          setUsers(
            users.map((user) =>
              user._id === id ? { ...user, status: newStatus } : user
            )
          );
        } else {
          alert("Lỗi từ server khi cập nhật trạng thái!");
        }
      } catch (error) {
        console.error("Lỗi cập nhật trạng thái:", error);
        alert("Có lỗi xảy ra khi kết nối với server!");
      }
    }
  };

  // Hàm chuyển đổi định dạng ngày tháng từ MongoDB (ISO) sang DD/MM/YYYY
  const formatDate = (dateString) => {
    if (!dateString) return "Chưa cập nhật";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN");
  };

  return (
    <div className="space-y-6 relative">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý Người dùng</h1>
      </div>

      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex gap-4">
        <div className="relative flex-1">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            size={20}
          />
          <input
            type="text"
            placeholder="Tìm kiếm theo tên, email học viên..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
          />
        </div>

        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="bg-gray-50 border border-gray-200 text-gray-700 py-2 px-4 rounded-lg focus:outline-none focus:border-blue-500 min-w-[200px]"
        >
          <option value="All">Tất cả trạng thái</option>
          <option value="Active">Đang hoạt động</option>
          <option value="Banned">Đã bị khóa (Banned)</option>
        </select>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-50 text-gray-600 text-sm border-b border-gray-200">
              <th className="p-4 font-medium">Học viên</th>
              <th className="p-4 font-medium">Điểm XP</th>
              <th className="p-4 font-medium">Chuỗi ngày (Streak)</th>
              <th className="p-4 font-medium">Trạng thái</th>
              <th className="p-4 font-medium text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredUsers.length > 0 ? (
              filteredUsers.map((user) => {
                const currentStatus = user.status || "Active";
                return (
                  <tr key={user._id} className="hover:bg-gray-50">
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold uppercase">
                          {user.name ? user.name.charAt(0) : "?"}
                        </div>
                        <div>
                          <p className="font-semibold text-gray-800">
                            {user.name}
                          </p>
                          <p className="text-sm text-gray-500">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4 font-medium text-yellow-600">
                      {(user.xp || 0).toLocaleString()} XP
                    </td>
                    <td className="p-4 font-medium text-orange-500">
                      {user.streakDays || 0} ngày
                    </td>
                    <td className="p-4">
                      <span
                        className={`px-3 py-1 rounded-full text-xs font-medium ${
                          currentStatus === "Active"
                            ? "bg-green-50 text-green-700"
                            : "bg-red-50 text-red-700"
                        }`}
                      >
                        {currentStatus === "Active" ? "Hoạt động" : "Bị khóa"}
                      </span>
                    </td>
                    <td className="p-4 text-center">
                      <button
                        onClick={() => setSelectedUser(user)}
                        className="text-gray-400 hover:text-blue-600 mx-2 transition-colors"
                        title="Xem chi tiết"
                      >
                        <Eye size={18} />
                      </button>
                      <button
                        onClick={() => handleToggleStatus(user._id, currentStatus)}
                        className={`mx-2 transition-colors ${
                          currentStatus === "Active"
                            ? "text-gray-400 hover:text-red-600"
                            : "text-red-500 hover:text-green-600"
                        }`}
                        title={
                          currentStatus === "Active"
                            ? "Khóa tài khoản"
                            : "Mở khóa tài khoản"
                        }
                      >
                        {currentStatus === "Active" ? (
                          <ShieldBan size={18} />
                        ) : (
                          <ShieldCheck size={18} />
                        )}
                      </button>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="5" className="p-8 text-center text-gray-500">
                  Không tìm thấy học viên nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal hiển thị chi tiết người dùng */}
      {selectedUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden relative">
            <div className="h-24 bg-gradient-to-r from-blue-500 to-indigo-600"></div>

            <button
              onClick={() => setSelectedUser(null)}
              className="absolute top-4 right-4 text-white hover:text-gray-200 bg-black/20 rounded-full p-1"
            >
              <X size={20} />
            </button>

            <div className="px-6 pb-6 relative">
              <div className="w-20 h-20 rounded-full bg-white border-4 border-white shadow-md flex items-center justify-center text-3xl font-bold text-blue-600 absolute -top-10 left-6 uppercase">
                {selectedUser.name ? selectedUser.name.charAt(0) : "?"}
              </div>

              <div className="pt-12">
                <h2 className="text-xl font-bold text-gray-800">
                  {selectedUser.name}
                </h2>
                <p className="text-gray-500">{selectedUser.email}</p>
                <p className="text-sm text-gray-400 mt-1">
                  Ngày tham gia: {formatDate(selectedUser.createdAt)}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4 mt-6">
                <div className="bg-orange-50 p-4 rounded-xl border border-orange-100 flex flex-col items-center justify-center">
                  <Flame className="text-orange-500 mb-2" size={28} />
                  <p className="text-2xl font-bold text-gray-800">
                    {selectedUser.streakDays || 0}
                  </p>
                  <p className="text-xs text-gray-600 font-medium uppercase tracking-wide">
                    Chuỗi ngày
                  </p>
                </div>

                <div className="bg-yellow-50 p-4 rounded-xl border border-yellow-100 flex flex-col items-center justify-center">
                  <Trophy className="text-yellow-600 mb-2" size={28} />
                  <p className="text-2xl font-bold text-gray-800">
                    {selectedUser.xp || 0}
                  </p>
                  <p className="text-xs text-gray-600 font-medium uppercase tracking-wide">
                    Tổng XP
                  </p>
                </div>

                <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex flex-col items-center justify-center col-span-2">
                  <BookOpen className="text-blue-500 mb-2" size={28} />
                  <p className="text-2xl font-bold text-gray-800">
                    {selectedUser.wordsLearned || 0}
                  </p>
                  <p className="text-xs text-gray-600 font-medium uppercase tracking-wide">
                    Từ vựng đã thuộc
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}