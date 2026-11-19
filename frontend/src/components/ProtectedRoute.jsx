import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, allowedRoles, onNavigate }) {
  const { user, isAuthenticated, loading, hasRole } = useAuth();

  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-slate-400 text-sm">Verifying session credentials...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center shadow-xl">
          <div className="w-14 h-14 mx-auto rounded-full bg-amber-500/10 text-amber-400 flex items-center justify-center text-2xl font-bold mb-4">
            !
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Authentication Required</h2>
          <p className="text-slate-400 text-sm mb-6">
            You must be signed in with valid credentials to access this protected application page.
          </p>
          <button
            onClick={() => onNavigate('/login')}
            className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-semibold transition"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const isAuthorized = allowedRoles.some(role => hasRole(role));
    if (!isAuthorized) {
      return (
        <div className="min-h-[70vh] flex items-center justify-center px-4">
          <div className="max-w-md w-full bg-slate-900 border border-rose-900/50 rounded-2xl p-8 text-center shadow-xl">
            <div className="w-14 h-14 mx-auto rounded-full bg-rose-500/10 text-rose-400 flex items-center justify-center text-2xl font-bold mb-4">
              X
            </div>
            <h2 className="text-xl font-bold text-white mb-2">Access Denied (403)</h2>
            <p className="text-slate-400 text-sm mb-6">
              Your account does not possess the required role permissions ({allowedRoles.join(', ')}) to view this resource.
            </p>
            <button
              onClick={() => {
                if (hasRole('ADMIN')) onNavigate('/dashboard/admin');
                else if (hasRole('UNDERWRITER')) onNavigate('/dashboard/underwriter');
                else onNavigate('/dashboard/borrower');
              }}
              className="w-full py-2.5 px-4 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl font-semibold transition"
            >
              Return to My Dashboard
            </button>
          </div>
        </div>
      );
    }
  }

  return children;
}
