import axios from 'axios';

// Always use relative /api so both localhost and public URLs route through the same origin proxy without mixed content or CORS blocks
const API_BASE_URL = '/api';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor: attach JWT token if present
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('skillsense_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 Unauthorized centrally
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('skillsense_token');
      localStorage.removeItem('skillsense_user');
      const pathname = typeof window !== 'undefined' ? window.location.pathname : '';
      const isPublic = pathname === '/' || pathname.startsWith('/login') || pathname === '/403' || pathname === '/404';
      const isAuthCheck = error.config?.url?.includes('/auth/me');
      if (typeof window !== 'undefined' && !isPublic && !isAuthCheck) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default client;
