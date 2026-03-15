import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Building2, Activity, LogOut } from 'lucide-react';
import { clearToken } from './lib/auth';
import { CrawlLogProvider } from './context/CrawlLogContext';

const NAV_ITEMS = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/companies', label: 'Companies', icon: Building2, end: false },
  { to: '/admin/crawls', label: 'Crawls', icon: Activity, end: false },
  { to: '/admin/pipeline', label: 'Pipeline', icon: Activity, end: false },
  { to: '/admin/analytics', label: 'Analytics', icon: LayoutDashboard, end: false },
];

export function AdminLayout() {
  const handleLogout = () => {
    clearToken();
    window.location.href = '/admin';
  };

  return (
    <CrawlLogProvider>
      <div className="flex h-screen bg-gray-50 overflow-hidden">
        {/* Sidebar */}
        <aside className="w-52 shrink-0 bg-gray-900 text-white flex flex-col">
          <div className="px-4 py-4 border-b border-gray-800">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest">Admin</p>
            <p className="text-sm font-bold text-white mt-0.5">DevAssembly</p>
          </div>

          <nav className="flex-1 p-2 space-y-0.5">
            {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 px-3 py-2 rounded text-sm transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-400 hover:text-white hover:bg-gray-800'
                  }`
                }
              >
                <Icon size={15} />
                {label}
              </NavLink>
            ))}
          </nav>

          <div className="p-2 border-t border-gray-800">
            <button
              onClick={handleLogout}
              className="flex items-center gap-2.5 px-3 py-2 rounded text-sm text-gray-400 hover:text-white hover:bg-gray-800 w-full transition-colors"
            >
              <LogOut size={15} /> Sign out
            </button>
          </div>
        </aside>

        {/* Main area */}
        <main className="flex-1 overflow-hidden flex flex-col min-w-0">
          <Outlet />
        </main>
      </div>
    </CrawlLogProvider>
  );
}
