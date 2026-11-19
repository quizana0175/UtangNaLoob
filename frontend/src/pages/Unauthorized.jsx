import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Unauthorized({ onNavigate }) {
  const { getDashboardRoute } = useAuth();

  return (
    <div className="min-h-[75vh] flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-slate-900 border border-rose-900/40 rounded-3xl p-8 text-center shadow-2xl">
        <div className="w-16 h-16 mx-auto rounded-2xl bg-rose-500/10 text-rose-400 flex items-center justify-center text-3xl font-black mb-4 border border-rose-500/20">
          !
        </div>
        <h2 className="text-2xl font-black text-white mb-2">403 - Access Forbidden</h2>
        <p className="text-slate-400 text-sm mb-6">
          You do not have permission to access this page. Your role does not match the required authorization level.
        </p>
        <button
          onClick={() => onNavigate(getDashboardRoute())}
          className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-bold transition shadow-lg shadow-emerald-600/20"
        >
          Return to My Role Dashboard
        </button>
      </div>
    </div>
  );
}
