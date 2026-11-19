const API_BASE_URL = 'http://localhost:8080/api';

export const getAuthToken = () => {
  return localStorage.getItem('token');
};

export const setAuthToken = (token) => {
  if (token) {
    localStorage.setItem('token', token);
  } else {
    localStorage.removeItem('token');
  }
};

export const getStoredUser = () => {
  const userStr = localStorage.getItem('user');
  try {
    return userStr ? JSON.parse(userStr) : null;
  } catch (e) {
    return null;
  }
};

export const setStoredUser = (user) => {
  if (user) {
    localStorage.setItem('user', JSON.stringify(user));
  } else {
    localStorage.removeItem('user');
  }
};

export const removeAuth = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

export async function request(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401) {
      removeAuth();
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    const message = data?.message || data?.error || `Request failed with status ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

// Authentication Endpoints
export const authApi = {
  login: (credentials) => request('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials)
  }),
  register: (userData) => request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(userData)
  }),
  logout: () => request('/auth/logout', {
    method: 'POST'
  }),
  getCurrentUser: () => request('/auth/me')
};

// Borrower Lending & Repayment Endpoints
export const borrowerApi = {
  getDashboard: () => request('/borrower/dashboard'),
  submitKyc: (kycData) => request('/borrower/kyc/submit', {
    method: 'POST',
    body: JSON.stringify(kycData)
  }),
  requestLoan: (loanData) => request('/borrower/loans/request', {
    method: 'POST',
    body: JSON.stringify(loanData)
  }),
  getMyLoans: () => request('/borrower/loans'),
  payInstallment: (paymentData) => request('/borrower/loans/pay', {
    method: 'POST',
    body: JSON.stringify(paymentData)
  }),
  getPaymentHistory: () => request('/borrower/loans/payments')
};

// Underwriter Endpoints
export const underwriterApi = {
  getQueue: () => request('/underwriter/queue'),
  reviewApplicant: (userId, reviewData) => request(`/underwriter/review/${userId}`, {
    method: 'POST',
    body: JSON.stringify(reviewData)
  })
};

// Admin Endpoints
export const adminApi = {
  getMetrics: () => request('/admin/metrics'),
  getUsers: () => request('/admin/users'),
  updateUserRoles: (userId, roles) => request(`/admin/users/${userId}/roles`, {
    method: 'PUT',
    body: JSON.stringify({ roles })
  }),
  toggleUserStatus: (userId) => request(`/admin/users/${userId}/toggle-status`, {
    method: 'PATCH'
  }),
  getParameters: () => request('/admin/parameters'),
  updateParameter: (key, paramValue) => request(`/admin/parameters/${encodeURIComponent(key)}`, {
    method: 'PUT',
    body: JSON.stringify({ paramValue })
  }),
  getAuditLogs: () => request('/admin/audit-logs')
};
