import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function Login({ onNavigate }) {
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!formData.email || !formData.password) {
      setError('Please provide both email and password.');
      return;
    }

    setLoading(true);
    try {
      const user = await login(formData);
      // Role-based redirection after successful authentication
      if (user.roles?.includes('ROLE_ADMIN')) {
        onNavigate('/dashboard/admin');
      } else if (user.roles?.includes('ROLE_UNDERWRITER')) {
        onNavigate('/dashboard/underwriter');
      } else {
        onNavigate('/dashboard/borrower');
      }
    } catch (err) {
      setError(err.message || 'Invalid email or password. Please verify your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (email, password) => {
    setFormData({ email, password });
    setError('');
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-slate-900 border border-slate-800 p-8 rounded-3xl shadow-2xl">
        <div className="text-center">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 font-black text-2xl text-slate-950 mb-3">
            U
          </div>
          <h2 className="text-3xl font-extrabold text-white tracking-tight">
            Sign In to Portal
          </h2>
          <p className="mt-2 text-sm text-slate-400">
            Access your Utang na Loob credit line or management console
          </p>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-start space-x-3">
            <span className="font-bold text-base leading-none">!</span>
            <div className="flex-1">{error}</div>
          </div>
        )}

        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Email Address
            </label>
            <input
              name="email"
              type="email"
              autoComplete="email"
              required
              value={formData.email}
              onChange={handleChange}
              placeholder="user@utangnaloob.ph"
              className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Password
            </label>
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 px-4 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg shadow-emerald-500/20 transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed text-center"
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        {/* Quick Demo Login Credentials Bar */}
        <div className="pt-4 border-t border-slate-800">
          <p className="text-xs font-semibold text-slate-400 text-center uppercase tracking-wider mb-3">
            One-Click Demo Credentials
          </p>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => handleQuickFill('borrower@utangnaloob.ph', 'Borrower@123')}
              className="px-2 py-2 bg-slate-800 hover:bg-slate-700 text-emerald-300 rounded-lg text-xs font-medium border border-slate-700 text-center transition"
            >
              Borrower
            </button>
            <button
              type="button"
              onClick={() => handleQuickFill('underwriter@utangnaloob.ph', 'Underwriter@123')}
              className="px-2 py-2 bg-slate-800 hover:bg-slate-700 text-blue-300 rounded-lg text-xs font-medium border border-slate-700 text-center transition"
            >
              Underwriter
            </button>
            <button
              type="button"
              onClick={() => handleQuickFill('admin@utangnaloob.ph', 'Admin@123')}
              className="px-2 py-2 bg-slate-800 hover:bg-slate-700 text-purple-300 rounded-lg text-xs font-medium border border-slate-700 text-center transition"
            >
              Admin
            </button>
          </div>
        </div>

        <div className="text-center pt-2">
          <p className="text-sm text-slate-400">
            Don't have an account yet?{' '}
            <button
              onClick={() => onNavigate('/register')}
              className="font-semibold text-emerald-400 hover:text-emerald-300 transition"
            >
              Register here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
