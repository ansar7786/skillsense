import client from './client';

/**
 * Fetch list of surveys visible to the current official
 */
export async function fetchSurveys(params = {}) {
  const response = await client.get('/surveys', { params });
  return response.data;
}

/**
 * Fetch a single survey by ID
 */
export async function fetchSurvey(id) {
  const response = await client.get(`/surveys/${id}`);
  return response.data;
}

/**
 * Create a new survey draft
 */
export async function createSurvey(data) {
  const response = await client.post('/surveys', data);
  return response.data;
}

/**
 * Update an existing survey draft
 */
export async function updateSurvey(id, data) {
  const response = await client.put(`/surveys/${id}`, data);
  return response.data;
}

/**
 * Delete a draft survey
 */
export async function deleteSurvey(id) {
  const response = await client.delete(`/surveys/${id}`);
  return response.data;
}

/**
 * Activate a survey
 */
export async function activateSurvey(id) {
  const response = await client.post(`/surveys/${id}/activate`);
  return response.data;
}

/**
 * Close an active survey
 */
export async function closeSurvey(id) {
  const response = await client.post(`/surveys/${id}/close`);
  return response.data;
}

/**
 * Create a new version of a closed survey
 */
export async function createNewVersion(id, data = {}) {
  const response = await client.post(`/surveys/${id}/new-version`, data);
  return response.data;
}

/**
 * Query eligible student count for a given target filter
 */
export async function fetchEligibleCount(filter) {
  const response = await client.post('/surveys/eligible-count', filter || {});
  return response.data;
}

/**
 * Fetch starter question preset for a category
 */
export async function fetchPreset(category) {
  const response = await client.get(`/survey-presets/${category}`);
  return response.data;
}

/**
 * Fetch dashboard overview summary
 */
export async function fetchDashboardSummary() {
  const response = await client.get('/dashboard/summary');
  return response.data;
}

/**
 * Fetch all academic terms
 */
export async function fetchTerms() {
  const response = await client.get('/terms');
  return response.data;
}

/**
 * Fetch aggregate analytics for a survey
 */
export async function fetchSurveyAnalytics(id) {
  const response = await client.get(`/surveys/${id}/analytics`);
  return response.data;
}

/**
 * Fetch historical version trend for a survey
 */
export async function fetchSurveyTrend(id) {
  const response = await client.get(`/surveys/${id}/trend`);
  return response.data;
}

/**
 * Fetch AI / Rules-based insights for a survey
 */
export async function fetchSurveyInsights(id, refresh = false) {
  const response = await client.get(`/surveys/${id}/insights`, {
    params: { refresh: Boolean(refresh) }
  });
  return response.data;
}

/**
 * Fetch free-text responses for a specific question key
 */
export async function fetchSurveyTexts(id, key) {
  const response = await client.get(`/surveys/${id}/texts`, {
    params: { key }
  });
  return response.data;
}

/**
 * Download survey report export (PDF or XLSX)
 */
export async function downloadSurveyExport(id, format) {
  const response = await client.get(`/surveys/${id}/export`, {
    params: { format },
    responseType: 'blob'
  });
  return response.data;
}

/**
 * Trigger download of blob as file in browser
 */
export function triggerFileDownload(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
