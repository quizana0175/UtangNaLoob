import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { underwriterApi } from '../services/api';

export default function UnderwriterDashboard() {
  const { user } = useAuth();
  const [queue, setQueue] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedApplicant, setSelectedApplicant] = useState(null);
  const [reviewForm, setReviewForm] = useState({
    decision: 'VERIFIED',
    verifiedMonthlyIncome: '',
    approvedCreditLimit: '',
    notesOrRejectionReason: ''
  });
  const [actionMsg, setActionMsg] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchQueue = async () => {
    try {
      const data = await underwriterApi.getQueue();
      setQueue(data);
    } catch (err) {
      console.error('Failed to fetch underwriter queue:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const handleOpenReview = (applicant) => {
    setSelectedApplicant(applicant);
    const recLimit = Math.min(Math.round((applicant.monthlyIncome || 25000) * 0.40 / 500) * 500, 50000);
    setReviewForm({
      decision: 'VERIFIED',
      verifiedMonthlyIncome: applicant.monthlyIncome || '',
      approvedCreditLimit: recLimit,
      notesOrRejectionReason: ''
    });
    setActionMsg(null);
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!selectedApplicant) return;

    setSubmitting(true);
    setActionMsg(null);
    try {
      const payload = {
        decision: reviewForm.decision,
        verifiedMonthlyIncome: parseFloat(reviewForm.verifiedMonthlyIncome) || null,
        approvedCreditLimit: reviewForm.decision === 'VERIFIED' ? parseFloat(reviewForm.approvedCreditLimit) : null,
        notesOrRejectionReason: reviewForm.notesOrRejectionReason || null
      };

      const res = await underwriterApi.reviewApplicant(selectedApplicant.id, payload);
      setActionMsg({ type: 'success', text: res.message });
      setSelectedApplicant(null);
      await fetchQueue();
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to submit underwriting review.' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="bg-gradient-to-r from-slate-900 via-blue-950/40 to-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-xl">
        <div>
          <div className="flex items-center space-x-3 mb-1">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Underwriting & Credit Risk Console
            </h1>
            <span className="text-xs px-2.5 py-1 rounded-full font-semibold border bg-blue-500/10 text-blue-400 border-blue-500/30">
              Loan Officer
            </span>
          </div>
          <p className="text-slate-400 text-sm">
            Digital applicant verification, algorithmic scoring vetting, and revolving credit limit decisioning
          </p>
        </div>

        <div className="flex items-center space-x-4 text-sm">
          <div className="px-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-center">
            <span className="text-xs text-slate-400 block uppercase">Pending in Queue</span>
            <span className="text-xl font-black text-amber-400">{queue.length}</span>
          </div>
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

      {/* Main Queue Section */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-lg font-bold text-white">Pending KYC Submissions Queue</h2>
            <p className="text-xs text-slate-400">Review documents and assign algorithmic revolving credit lines</p>
          </div>
          <button
            onClick={fetchQueue}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
          >
            Refresh Queue
          </button>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-400">Loading underwriting queue...</div>
        ) : queue.length === 0 ? (
          <div className="py-12 text-center border border-dashed border-slate-800 rounded-xl">
            <div className="text-2xl mb-2 text-emerald-400">✓</div>
            <h3 className="text-base font-semibold text-white">All Caught Up!</h3>
            <p className="text-slate-400 text-xs mt-1">There are no pending applicant KYC verifications in the queue.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/60 text-xs uppercase text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="py-3.5 px-4 font-semibold">Applicant</th>
                  <th className="py-3.5 px-4 font-semibold">Contact & Address</th>
                  <th className="py-3.5 px-4 font-semibold">Employment</th>
                  <th className="py-3.5 px-4 font-semibold">Declared Income</th>
                  <th className="py-3.5 px-4 font-semibold">ID & Proof</th>
                  <th className="py-3.5 px-4 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {queue.map((applicant) => (
                  <tr key={applicant.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-4 px-4 font-medium text-white">
                      <div>{applicant.firstName} {applicant.lastName}</div>
                      <div className="text-xs text-slate-400">{applicant.email}</div>
                    </td>
                    <td className="py-4 px-4 text-xs">
                      <div>{applicant.phoneNumber || 'N/A'}</div>
                      <div className="text-slate-400 truncate max-w-xs">{applicant.address || 'N/A'}</div>
                    </td>
                    <td className="py-4 px-4 text-xs">
                      <span className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300">
                        {applicant.employmentStatus || 'EMPLOYED'}
                      </span>
                    </td>
                    <td className="py-4 px-4 font-semibold text-emerald-400">
                      ₱ {(applicant.monthlyIncome || 0).toLocaleString()}
                    </td>
                    <td className="py-4 px-4 text-xs">
                      <div className="text-slate-300 font-medium">{applicant.idType || 'ID'}</div>
                      <div className="text-slate-400">{applicant.proofOfIncomeType || 'Income Proof'}</div>
                    </td>
                    <td className="py-4 px-4 text-right">
                      <button
                        onClick={() => handleOpenReview(applicant)}
                        className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-semibold shadow-md transition"
                      >
                        Inspect & Review
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delinquency & Portfolio Risk Monitor Preview */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <h2 className="text-lg font-bold text-white mb-2">Automated Delinquency & Penalty Surveillance</h2>
        <p className="text-xs text-slate-400 mb-4">
          Automated daily scheduled routine monitors past-due amortization installments and enforces late fees
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <span className="text-xs text-slate-400 block uppercase">Non-Performing Loans (NPL)</span>
            <span className="text-2xl font-bold text-emerald-400">1.60%</span>
            <span className="text-xs text-slate-500 block mt-1">Healthy threshold &lt; 5.0%</span>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <span className="text-xs text-slate-400 block uppercase">Grace Period Standard</span>
            <span className="text-2xl font-bold text-white">3 Days</span>
            <span className="text-xs text-slate-500 block mt-1">Zero penalty during grace window</span>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <span className="text-xs text-slate-400 block uppercase">Daily Penalty Formula</span>
            <span className="text-2xl font-bold text-amber-400">0.50% / day</span>
            <span className="text-xs text-slate-500 block mt-1">Applied to past-due principal</span>
          </div>
        </div>
      </div>

      {/* Review & Document Inspector Modal */}
      {selectedApplicant && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6 border-b border-slate-800 pb-4">
              <div>
                <h3 className="text-xl font-bold text-white">
                  Underwriter Inspection: {selectedApplicant.firstName} {selectedApplicant.lastName}
                </h3>
                <span className="text-xs text-slate-400">{selectedApplicant.email} • {selectedApplicant.phoneNumber}</span>
              </div>
              <button
                onClick={() => setSelectedApplicant(null)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {/* Document Viewer Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
                <span className="text-xs font-semibold text-slate-300 block mb-2">
                  ID: {selectedApplicant.idType} ({selectedApplicant.idNumber || 'No number'})
                </span>
                <div className="h-44 rounded-lg overflow-hidden bg-slate-900 border border-slate-800 flex items-center justify-center">
                  <img
                    src={selectedApplicant.idDocumentUrl}
                    alt="Government ID"
                    className="w-full h-full object-cover"
                  />
                </div>
              </div>

              <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
                <span className="text-xs font-semibold text-slate-300 block mb-2">
                  Proof of Income: {selectedApplicant.proofOfIncomeType}
                </span>
                <div className="h-44 rounded-lg overflow-hidden bg-slate-900 border border-slate-800 flex items-center justify-center">
                  <img
                    src={selectedApplicant.proofOfIncomeUrl}
                    alt="Proof of Income"
                    className="w-full h-full object-cover"
                  />
                </div>
              </div>
            </div>

            {/* Decision Controls */}
            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                    Decision *
                  </label>
                  <select
                    value={reviewForm.decision}
                    onChange={(e) => setReviewForm({ ...reviewForm, decision: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                  >
                    <option value="VERIFIED">Approve & Activate Credit Line</option>
                    <option value="REJECTED">Reject Application</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                    Verified Monthly Income (₱)
                  </label>
                  <input
                    type="number"
                    value={reviewForm.verifiedMonthlyIncome}
                    onChange={(e) => {
                      const inc = parseFloat(e.target.value) || 0;
                      const rec = Math.min(Math.round(inc * 0.40 / 500) * 500, 50000);
                      setReviewForm({
                        ...reviewForm,
                        verifiedMonthlyIncome: e.target.value,
                        approvedCreditLimit: rec
                      });
                    }}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                  />
                </div>
              </div>

              {reviewForm.decision === 'VERIFIED' && (
                <div>
                  <label className="block text-xs font-semibold text-emerald-400 uppercase mb-1">
                    Approved Revolving Credit Limit (₱) *
                  </label>
                  <input
                    type="number"
                    required
                    min="1000"
                    step="500"
                    value={reviewForm.approvedCreditLimit}
                    onChange={(e) => setReviewForm({ ...reviewForm, approvedCreditLimit: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-emerald-500/50 rounded-xl text-emerald-300 font-bold text-sm"
                  />
                  <p className="text-xs text-slate-400 mt-1">
                    Algorithmic recommendation: 40% of income = ₱ {Math.min(Math.round((parseFloat(reviewForm.verifiedMonthlyIncome) || 0) * 0.40 / 500) * 500, 50000).toLocaleString()}
                  </p>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Underwriter Notes / Rejection Reason
                </label>
                <textarea
                  rows="2"
                  value={reviewForm.notesOrRejectionReason}
                  onChange={(e) => setReviewForm({ ...reviewForm, notesOrRejectionReason: e.target.value })}
                  placeholder="Optional review notes or reason for rejection..."
                  className="w-full px-3.5 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setSelectedApplicant(null)}
                  className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-sm font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className={`px-5 py-2.5 rounded-xl text-sm font-bold shadow-md transition ${
                    reviewForm.decision === 'VERIFIED'
                      ? 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-emerald-500/20'
                      : 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-600/20'
                  }`}
                >
                  {submitting ? 'Processing...' : reviewForm.decision === 'VERIFIED' ? 'Confirm Approval' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
