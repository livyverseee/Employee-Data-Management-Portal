import axios from 'axios';

// Base API URL pointing to the Spring Boot backend
const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Request interceptor: automatically attaches JWT/token header to all outgoing requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handles 401 Unauthorized by clearing session and redirecting
// Only redirects for non-auth requests so login/register error messages are not interrupted
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const url = error.config?.url || '';
      if (!url.includes('/auth/')) {
        const role = localStorage.getItem('role');
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('fullName');
        localStorage.removeItem('role');

        const redirectUrl = role === 'DEAN' ? '/dean/login' : (role === 'EMPLOYEE' ? '/employee/login' : '/');
        if (!window.location.pathname.endsWith('/login')) {
          window.location.href = redirectUrl;
        }
      }
    }
    return Promise.reject(error);
  }
);

// ====================================================
// AUTH ENDPOINTS (Portal-Specific)
// ====================================================

export const loginDeanApi = (identifier, password) => {
  return api.post('/auth/dean/login', { identifier, password });
};

export const loginEmployeeApi = (identifier, password) => {
  return api.post('/auth/employee/login', { identifier, password });
};

export const registerDeanApi = (data) => {
  return api.post('/auth/dean/register', data);
};

export const registerEmployeeApi = (data) => {
  return api.post('/auth/employee/register', data);
};

// ====================================================
// DATASET ENDPOINTS
// ====================================================

export const getActiveDatasetApi = () => {
  return api.get('/dataset/active');
};

export const uploadDatasetApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/dataset/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const replaceDatasetApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/dataset/replace', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

// ====================================================
// EMPLOYEE ENDPOINTS
// ====================================================

export const getEmployeesApi = (params) => {
  return api.get('/employees', { params });
};

export const getFilterOptionsApi = () => {
  return api.get('/employees/filter-options');
};

export const getEmployeeByIdApi = (id) => {
  return api.get(`/employees/${encodeURIComponent(id)}`);
};

export const updateEmployeeApi = (id, data) => {
  return api.put(`/employees/${encodeURIComponent(id)}`, data);
};

export const deleteEmployeeApi = (id) => {
  return api.delete(`/employees/${encodeURIComponent(id)}`);
};

export const exportExcelApi = (params) => {
  return api.get('/employees/export/excel', {
    params,
    responseType: 'blob', // Required for binary file download
  });
};

export const exportCsvApi = (params) => {
  return api.get('/employees/export', {
    params,
    responseType: 'blob', // Required for binary file download
  });
};

export default api;
