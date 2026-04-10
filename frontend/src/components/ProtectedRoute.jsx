import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';

const ProtectedRoute = ({ children }) => {
  const { isLoggedIn, _hasHydrated } = useAuthStore();
  const location = useLocation();

  if (!_hasHydrated) {
    return <LoadingSpinner/>;
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
};

export default ProtectedRoute;