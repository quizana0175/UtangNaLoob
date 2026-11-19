import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { borrowerApi } from '../services/api';

export default function BorrowerDashboard() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loans, setLoans] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionMsg, setActionMsg] = useState(null);

  // KYC Modal
  const [showKycModal, setShowKycModal] = useState(false);
  const [kycForm, setKycForm] = useState({
    idType: 'UMID',
    idNumber: '',
    idDocumentUrl: 'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=600&q=80',
    proofOfIncomeType: 'PAYSLIP',
    proofOfIncomeUrl: 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=600&q=80',
    monthlyIncome: 35000
  });
  const [isSubmittingKyc, setIsSubmittingKyc] = useState(false);

  // Loan Request Modal
  const [showLoanModal, setShowLoanModal] = useState(false);
  const [loanForm, setLoanForm] = useState({
    amount: 5000,
    installmentTermMonths: 3,
    repaymentFrequency: 'MONTHLY',
    loanPurpose: 'Personal / Emergency Cash Advance'
  });
  const [isSubmittingLoan, setIsSubmittingLoan] = useState(false);

  // Digital Repayment Modal
  const [selectedInstallment, setSelectedInstallment] = useState(null);
  const [selectedLoan, setSelectedLoan] = useState(null);
  const [payMethod, setPayMethod] = useState('GCASH');
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false);
  const [paymentReceipt, setPaymentReceipt] = useState(null);

  const loadData = async () => {
    try {
      const [prof, userLoans, userPayments] = await Promise.all([
        borrowerApi.getDashboard(),
        borrowerApi.getMyLoans().catch(() => []),
        borrowerApi.getPaymentHistory().catch(() => [])
      ]);
      setProfile(prof);
      setLoans(userLoans);
      setPayments(userPayments);
      if (prof.monthlyIncome) {
        setKycForm(prev => ({ ...prev, monthlyIncome: prof.monthlyIncome }));
      }
    } catch (err) {
      console.error('Failed to load borrower data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleKycSubmit = async (e) => {
    e.preventDefault();
    setIsSubmittingKyc(true);
    setActionMsg(null);
    try {
      const res = await borrowerApi.submitKyc(kycForm);
      setActionMsg({ type: 'success', text: res.message });
      setShowKycModal(false);
      await loadData();
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to submit KYC documentation.' });
    } finally {
      setIsSubmittingKyc(false);
    }
  };

  const handleLoanSubmit = async (e) => {
    e.preventDefault();
    setIsSubmittingLoan(true);
    setActionMsg(null);
    try {
      const res = await borrowerApi.requestLoan(loanForm);
      setActionMsg({
        type: 'success',
        text: `Cash loan disbursement of ₱${res.principalAmount?.toLocaleString()} approved! Total repayment: ₱${res.totalRepaymentAmount?.toLocaleString()} across ${res.installments?.length} installments.`
      });
      setShowLoanModal(false);
      await loadData();
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Failed to process loan request.' });
    } finally {
      setIsSubmittingLoan(false);
    }
  };

  const handlePayInstallment = async (e) => {
    e.preventDefault();
    if (!selectedLoan || !selectedInstallment) return;

    setIsSubmittingPayment(true);
    setActionMsg(null);
    try {
      const res = await borrowerApi.payInstallment({
        loanId: selectedLoan.id,
        installmentId: selectedInstallment.id,
        amount: selectedInstallment.totalDue,
        paymentMethod: payMethod
      });
      setPaymentReceipt(res);
      setSelectedInstallment(null);
      setSelectedLoan(null);
      setActionMsg({
        type: 'success',
        text: `Payment of ₱${res.amountPaid?.toLocaleString()} via ${res.paymentMethod} verified! Reference: ${res.transactionReference}. Available revolving credit limit instantly replenished!`
      });
      await loadData();
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Digital payment processing failed.' });
    } finally {
      setIsSubmittingPayment(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-16 text-center">
        <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
        <p className="text-slate-400">Loading your revolving credit dashboard...</p>
      </div>
    );
  }

  const approvedLimit = profile?.approvedCreditLimit || 0;
  const availableBalance = profile?.availableBalance || 0;
  const drawnBalance = profile?.drawnBalance || 0;
  const kycStatus = profile?.kycStatus || 'NOT_SUBMITTED';
  const isKycVerified = kycStatus === 'VERIFIED';

  // Preview computations for loan request modal
  const reqAmount = Number(loanForm.amount) || 1000;
  const reqTerm = Number(loanForm.installmentTermMonths) || 3;
  const reqFreq = loanForm.repaymentFrequency;
  const reqInterest = reqAmount * 0.02 * reqTerm;
  const reqFee = Math.max(150, reqAmount * 0.015);
  const reqTotal = reqAmount + reqInterest + reqFee;
  const reqInstallmentsCount = reqFreq === 'BI_WEEKLY' ? reqTerm * 2 : reqTerm;
  const reqInstallmentDue = reqTotal / reqInstallmentsCount;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-xl">
        <div>
          <div className="flex items-center space-x-3 mb-1">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Kumusta, {profile?.firstName || user?.firstName}!
            </h1>
            <span className={`text-xs px-2.5 py-1 rounded-full font-semibold border ${
              profile?.creditLineStatus === 'ACTIVE'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
            }`}>
              {profile?.creditLineStatus || 'PENDING'}
            </span>
          </div>
          <p className="text-slate-400 text-sm">
            Utang na Loob Revolving Credit Portal & Transparent Amortization Tracker
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {!isKycVerified ? (
            <button
              onClick={() => setShowKycModal(true)}
              className="px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg shadow-emerald-500/20 text-sm transition"
            >
              {kycStatus === 'PENDING' ? 'Update KYC Documents' : 'Submit KYC Documents'}
            </button>
          ) : (
            <button
              onClick={() => {
                setLoanForm(prev => ({ ...prev, amount: Math.min(5000, availableBalance > 1000 ? availableBalance : 1000) }));
                setShowLoanModal(true);
              }}
              disabled={availableBalance < 1000}
              className="px-5 py-2.5 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-xl shadow-lg shadow-emerald-500/20 text-sm transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              + Request Cash Loan Advance
            </button>
          )}
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

      {/* Credit Balance Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            Approved Revolving Credit Limit
          </div>
          <div className="text-3xl font-black text-white">
            ₱ {approvedLimit.toLocaleString('en-PH', { minimumFractionDigits: 2 })}
          </div>
          <div className="mt-4 flex items-center justify-between text-xs text-slate-400 pt-3 border-t border-slate-800">
            <span>Revolving Ceiling</span>
            <span className="text-emerald-400 font-medium">Revolving</span>
          </div>
        </div>

        <div className="bg-slate-900 border border-emerald-500/30 rounded-2xl p-6 shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none"></div>
          <div className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2">
            Available Spending Balance
          </div>
          <div className="text-3xl font-black text-emerald-400">
            ₱ {availableBalance.toLocaleString('en-PH', { minimumFractionDigits: 2 })}
          </div>
          <div className="mt-4 flex items-center justify-between text-xs text-slate-400 pt-3 border-t border-slate-800">
            <span>Instant Drawdown Ready</span>
            <span className="text-emerald-400 font-semibold">Ready</span>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-lg">
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            Outstanding Drawn Balance
          </div>
          <div className="text-3xl font-black text-amber-400">
            ₱ {drawnBalance.toLocaleString('en-PH', { minimumFractionDigits: 2 })}
          </div>
          <div className="mt-4 flex items-center justify-between text-xs text-slate-400 pt-3 border-t border-slate-800">
            <span>Restored upon repayment</span>
            <span className="text-amber-400 font-medium">Active Dues</span>
          </div>
        </div>
      </div>

      {/* Active Loans & Amortization Schedules */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-6">
          <div>
            <h2 className="text-lg font-bold text-white">My Active Loan Disbursements & Amortization</h2>
            <p className="text-xs text-slate-400">Track binding installment schedules and settle dues online</p>
          </div>
          {isKycVerified && (
            <button
              onClick={() => {
                setLoanForm(prev => ({ ...prev, amount: Math.min(5000, availableBalance > 1000 ? availableBalance : 1000) }));
                setShowLoanModal(true);
              }}
              disabled={availableBalance < 1000}
              className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition disabled:opacity-50"
            >
              + Draw Cash Advance
            </button>
          )}
        </div>

        {loans.length === 0 ? (
          <div className="py-10 text-center border border-dashed border-slate-800 rounded-xl">
            <p className="text-slate-400 text-sm">No cash loans drawn yet.</p>
            {isKycVerified ? (
              <button
                onClick={() => setShowLoanModal(true)}
                className="mt-3 px-4 py-2 bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs"
              >
                Request First Loan Drawdown
              </button>
            ) : (
              <p className="text-xs text-amber-400 mt-2">Complete KYC verification above to unlock cash disbursements.</p>
            )}
          </div>
        ) : (
          <div className="space-y-6">
            {loans.map((loan) => (
              <div key={loan.id} className="bg-slate-950 border border-slate-800 rounded-xl p-5">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-slate-800 gap-2 mb-4">
                  <div>
                    <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider block">
                      Loan #{loan.id} • {loan.loanPurpose}
                    </span>
                    <span className="text-sm font-semibold text-white">
                      Principal: ₱{loan.principalAmount?.toLocaleString()} • Total Repayment: ₱{loan.totalRepaymentAmount?.toLocaleString()}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs px-2.5 py-1 rounded-full font-bold ${
                      loan.status === 'PAID'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                        : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                    }`}>
                      {loan.status}
                    </span>
                    <span className="text-xs text-slate-400 block mt-1">
                      Outstanding: ₱{loan.outstandingBalance?.toLocaleString()}
                    </span>
                  </div>
                </div>

                {/* Installments Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-900 text-slate-400 uppercase">
                      <tr>
                        <th className="py-2.5 px-3">#</th>
                        <th className="py-2.5 px-3">Due Date</th>
                        <th className="py-2.5 px-3">Principal</th>
                        <th className="py-2.5 px-3">Interest</th>
                        <th className="py-2.5 px-3">Total Due</th>
                        <th className="py-2.5 px-3">Status</th>
                        <th className="py-2.5 px-3 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {loan.installments?.map((inst) => (
                        <tr key={inst.id} className="hover:bg-slate-900/40">
                          <td className="py-2.5 px-3 font-semibold">{inst.installmentNumber}</td>
                          <td className="py-2.5 px-3">{inst.dueDate}</td>
                          <td className="py-2.5 px-3">₱{inst.principalDue?.toLocaleString()}</td>
                          <td className="py-2.5 px-3">₱{inst.interestDue?.toLocaleString()}</td>
                          <td className="py-2.5 px-3 font-bold text-white">₱{inst.totalDue?.toLocaleString()}</td>
                          <td className="py-2.5 px-3">
                            <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                              inst.status === 'PAID'
                                ? 'bg-emerald-500/10 text-emerald-400'
                                : 'bg-amber-500/10 text-amber-400'
                            }`}>
                              {inst.status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-right">
                            {inst.status !== 'PAID' ? (
                              <button
                                onClick={() => {
                                  setSelectedLoan(loan);
                                  setSelectedInstallment(inst);
                                }}
                                className="px-3 py-1 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-bold rounded-lg text-xs transition"
                              >
                                Pay Online
                              </button>
                            ) : (
                              <span className="text-slate-500 text-[11px]">Paid</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Payment History / Digital Ledger */}
      {payments.length > 0 && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
          <h2 className="text-lg font-bold text-white mb-4">Repayment History & Settlement Ledger</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 uppercase">
                <tr>
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3">Transaction Reference</th>
                  <th className="py-2.5 px-3">Payment Method</th>
                  <th className="py-2.5 px-3">Amount Settled</th>
                  <th className="py-2.5 px-3">Credit Line Impact</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {payments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/40">
                    <td className="py-2.5 px-3">{new Date(p.paidAt).toLocaleDateString()}</td>
                    <td className="py-2.5 px-3 font-mono text-emerald-400">{p.transactionReference}</td>
                    <td className="py-2.5 px-3 font-semibold">{p.paymentMethod}</td>
                    <td className="py-2.5 px-3 font-bold text-white">₱{p.amount?.toLocaleString()}</td>
                    <td className="py-2.5 px-3 text-emerald-400 font-medium">+₱{p.amount?.toLocaleString()} Replenished</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Lending Request Modal */}
      {showLoanModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-white">Request Cash Advance</h3>
              <button onClick={() => setShowLoanModal(false)} className="text-slate-400 hover:text-white font-bold text-lg">✕</button>
            </div>

            <form onSubmit={handleLoanSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Disbursement Amount (PHP) *
                </label>
                <input
                  type="number"
                  required
                  min="1000"
                  max={availableBalance}
                  step="500"
                  value={loanForm.amount}
                  onChange={(e) => setLoanForm({ ...loanForm, amount: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                />
                <span className="text-xs text-slate-400 mt-1 block">
                  Available spending limit: ₱{availableBalance.toLocaleString()} (Min: ₱1,000)
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                    Duration (Months)
                  </label>
                  <select
                    value={loanForm.installmentTermMonths}
                    onChange={(e) => setLoanForm({ ...loanForm, installmentTermMonths: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                  >
                    <option value={1}>1 Month</option>
                    <option value={2}>2 Months</option>
                    <option value={3}>3 Months</option>
                    <option value={6}>6 Months</option>
                    <option value={12}>12 Months</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                    Repayment Frequency
                  </label>
                  <select
                    value={loanForm.repaymentFrequency}
                    onChange={(e) => setLoanForm({ ...loanForm, repaymentFrequency: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                  >
                    <option value="BI_WEEKLY">Bi-Weekly (Every 15 Days)</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Loan Purpose / Note
                </label>
                <input
                  type="text"
                  value={loanForm.loanPurpose}
                  onChange={(e) => setLoanForm({ ...loanForm, loanPurpose: e.target.value })}
                  placeholder="e.g. Utility bills, groceries, emergency"
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                />
              </div>

              {/* Real-time Calculation Breakdown */}
              <div className="p-4 bg-slate-950 border border-slate-800 rounded-xl space-y-2 text-xs">
                <div className="flex justify-between text-slate-400">
                  <span>Interest (2.0% monthly amortized):</span>
                  <span className="text-slate-200">₱{reqInterest.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Processing Fee (1.5%):</span>
                  <span className="text-slate-200">₱{reqFee.toFixed(2)}</span>
                </div>
                <div className="flex justify-between font-bold text-white pt-2 border-t border-slate-800">
                  <span>Total Repayment:</span>
                  <span className="text-emerald-400">₱{reqTotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between font-bold text-emerald-400">
                  <span>Due per Installment ({reqInstallmentsCount} payments):</span>
                  <span>₱{reqInstallmentDue.toFixed(2)}</span>
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowLoanModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingLoan || reqAmount > availableBalance || reqAmount < 1000}
                  className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 rounded-xl text-xs font-bold shadow-md shadow-emerald-500/20 disabled:opacity-50"
                >
                  {isSubmittingLoan ? 'Processing...' : 'Confirm Loan Disbursement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Digital Repayment Modal */}
      {selectedInstallment && selectedLoan && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-md w-full p-6 shadow-2xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold text-white">Online Digital Repayment</h3>
              <button
                onClick={() => { setSelectedInstallment(null); setSelectedLoan(null); }}
                className="text-slate-400 hover:text-white font-bold"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-slate-400 mb-4">
              Repaying Installment #{selectedInstallment.installmentNumber} for Loan #{selectedLoan.id}. Paying will instantly restore your revolving credit line!
            </p>

            <form onSubmit={handlePayInstallment} className="space-y-4">
              <div className="p-4 bg-slate-950 border border-slate-800 rounded-xl text-center">
                <span className="text-xs text-slate-400 block mb-1">Total Installment Amount Due</span>
                <span className="text-2xl font-black text-emerald-400">₱{selectedInstallment.totalDue?.toFixed(2)}</span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">
                  Select Payment Gateway / Method
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {['GCASH', 'MAYA', 'BANK_CARD'].map((method) => (
                    <button
                      key={method}
                      type="button"
                      onClick={() => setPayMethod(method)}
                      className={`p-2.5 rounded-xl border text-xs font-bold text-center transition ${
                        payMethod === method
                          ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                      }`}
                    >
                      {method === 'GCASH' ? 'GCash' : method === 'MAYA' ? 'Maya' : 'Visa / MC'}
                    </button>
                  ))}
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmittingPayment}
                className="w-full py-3 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 text-slate-950 font-bold rounded-xl shadow-lg shadow-emerald-500/20 text-sm transition"
              >
                {isSubmittingPayment ? 'Processing Digital Payment...' : `Confirm Payment of ₱${selectedInstallment.totalDue?.toFixed(2)}`}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* KYC Modal */}
      {showKycModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl relative">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-white">Submit KYC Verification Documents</h3>
              <button onClick={() => setShowKycModal(false)} className="text-slate-400 hover:text-white text-lg font-bold">✕</button>
            </div>

            <form onSubmit={handleKycSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Government ID Type *</label>
                <select
                  value={kycForm.idType}
                  onChange={(e) => setKycForm({ ...kycForm, idType: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                >
                  <option value="UMID">Unified Multi-Purpose ID (UMID)</option>
                  <option value="SSS">Social Security System (SSS)</option>
                  <option value="DRIVERS_LICENSE">Driver's License (LTO)</option>
                  <option value="PASSPORT">Philippine Passport (DFA)</option>
                  <option value="PRC_ID">PRC Professional ID</option>
                  <option value="VOTERS_ID">COMELEC Voter's ID</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Government ID Number *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CRN-0111-2345678-9"
                  value={kycForm.idNumber}
                  onChange={(e) => setKycForm({ ...kycForm, idNumber: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Proof of Income Type *</label>
                <select
                  value={kycForm.proofOfIncomeType}
                  onChange={(e) => setKycForm({ ...kycForm, proofOfIncomeType: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                >
                  <option value="PAYSLIP">Latest 3 Months Payslip</option>
                  <option value="CERTIFICATE_OF_EMPLOYMENT">Certificate of Employment (COE)</option>
                  <option value="ITR">Income Tax Return (BIR Form 2316)</option>
                  <option value="BANK_STATEMENT">Bank Account Statement</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Monthly Income (PHP) *</label>
                <input
                  type="number"
                  required
                  min="1000"
                  step="500"
                  value={kycForm.monthlyIncome}
                  onChange={(e) => setKycForm({ ...kycForm, monthlyIncome: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-white text-sm"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowKycModal(false)}
                  className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-sm font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingKyc}
                  className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 rounded-xl text-sm font-bold shadow-md shadow-emerald-500/20"
                >
                  {isSubmittingKyc ? 'Uploading...' : 'Submit to Underwriter Queue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
