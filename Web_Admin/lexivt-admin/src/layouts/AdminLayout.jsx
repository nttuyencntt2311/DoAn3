import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { LayoutDashboard, BookOpen, Users, BrainCircuit, LogOut, Bell } from 'lucide-react';

export default function AdminLayout() {
  const navigate = useNavigate();
  const [showNotifMenu, setShowNotifMenu] = useState(false);

  const adminUser = (() => {
    try {
      return JSON.parse(localStorage.getItem('adminUser') || 'null');
    } catch {
      return null;
    }
  })();

  const displayName = adminUser?.name || 'Admin';
  const displayInitial = displayName.charAt(0).toUpperCase();

  const navItems = [
    { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={20} /> },
    { name: 'Từ vựng', path: '/vocabulary', icon: <BookOpen size={20} /> },
    { name: 'Người dùng', path: '/users', icon: <Users size={20} /> },
    { name: 'Phân tích XAI', path: '/xai-analytics', icon: <BrainCircuit size={20} /> },
  ];

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminRefreshToken');
    localStorage.removeItem('adminUser');
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar bên trái */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <span className="text-2xl font-bold text-blue-600">LexiVT Admin</span>
        </div>

        <nav className="flex-1 py-4 flex flex-col gap-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.name}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-600 font-medium'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {item.icon}
              {item.name}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-200">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 w-full text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          >
            <LogOut size={20} />
            <span className="font-medium">Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Khu vực nội dung bên phải */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header (Thanh Topbar) */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 relative">
          <h2 className="text-xl font-semibold text-gray-800">Trang quản trị</h2>
          <div className="flex items-center gap-4">
            <div className="relative">
              <button
                onClick={() => setShowNotifMenu((prev) => !prev)}
                className="p-2 text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-full transition-colors"
                title="Thông báo hệ thống"
              >
                <Bell size={20} />
              </button>
              {showNotifMenu && (
                <div className="absolute right-0 mt-2 w-72 bg-white border border-gray-200 rounded-xl shadow-lg p-4 z-50">
                  <p className="text-sm font-semibold text-gray-800 mb-2">Thông báo hệ thống</p>
                  <p className="text-xs text-gray-500 mb-3">
                    Theo dõi danh sách học viên có rủi ro cao theo thời gian thực tại mục Phân tích XAI.
                  </p>
                  <button
                    onClick={() => {
                      setShowNotifMenu(false);
                      navigate('/xai-analytics');
                    }}
                    className="w-full text-xs font-medium bg-indigo-50 text-indigo-700 hover:bg-indigo-100 py-2 rounded-lg transition-colors"
                  >
                    Mở Phân tích XAI (SHAP) →
                  </button>
                </div>
              )}
            </div>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold">
                {displayInitial}
              </div>
              <div className="leading-tight">
                <p className="text-sm font-medium text-gray-800">{displayName}</p>
                {adminUser?.email && (
                  <p className="text-xs text-gray-400">{adminUser.email}</p>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
