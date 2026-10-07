import axios from 'axios';

// Base API URL pointing to the Spring Boot backend
const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Request interceptor: attaches Bearer token header to outgoing requests
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
// DATASET LIFECYCLE ENDPOINTS
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

export const downloadDatasetXmlApi = () => {
  return api.get('/dataset/xml', {
    responseType: 'blob',
  });
};

// ====================================================
// RECORD QUERY & CRUD ENDPOINTS
// ====================================================

export const getRecordsApi = (params = {}) => {
  return api.get('/records', { params });
};

export const getEmployeesApi = getRecordsApi;

export const getFilterOptionsApi = () => {
  return api.get('/records/filter-options');
};

export const getRecordByIdApi = (id) => {
  return api.get(`/records/${encodeURIComponent(id)}`);
};

export const getEmployeeByIdApi = getRecordByIdApi;

export const addRecordApi = (data) => {
  return api.post('/records', { data });
};

export const updateRecordApi = (id, data) => {
  return api.put(`/records/${encodeURIComponent(id)}`, { data });
};

export const updateEmployeeApi = updateRecordApi;

export const deleteRecordApi = (id) => {
  return api.delete(`/records/${encodeURIComponent(id)}`);
};

export const deleteEmployeeApi = deleteRecordApi;

// ====================================================
// EXPORT ENDPOINT (Excel .xlsx only)
// ====================================================

export const exportExcelApi = (params = {}) => {
  return api.get('/records/export/excel', {
    params,
    responseType: 'blob', // Binary blob for .xlsx download
  });
};

export default api;
