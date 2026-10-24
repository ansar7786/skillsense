import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import LoginPage from './pages/LoginPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import StudentHomePage from './pages/StudentHomePage';
import TakeSurveyPage from './pages/TakeSurveyPage';
import StudentResultPage from './pages/StudentResultPage';
import OfficialsHomePage from './pages/OfficialsHomePage';
import SurveyBuilderPage from './pages/SurveyBuilderPage';
import SurveyAnalyticsDashboardPage from './pages/SurveyAnalyticsDashboardPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminTermsPage from './pages/AdminTermsPage';
import ForbiddenPage from './pages/ForbiddenPage';
import NotFoundPage from './pages/NotFoundPage';

function RootRedirect() {
  const { isAuthenticated, user, loading, mustChangePassword } = useAuth();

  if (loading) {
    return (
      <div className="loading-container" role="status">
        <div className="spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (mustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  if (user?.role === 'STUDENT') {
    return <Navigate to="/student" replace />;
  }

  return <Navigate to="/officials" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="/404" element={<NotFoundPage />} />

        {/* Change Password (accessible to any authenticated role) */}
        <Route
          path="/change-password"
          element={
            <ProtectedRoute>
              <ChangePasswordPage />
            </ProtectedRoute>
          }
        />

        {/* Student Routes (STUDENT role only) */}
        <Route
          path="/student"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/surveys/:id"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <TakeSurveyPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/surveys/:id/result"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentResultPage />
            </ProtectedRoute>
          }
        />

        {/* Officials Routes (FACULTY, HOD, PLACEMENT_OFFICER, ADMIN) */}
        <Route
          path="/officials"
          element={
            <ProtectedRoute allowedRoles={['FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN']}>
              <OfficialsHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officials/surveys/new"
          element={
            <ProtectedRoute allowedRoles={['FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN']}>
              <SurveyBuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officials/surveys/:id/edit"
          element={
            <ProtectedRoute allowedRoles={['FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN']}>
              <SurveyBuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officials/surveys/:id/analytics"
          element={
            <ProtectedRoute allowedRoles={['FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN']}>
              <SurveyAnalyticsDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officials/surveys/:id"
          element={
            <ProtectedRoute allowedRoles={['FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN']}>
              <SurveyAnalyticsDashboardPage />
            </ProtectedRoute>
          }
        />

        {/* Admin Routes (ADMIN role only) */}
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/terms"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminTermsPage />
            </ProtectedRoute>
          }
        />

        {/* Root Redirect */}
        <Route path="/" element={<RootRedirect />} />

        {/* Catch-all 404 */}
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </AuthProvider>
  );
}
