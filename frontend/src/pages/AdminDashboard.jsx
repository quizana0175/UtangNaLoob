import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminApi } from '../services/api';

export default function AdminDashboard() {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [users, setUsers] = useState([]);
  const [parameters, setParameters] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [activeTab, setActiveTab] = useState('portfolio'); // portfolio, users, parameters, logs
  const [loading, setLoading] = useState(true);
  const [actionMsg, setActionMsg] = useState(null);

  // Edit parameter state
  const [editingParam, setEditingParam] = useState(null);
  const [paramValueInput, setParamValueInput] = useState('');

  const loadAllData = async () => {
    try {
      const [m, u, p, l] = await Promise.all([
        adminApi.getMetrics(),
        adminApi.getUsers(),
        adminApi.getParameters(),
        adminApi.getAuditLogs()
      ]);
      setMetrics(m);
      setUsers(u);
      setParameters(p);
      setAuditLogs(l);
    } catch (err) {
      console.error('Failed to load admin data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  const handleToggleStatus = async (userId) => {
    setActionMsg(null);
    try {
      await adminApi.toggleUserStatus(userId);
      setActionMsg({ type: 'success', text: 'User status successfully toggled.' });
      const updatedUsers = await adminApi.getUsers();
      setUsers(updatedUsers);
      const updatedLogs = await adminApi.getAuditLogs();
      setAuditLogs(updatedLogs);
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to toggle user status.' });
    }
  };

  const handleRoleChange = async (userId, newRole) => {
    setActionMsg(null);
    try {
      await adminApi.updateUserRoles(userId, [newRole]);
      setActionMsg({ type: 'success', text: `User role updated to ${newRole}.` });
      const updatedUsers = await adminApi.getUsers();
      setUsers(updatedUsers);
      const updatedLogs = await adminApi.getAuditLogs();
      setAuditLogs(updatedLogs);
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to update user role.' });
    }
  };

  const handleStartEditParam = (param) => {
    setEditingParam(param);
    setParamValueInput(param.paramValue);
  };

  const handleSaveParam = async () => {
    if (!editingParam) return;
    setActionMsg(null);
    try {
      await adminApi.updateParameter(editingParam.paramKey, paramValueInput);
      setActionMsg({ type: 'success', text: `Parameter '${editingParam.paramKey}' updated to ${paramValueInput}.` });
      setEditingParam(null);
      const updatedParams = await adminApi.getParameters();
      setParameters(updatedParams);
      const updatedLogs = await adminApi.getAuditLogs();
      setAuditLogs(updatedLogs);
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to update parameter.' });
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-16 text-center">
        <div className="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
        <p className="text-slate-400">Loading system administration console...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="bg-gradient-to-r from-slate-900 via-purple-950/40 to-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-xl">
        <div>
          <div className="flex items-center space-x-3 mb-1">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              System Administration & Financial Control
            </h1>
            <span className="text-xs px-2.5 py-1 rounded-full font-semibold border bg-purple-500/10 text-purple-400 border-purple-500/30">
              Admin
            </span>
          </div>
          <p className="text-slate-400 text-sm">
            Financial parameter formulas, user access control, and platform audit compliance
          </p>
        </div>

        <div className="flex bg-slate-950/80 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('portfolio')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'portfolio' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Portfolio
          </button>
          <button
            onClick={() => setActiveTab('users')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'users' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Users ({users.length})
          </button>
          <button
            onClick={() => setActiveTab('parameters')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'parameters' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Parameters
          </button>
          <button
            onClick={() => setActiveTab('logs')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'logs' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Audit Logs
          </button>
        </div>
      </div>

      {actionMsg && (
        <div className={`p-4 rounded-xl border text-sm flex items-center space-x-3 ${
          actionMsg.type === 'success'
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
        }`}>
          <span className="font-bold">{actionMsg.type === 'success' ? '✓' : '!'}</span>
          <span>{actionMsg.text}</span>
        </div>
      )}

      {/* Tab 1: Portfolio Performance KPIs */}
      {activeTab === 'portfolio' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                Total Portfolio Approved Limit
              </span>
              <span className="text-2xl font-black text-white">
                ₱ {(metrics?.totalApprovedCredit || 0).toLocaleString()}
              </span>
              <span className="text-xs text-emerald-400 block mt-2">Active Credit Lines: {metrics?.activeCreditLines}</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                Active Drawn Principal
              </span>
              <span className="text-2xl font-black text-amber-400">
                ₱ {(metrics?.totalDrawnBalance || 0).toLocaleString()}
              </span>
              <span className="text-xs text-slate-400 block mt-2">Disbursed cash advances</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                Repayment Recovery Rate
              </span>
              <span className="text-2xl font-black text-emerald-400">
                {metrics?.repaymentRecoveryRate}%
              </span>
              <span className="text-xs text-slate-400 block mt-2">Target &gt; 95.0%</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                Non-Performing Loans (NPL)
              </span>
              <span className="text-2xl font-black text-rose-400">
                {metrics?.nonPerformingLoanRatio}%
              </span>
              <span className="text-xs text-slate-400 block mt-2">Past-due defaults</span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg text-center">
              <span className="text-xs text-slate-400 uppercase font-semibold block mb-1">Total Registered Users</span>
              <span className="text-3xl font-extrabold text-white">{metrics?.totalUsers}</span>
            </div>
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg text-center">
              <span className="text-xs text-slate-400 uppercase font-semibold block mb-1">Total Borrowers</span>
              <span className="text-3xl font-extrabold text-emerald-400">{metrics?.totalBorrowers}</span>
            </div>
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg text-center">
              <span className="text-xs text-slate-400 uppercase font-semibold block mb-1">Pending KYC Queue</span>
              <span className="text-3xl font-extrabold text-amber-400">{metrics?.pendingKycCount}</span>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: User Accounts & Roles Management */}
      {activeTab === 'users' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-white">Registered User Accounts & Access Control</h2>
              <p className="text-xs text-slate-400">View registered users, assign roles, and toggle account access</p>
            </div>
            <button
              onClick={loadAllData}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
            >
              Refresh
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/60 text-xs uppercase text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4 font-semibold">User</th>
                  <th className="py-3 px-4 font-semibold">Contact</th>
                  <th className="py-3 px-4 font-semibold">Current Role</th>
                  <th className="py-3 px-4 font-semibold">Status</th>
                  <th className="py-3 px-4 font-semibold">Change Role</th>
                  <th className="py-3 px-4 font-semibold text-right">Access Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {users.map((u) => {
                  const roleStr = u.roles?.includes('ROLE_ADMIN') ? 'ADMIN'
                    : u.roles?.includes('ROLE_UNDERWRITER') ? 'UNDERWRITER' : 'BORROWER';

                  return (
                    <tr key={u.id} className="hover:bg-slate-800/40 transition">
                      <td className="py-3.5 px-4 font-medium text-white">
                        <div>{u.firstName} {u.lastName}</div>
                        <div className="text-xs text-slate-400">{u.email}</div>
                      </td>
                      <td className="py-3.5 px-4 text-xs">
                        <div>{u.phoneNumber || 'N/A'}</div>
                        <div className="text-slate-400">{u.employmentStatus || 'N/A'}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                          roleStr === 'ADMIN' ? 'bg-purple-500/10 text-purple-400 border-purple-500/30'
                          : roleStr === 'UNDERWRITER' ? 'bg-blue-500/10 text-blue-400 border-blue-500/30'
                          : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                        }`}>
                          {roleStr}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`text-xs px-2 py-0.5 rounded font-medium ${
                          u.accountStatus === 'ACTIVE'
                            ? 'bg-emerald-500/10 text-emerald-400'
                            : 'bg-rose-500/10 text-rose-400'
                        }`}>
                          {u.accountStatus}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-xs">
                        <select
                          value={roleStr}
                          onChange={(e) => handleRoleChange(u.id, e.target.value)}
                          className="px-2.5 py-1 bg-slate-950 border border-slate-700 rounded-lg text-slate-200 text-xs focus:ring-1 focus:ring-purple-500"
                        >
                          <option value="BORROWER">Borrower</option>
                          <option value="UNDERWRITER">Underwriter</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => handleToggleStatus(u.id)}
                          className={`px-3 py-1 rounded-lg text-xs font-semibold border transition ${
                            u.accountStatus === 'ACTIVE'
                              ? 'bg-rose-950/40 text-rose-300 border-rose-800/40 hover:bg-rose-900/60'
                              : 'bg-emerald-950/40 text-emerald-300 border-emerald-800/40 hover:bg-emerald-900/60'
                          }`}
                        >
                          {u.accountStatus === 'ACTIVE' ? 'Lock Account' : 'Activate Account'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 3: Financial Parameters */}
      {activeTab === 'parameters' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-white">System-Wide Financial Parameters Configuration</h2>
              <p className="text-xs text-slate-400">Configure interest rates, grace periods, penalty formulas, and loan limits</p>
            </div>
            <button
              onClick={loadAllData}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
            >
              Refresh
            </button>
          </div>

          <div className="space-y-4">
            {parameters.map((param) => (
              <div key={param.id} className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-bold text-purple-400 font-mono">{param.paramKey}</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                      {param.paramValue}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">{param.description}</p>
                  <span className="text-[10px] text-slate-500 block">
                    Last updated by: {param.updatedBy || 'SYSTEM'}
                  </span>
                </div>

                <div>
                  <button
                    onClick={() => handleStartEditParam(param)}
                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-purple-300 rounded-lg text-xs font-semibold border border-purple-500/20 transition"
                  >
                    Edit Value
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Edit Parameter Modal */}
          {editingParam && (
            <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
              <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl">
                <h3 className="text-lg font-bold text-white mb-2">Edit System Parameter</h3>
                <p className="text-xs text-slate-400 font-mono mb-4">{editingParam.paramKey}</p>

                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                      New Parameter Value
                    </label>
                    <input
                      type="text"
                      value={paramValueInput}
                      onChange={(e) => setParamValueInput(e.target.value)}
                      className="w-full px-3.5 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                    />
                  </div>

                  <div className="flex justify-end space-x-3 pt-2">
                    <button
                      type="button"
                      onClick={() => setEditingParam(null)}
                      className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={handleSaveParam}
                      className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold shadow-md shadow-purple-600/20"
                    >
                      Save Parameter
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 4: Audit Logs */}
      {activeTab === 'logs' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-white">System Security & Operational Audit Trail</h2>
              <p className="text-xs text-slate-400">Chronological log of authentication, role mutations, and KYC reviews</p>
            </div>
            <button
              onClick={loadAllData}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
            >
              Refresh
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/60 text-xs uppercase text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4 font-semibold">Timestamp</th>
                  <th className="py-3 px-4 font-semibold">Action</th>
                  <th className="py-3 px-4 font-semibold">Actor</th>
                  <th className="py-3 px-4 font-semibold">Target User</th>
                  <th className="py-3 px-4 font-semibold">Details</th>
                  <th className="py-3 px-4 font-semibold">IP Address</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/40 text-xs transition">
                    <td className="py-3 px-4 text-slate-400 whitespace-nowrap">
                      {log.timestamp ? new Date(log.timestamp).toLocaleString() : 'Just now'}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-mono font-semibold px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-purple-300">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-300 font-medium">{log.performedBy}</td>
                    <td className="py-3 px-4 text-slate-400">{log.targetUser || '---'}</td>
                    <td className="py-3 px-4 text-slate-300 max-w-sm truncate">{log.details}</td>
                    <td className="py-3 px-4 text-slate-500 font-mono">{log.ipAddress || '127.0.0.1'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
