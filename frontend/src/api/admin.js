import client from './client';

/**
 * Fetch paginated users list with optional search and filters
 */
export async function fetchUsers(params = {}) {
  const response = await client.get('/users', { params });
  return response.data;
}

/**
 * Create a new user manually
 */
export async function createUser(data) {
  const response = await client.post('/users', data);
  return response.data;
}

/**
 * Update an existing user
 */
export async function updateUser(id, data) {
  const response = await client.put(`/users/${id}`, data);
  return response.data;
}

/**
 * Reset user password (generates temporary password)
 */
export async function resetPassword(id) {
  const response = await client.post(`/users/${id}/reset-password`);
  return response.data;
}

/**
 * Upload CSV file for batch user import
 */
export async function importUsersCsv(file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post('/users/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
}

/**
 * Fetch all academic terms
 */
export async function fetchAcademicTerms() {
  const response = await client.get('/terms');
  return response.data;
}

/**
 * Create a new academic term
 */
export async function createAcademicTerm(data) {
  const response = await client.post('/terms', data);
  return response.data;
}

/**
 * Set an academic term as current
 */
export async function setCurrentAcademicTerm(id) {
  const response = await client.put(`/terms/${id}/current`);
  return response.data;
}
