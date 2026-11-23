import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ currentPath, onNavigate }) {
  const { user, isAuthenticated, logout, getPrimaryRole, getDashboardRoute } = useAuth();
  const role = getPrimaryRole();

  const handleLogout = async () => {
    await logout();
    onNavigate('/login');
  };

  const getRoleBadgeColor = () => {
    switch (role) {
      case 'ADMIN': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'UNDERWRITER': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'BORROWER': return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <nav className="bg-slate-900 border-b border-slate-800 sticky top-0 z-50 text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onNavigate(isAuthenticated ? getDashboardRoute() : '/login')}>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 font-bold text-xl text-slate-950">
              U
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-300 bg-clip-text text-transparent">
                Utang na Loob
              </span>
              <span className="hidden sm:inline-block ml-2 text-xs uppercase px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-medium">
                Micro-Lending
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {isAuthenticated ? (
              <>
                <div className="flex items-center space-x-3 border-r border-slate-800 pr-4">
                  <div className="text-right hidden sm:block">
                    <div className="text-sm font-semibold text-slate-200">
                      {user?.firstName} {user?.lastName}
                    </div>
                    <div className="text-xs text-slate-400">{user?.email}</div>
                  </div>
                  <span className={`text-xs px-2.5 py-1 rounded-full font-semibold border ${getRoleBadgeColor()}`}>
                    {role || 'USER'}
                  </span>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => onNavigate(getDashboardRoute())}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                      currentPath.startsWith('/dashboard')
                        ? 'bg-emerald-500 text-slate-950 shadow-md font-semibold'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    Dashboard
                  </button>

                  <button
                    onClick={() => onNavigate('/profile')}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                      currentPath === '/profile'
                        ? 'bg-teal-500 text-slate-950 shadow-md font-semibold'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    My Profile
                  </button>

                  <button
                    onClick={handleLogout}
                    className="px-3 py-1.5 rounded-lg text-sm font-medium text-rose-300 hover:text-rose-200 hover:bg-rose-950/40 border border-rose-800/40 transition"
                  >
                    Logout
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => onNavigate('/login')}
                  className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition ${
                    currentPath === '/login'
                      ? 'bg-emerald-500 text-slate-950 font-semibold'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  Sign In
                </button>
                <button
                  onClick={() => onNavigate('/register')}
                  className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition ${
                    currentPath === '/register'
                      ? 'bg-teal-500 text-slate-950 font-semibold'
                      : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                  }`}
                >
                  Register
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
