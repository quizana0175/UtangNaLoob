import React, { useState } from 'react';
import { authApi } from '../services/api';

export default function ForgotPassword({ onNavigate }) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [tokenInfo, setTokenInfo] = useState(null);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setTokenInfo(null);

    if (!email) {
      setError('Please enter your registered email address.');
      return;
    }

    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email);
      setMessage(res.message || 'Reset token generated successfully.');
      if (res.token) {
        setTokenInfo({
          token: res.token,
          expiryMinutes: res.expiryMinutes || 15
        });
      }
    } catch (err) {
      setError(err.message || 'Unable to process password reset request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-6 bg-slate-900 border border-slate-800 p-8 rounded-3xl shadow-2xl">
        <div className="text-center">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-400 flex items-center justify-center shadow-lg shadow-amber-500/20 font-black text-2xl text-slate-950 mb-3">
            ?
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">
            Reset Your Password
          </h2>
          <p className="mt-2 text-sm text-slate-400">
            Enter your email to receive an expiring single-use verification token
          </p>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm">
            {error}
          </div>
        )}

        {message && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm">
            {message}
          </div>
        )}

        {tokenInfo && (
          <div className="p-4 rounded-xl bg-slate-950 border border-amber-500/40 text-slate-200 space-y-3">
            <div className="text-xs font-semibold text-amber-400 uppercase tracking-wider">
              Single-Use Security Token ({tokenInfo.expiryMinutes} Min Expiry)
            </div>
            <div className="p-2.5 bg-slate-900 rounded font-mono text-xs text-amber-200 break-all select-all border border-slate-800">
              {tokenInfo.token}
            </div>
            <button
              type="button"
              onClick={() => onNavigate(`/reset-password?token=${encodeURIComponent(tokenInfo.token)}`)}
              className="w-full py-2.5 px-3 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold rounded-lg text-sm transition"
            >
              Continue to Password Reset Form &rarr;
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Registered Email
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@utangnaloob.ph"
              className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl transition disabled:opacity-50"
          >
            {loading ? 'Generating Token...' : 'Send Password Reset Token'}
          </button>
        </form>

        <div className="pt-4 border-t border-slate-800 flex justify-between text-sm text-slate-400">
          <button
            type="button"
            onClick={() => onNavigate('/login')}
            className="text-emerald-400 hover:text-emerald-300"
          >
            &larr; Back to Sign In
          </button>
          <button
            type="button"
            onClick={() => onNavigate('/reset-password')}
            className="text-slate-400 hover:text-slate-200"
          >
            Already have a token?
          </button>
        </div>
      </div>
    </div>
  );
}
