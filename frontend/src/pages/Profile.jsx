import React, { useState, useEffect } from 'react';
import { userApi, authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function Profile({ onNavigate }) {
  const { user: authUser, getDashboardRoute } = useAuth();
  const [activeTab, setActiveTab] = useState('profile'); // 'profile' | 'password'

  // Profile State
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    dateOfBirth: '',
    address: '',
    employmentStatus: 'EMPLOYED',
    monthlyIncome: 0
  });
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMessage, setProfileMessage] = useState({ type: '', text: '' });

  // Change Password State
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    setProfileLoading(true);
    setProfileMessage({ type: '', text: '' });
    try {
      const data = await userApi.getProfile();
      setProfile(data);
      setFormData({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        phoneNumber: data.phoneNumber || '',
        dateOfBirth: data.dateOfBirth || '',
        address: data.address || '',
        employmentStatus: data.employmentStatus || 'EMPLOYED',
        monthlyIncome: data.monthlyIncome || 0
      });
    } catch (err) {
      setProfileMessage({ type: 'error', text: err.message || 'Failed to load profile details.' });
    } finally {
      setProfileLoading(false);
    }
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'monthlyIncome' ? (parseFloat(value) || 0) : value
    }));
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileMessage({ type: '', text: '' });

    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setProfileMessage({ type: 'error', text: 'First name and last name are required.' });
      return;
    }

    setProfileSaving(true);
    try {
      const updated = await userApi.updateProfile(formData);
      setProfile(updated);
      setProfileMessage({ type: 'success', text: 'Profile information updated successfully in database!' });
    } catch (err) {
      setProfileMessage({ type: 'error', text: err.message || 'Failed to update profile.' });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordMessage({ type: '', text: '' });

    if (!passwordData.currentPassword) {
      setPasswordMessage({ type: 'error', text: 'Current password is required.' });
      return;
    }
    if (passwordData.newPassword.length < 6) {
      setPasswordMessage({ type: 'error', text: 'New password must be at least 6 characters.' });
      return;
    }
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordMessage({ type: 'error', text: 'New password and confirmation password do not match.' });
      return;
    }
    if (passwordData.newPassword === passwordData.currentPassword) {
      setPasswordMessage({ type: 'error', text: 'New password cannot be identical to your current password.' });
      return;
    }

    setPasswordSaving(true);
    try {
      const res = await authApi.changePassword(passwordData);
      setPasswordMessage({ type: 'success', text: res.message || 'Password changed successfully!' });
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPasswordMessage({ type: 'error', text: err.message || 'Failed to update password.' });
    } finally {
      setPasswordSaving(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">User Account Management</h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage your personal profile details, identity parameters, and account security
          </p>
        </div>
        <button
          onClick={() => onNavigate(getDashboardRoute())}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-sm font-medium border border-slate-700 transition"
        >
          &larr; Back to Dashboard
        </button>
      </div>

      {/* Tabs */}
      <div className="flex space-x-2 mt-6 border-b border-slate-800">
        <button
          onClick={() => setActiveTab('profile')}
          className={`px-5 py-3 font-semibold text-sm border-b-2 transition ${
            activeTab === 'profile'
              ? 'border-emerald-500 text-emerald-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Profile Information
        </button>
        <button
          onClick={() => setActiveTab('password')}
          className={`px-5 py-3 font-semibold text-sm border-b-2 transition ${
            activeTab === 'password'
              ? 'border-emerald-500 text-emerald-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Change Password & Security
        </button>
      </div>

      {/* Tab 1: Profile View & Edit */}
      {activeTab === 'profile' && (
        <div className="mt-6 space-y-6">
          {profileMessage.text && (
            <div className={`p-4 rounded-xl text-sm ${
              profileMessage.type === 'success'
                ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300'
                : 'bg-rose-500/10 border border-rose-500/30 text-rose-300'
            }`}>
              {profileMessage.text}
            </div>
          )}

          {profileLoading ? (
            <div className="text-center py-12 text-slate-400">Loading profile information...</div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Account Overview Sidebar Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-5 h-fit">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center font-bold text-xl text-slate-950">
                    {formData.firstName?.[0] || 'U'}{formData.lastName?.[0] || ''}
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">{formData.firstName} {formData.lastName}</h3>
                    <p className="text-xs text-slate-400">{profile?.email}</p>
                    <span className="inline-block mt-1 text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                      {profile?.accountStatus || 'ACTIVE'}
                    </span>
                  </div>
                </div>

                <div className="border-t border-slate-800 pt-4 space-y-3 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Assigned Roles:</span>
                    <span className="text-white font-medium">{profile?.roles?.join(', ')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">KYC Status:</span>
                    <span className="text-amber-400 font-medium">{profile?.kycStatus || 'NOT_SUBMITTED'}</span>
                  </div>
                  {profile?.approvedCreditLimit !== undefined && (
                    <div className="flex justify-between">
                      <span className="text-slate-400">Credit Limit:</span>
                      <span className="text-emerald-400 font-bold">₱{Number(profile.approvedCreditLimit).toLocaleString()}</span>
                    </div>
                  )}
                  {profile?.availableBalance !== undefined && (
                    <div className="flex justify-between">
                      <span className="text-slate-400">Available Headroom:</span>
                      <span className="text-teal-300 font-bold">₱{Number(profile.availableBalance).toLocaleString()}</span>
                    </div>
                  )}
                </div>

                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 text-[11px] text-slate-400">
                  <strong className="text-slate-300">Privacy & Security Guard:</strong> Authenticated profile modifications are strictly restricted to your own verified credentials.
                </div>
              </div>

              {/* Profile Edit Form */}
              <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-3xl p-6">
                <h3 className="text-lg font-bold text-white mb-4">Edit Profile Information</h3>
                <form onSubmit={handleProfileSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        First Name *
                      </label>
                      <input
                        type="text"
                        name="firstName"
                        required
                        value={formData.firstName}
                        onChange={handleProfileChange}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Last Name *
                      </label>
                      <input
                        type="text"
                        name="lastName"
                        required
                        value={formData.lastName}
                        onChange={handleProfileChange}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Email Address (Permanent Identifier)
                    </label>
                    <input
                      type="email"
                      disabled
                      value={profile?.email || ''}
                      className="w-full px-4 py-2.5 bg-slate-950/60 border border-slate-800 rounded-xl text-slate-400 cursor-not-allowed"
                    />
                    <span className="text-[11px] text-slate-500 mt-1 block">
                      Email address is tied to your cryptographic security token and cannot be modified.
                    </span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Phone Number
                      </label>
                      <input
                        type="text"
                        name="phoneNumber"
                        value={formData.phoneNumber}
                        onChange={handleProfileChange}
                        placeholder="09171234567"
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Date of Birth
                      </label>
                      <input
                        type="date"
                        name="dateOfBirth"
                        value={formData.dateOfBirth}
                        onChange={handleProfileChange}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Complete Address
                    </label>
                    <textarea
                      name="address"
                      rows="2"
                      value={formData.address}
                      onChange={handleProfileChange}
                      placeholder="Street, Barangay, City, Province"
                      className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Employment Status
                      </label>
                      <select
                        name="employmentStatus"
                        value={formData.employmentStatus}
                        onChange={handleProfileChange}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      >
                        <option value="EMPLOYED">Employed (Full-Time / Part-Time)</option>
                        <option value="SELF_EMPLOYED">Self-Employed / Business Owner</option>
                        <option value="FREELANCER">Freelancer / Gig Worker</option>
                        <option value="UNEMPLOYED">Unemployed</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Monthly Income (PHP ₱)
                      </label>
                      <input
                        type="number"
                        min="0"
                        step="1000"
                        name="monthlyIncome"
                        value={formData.monthlyIncome}
                        onChange={handleProfileChange}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                  </div>

                  <div className="pt-4 flex justify-end">
                    <button
                      type="submit"
                      disabled={profileSaving}
                      className="px-6 py-3 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg transition disabled:opacity-50"
                    >
                      {profileSaving ? 'Saving Changes...' : 'Save Profile Changes'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Change Password */}
      {activeTab === 'password' && (
        <div className="mt-6 max-w-xl mx-auto bg-slate-900 border border-slate-800 rounded-3xl p-8 space-y-6">
          <div>
            <h3 className="text-xl font-bold text-white">Change Account Password</h3>
            <p className="text-xs text-slate-400 mt-1">
              Verify your existing password and specify a strong new credential
            </p>
          </div>

          {passwordMessage.text && (
            <div className={`p-4 rounded-xl text-sm ${
              passwordMessage.type === 'success'
                ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300'
                : 'bg-rose-500/10 border border-rose-500/30 text-rose-300'
            }`}>
              {passwordMessage.text}
            </div>
          )}

          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Current Password *
              </label>
              <input
                type="password"
                required
                value={passwordData.currentPassword}
                onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
                placeholder="Enter current password"
                className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                New Password * (Minimum 6 characters)
              </label>
              <input
                type="password"
                required
                value={passwordData.newPassword}
                onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                placeholder="••••••••"
                className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Confirm New Password *
              </label>
              <input
                type="password"
                required
                value={passwordData.confirmPassword}
                onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
                placeholder="••••••••"
                className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>

            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 text-[11px] text-slate-400">
              Upon successful update, your previous password will be invalidated immediately. All future sign-ins will require your new password.
            </div>

            <button
              type="submit"
              disabled={passwordSaving}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg transition disabled:opacity-50"
            >
              {passwordSaving ? 'Verifying & Updating...' : 'Update Password'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
