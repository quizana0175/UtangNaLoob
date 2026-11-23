import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Profile from './pages/Profile';
import BorrowerDashboard from './pages/BorrowerDashboard';
import UnderwriterDashboard from './pages/UnderwriterDashboard';
import AdminDashboard from './pages/AdminDashboard';
import Unauthorized from './pages/Unauthorized';

function MainRouter() {
  const [currentPath, setCurrentPath] = useState(() => {
    return window.location.hash ? window.location.hash.replace('#', '') : '/login';
  });

  const { isAuthenticated, getDashboardRoute } = useAuth();

  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.replace('#', '');
      setCurrentPath(hash || '/login');
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  const navigate = (path) => {
    window.location.hash = path;
    setCurrentPath(path);
  };

  // If already authenticated and landing on login or root, redirect to dashboard
  useEffect(() => {
    if (isAuthenticated && (currentPath === '/login' || currentPath === '/' || currentPath === '')) {
      navigate(getDashboardRoute());
    }
  }, [isAuthenticated]);

  const renderContent = () => {
    const basePath = currentPath.split('?')[0];
    switch (basePath) {
      case '/login':
        return <Login onNavigate={navigate} />;
      case '/register':
        return <Register onNavigate={navigate} />;
      case '/forgot-password':
        return <ForgotPassword onNavigate={navigate} />;
      case '/reset-password':
        return <ResetPassword onNavigate={navigate} />;
      case '/profile':
        return (
          <ProtectedRoute onNavigate={navigate}>
            <Profile onNavigate={navigate} />
          </ProtectedRoute>
        );
      case '/dashboard/borrower':
        return (
          <ProtectedRoute allowedRoles={['ROLE_BORROWER', 'ROLE_ADMIN']} onNavigate={navigate}>
            <BorrowerDashboard />
          </ProtectedRoute>
        );
      case '/dashboard/underwriter':
        return (
          <ProtectedRoute allowedRoles={['ROLE_UNDERWRITER', 'ROLE_ADMIN']} onNavigate={navigate}>
            <UnderwriterDashboard />
          </ProtectedRoute>
        );
      case '/dashboard/admin':
        return (
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']} onNavigate={navigate}>
            <AdminDashboard />
          </ProtectedRoute>
        );
      case '/unauthorized':
        return <Unauthorized onNavigate={navigate} />;
      default:
        if (isAuthenticated) {
          return (
            <ProtectedRoute onNavigate={navigate}>
              <BorrowerDashboard />
            </ProtectedRoute>
          );
        }
        return <Login onNavigate={navigate} />;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-slate-950">
      <Navbar currentPath={currentPath} onNavigate={navigate} />
      <main className="flex-1 pb-12">
        {renderContent()}
      </main>
      <footer className="border-t border-slate-900 bg-slate-950/80 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4">
          <p>© 2026 Utang na Loob: Automated Micro-Lending & Transparent Repayment Tracking Platform. All rights reserved.</p>
          <p className="mt-1 text-slate-600">Built with ReactJS, Spring Boot, Spring Security & PostgreSQL</p>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainRouter />
    </AuthProvider>
  );
}
