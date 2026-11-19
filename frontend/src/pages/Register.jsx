import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function Register({ onNavigate }) {
  const { register } = useAuth();
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    phoneNumber: '',
    dateOfBirth: '1995-05-15',
    address: '',
    employmentStatus: 'EMPLOYED',
    monthlyIncome: 35000
  });

  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errs = {};
    if (!formData.firstName.trim()) errs.firstName = 'First name is required';
    if (!formData.lastName.trim()) errs.lastName = 'Last name is required';
    if (!formData.email.trim()) {
      errs.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errs.email = 'Please provide a valid email format';
    }
    if (!formData.password) {
      errs.password = 'Password is required';
    } else if (formData.password.length < 6) {
      errs.password = 'Password must be at least 6 characters';
    }
    if (formData.password !== formData.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }
    if (!formData.phoneNumber.trim()) errs.phoneNumber = 'Phone number is required';
    if (!formData.address.trim()) errs.address = 'Residential address is required';
    if (!formData.monthlyIncome || formData.monthlyIncome <= 0) {
      errs.monthlyIncome = 'Declared monthly income must be greater than zero';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'monthlyIncome' ? parseFloat(value) || 0 : value
    }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
    if (serverError) setServerError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccessMsg('');

    if (!validate()) return;

    setLoading(true);
    try {
      const payload = {
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        phoneNumber: formData.phoneNumber.trim(),
        dateOfBirth: formData.dateOfBirth,
        address: formData.address.trim(),
        employmentStatus: formData.employmentStatus,
        monthlyIncome: formData.monthlyIncome
      };

      const res = await register(payload);
      setSuccessMsg(res.message || 'Borrower account registered successfully! Redirecting to login...');
      setTimeout(() => {
        onNavigate('/login');
      }, 1500);
    } catch (err) {
      setServerError(err.message || 'Registration failed. Please check your details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl w-full bg-slate-900 border border-slate-800 p-8 sm:p-10 rounded-3xl shadow-2xl">
        <div className="text-center mb-8">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 font-black text-2xl text-slate-950 mb-3">
            U
          </div>
          <h2 className="text-3xl font-extrabold text-white tracking-tight">
            Create Borrower Account
          </h2>
          <p className="mt-2 text-sm text-slate-400">
            Sign up for digital onboarding, algorithmic revolving credit limits, and flexible installment financing
          </p>
        </div>

        {serverError && (
          <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-start space-x-3">
            <span className="font-bold text-base leading-none">!</span>
            <div className="flex-1">{serverError}</div>
          </div>
        )}

        {successMsg && (
          <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm flex items-start space-x-3">
            <span className="font-bold text-base leading-none">✓</span>
            <div className="flex-1">{successMsg}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                First Name *
              </label>
              <input
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                placeholder="Juan"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.firstName ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.firstName && <p className="text-rose-400 text-xs mt-1">{errors.firstName}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Last Name *
              </label>
              <input
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                placeholder="Dela Cruz"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.lastName ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.lastName && <p className="text-rose-400 text-xs mt-1">{errors.lastName}</p>}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Email Address *
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="juan@example.ph"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.email ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.email && <p className="text-rose-400 text-xs mt-1">{errors.email}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Mobile Number *
              </label>
              <input
                type="text"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                placeholder="09171234567"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.phoneNumber ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.phoneNumber && <p className="text-rose-400 text-xs mt-1">{errors.phoneNumber}</p>}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Password *
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.password ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.password && <p className="text-rose-400 text-xs mt-1">{errors.password}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Confirm Password *
              </label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="••••••••"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.confirmPassword ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.confirmPassword && <p className="text-rose-400 text-xs mt-1">{errors.confirmPassword}</p>}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Employment Status *
              </label>
              <select
                name="employmentStatus"
                value={formData.employmentStatus}
                onChange={handleChange}
                className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="EMPLOYED">Employed (Private/Govt)</option>
                <option value="SELF_EMPLOYED">Self-Employed / Business Owner</option>
                <option value="FREELANCER">Freelancer / Gig Worker</option>
                <option value="OFW">OFW / Remittance Dependent</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Monthly Income (PHP) *
              </label>
              <input
                type="number"
                name="monthlyIncome"
                value={formData.monthlyIncome}
                onChange={handleChange}
                placeholder="35000"
                min="1000"
                step="500"
                className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                  errors.monthlyIncome ? 'border-rose-500' : 'border-slate-700'
                }`}
              />
              {errors.monthlyIncome && <p className="text-rose-400 text-xs mt-1">{errors.monthlyIncome}</p>}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Complete Residential Address *
            </label>
            <input
              type="text"
              name="address"
              value={formData.address}
              onChange={handleChange}
              placeholder="House/Unit #, Street, Barangay, City, Province"
              className={`w-full px-4 py-2.5 bg-slate-950 border rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                errors.address ? 'border-rose-500' : 'border-slate-700'
              }`}
            />
            {errors.address && <p className="text-rose-400 text-xs mt-1">{errors.address}</p>}
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg shadow-emerald-500/20 transition duration-200 disabled:opacity-50"
            >
              {loading ? 'Creating Account...' : 'Register as Borrower'}
            </button>
          </div>
        </form>

        <div className="text-center pt-6 border-t border-slate-800 mt-6">
          <p className="text-sm text-slate-400">
            Already registered?{' '}
            <button
              onClick={() => onNavigate('/login')}
              className="font-semibold text-emerald-400 hover:text-emerald-300 transition"
            >
              Sign In here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
