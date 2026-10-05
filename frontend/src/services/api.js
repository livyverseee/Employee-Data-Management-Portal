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

// Response interceptor: handles 401 Unauthorized by clearing session and redirecting to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth endpoints
export const loginApi = (username, password) => {
  return api.post('/auth/login', { username, password });
};

// Employee endpoints
export const getEmployeesApi = (params) => {
  return api.get('/employees', { params });
};

export const getEmployeeByIdApi = (id) => {
  return api.get(`/employees/${encodeURIComponent(id)}`);
};

export const uploadXmlApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/employees/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const deleteEmployeeApi = (id) => {
  return api.delete(`/employees/${encodeURIComponent(id)}`);
};

export const exportCsvApi = (params) => {
  return api.get('/employees/export', {
    params,
    responseType: 'blob', // Required for binary file download
  });
};

export default api;
