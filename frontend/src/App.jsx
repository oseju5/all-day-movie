import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainPage from './components/MainPage';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import FindAccountPage from './pages/FindAccountPage';
import MyPage from './pages/MyPage';
import BookingPage from './pages/BookingPage';
import SeatSelectionPage from './pages/SeatSelectionPage';
import SearchResults from './components/SearchResults';

function App() {
  return (
    <Router>
      <Routes>
        {/* 메인 페이지 */}
        <Route path="/" element={<MainPage />} />
        
        {/* 로그인 페이지 */}
        <Route path="/login" element={<LoginPage />} />

        {/* 회원가입 페이지 */}
        <Route path="/signup" element={<SignupPage />} />

        {/* 계정 찾기 페이지 */}
        <Route path="/find-account" element={<FindAccountPage />} />

        {/* 마이 페이지 */}
        <Route 
          path="/mypage" 
          element={
            <ProtectedRoute>
              <MyPage />
            </ProtectedRoute>
          } 
        />
        
        {/* 예매 페이지 (비로그인 허용) */}
        <Route path="/booking" element={<BookingPage />} />

        {/* 좌석 선택 페이지 (로그인 필수) */}
        <Route 
          path="/seat-selection" 
          element={
            <ProtectedRoute>
              <SeatSelectionPage />
            </ProtectedRoute>
          } 
        />

        {/* 없는 경로는 메인으로 리다이렉트 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
