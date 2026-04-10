import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';
import { ChevronLeft } from 'lucide-react';

const LoginPage = () => {
  const [formData, setFormData] = useState({ username: '', password: '' });
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);

  // ProtectedRoute 또는 BookingPage에서 넘어온 상태 정보
  const from = location.state?.from || '/';
  const movieId = location.state?.movieId;
  const date = location.state?.date;

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!formData.username || !formData.password) {
      setErrorMsg('아이디와 비밀번호를 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setErrorMsg('');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        const data = await response.json();
        // Zustand store에 토큰, 닉네임, 아이디 저장
        login(data.token, data.nickname, data.username);
        
        // 원래 가려던 페이지로 이동하되, 전달받은 추가 상태(movieId, date 등)도 그대로 넘김
        navigate(from, { 
          replace: true,
          state: { movieId, date }
        });
      } else {
        // 401 Unauthorized 등 로그인 실패
        setErrorMsg('아이디 혹은 비밀번호가 일치하지 않습니다. 다시 입력해주세요.');
      }
    } catch (err) {
      console.error(err);
      setErrorMsg('서버와 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center font-sans">
      <div className="w-full max-w-[600px] flex-1 bg-white shadow-2xl border-x border-gray-100 flex flex-col px-6 sm:px-12">
        {/* 헤더 간소화 로고 */}
        <div className="pt-16 pb-8 text-center relative">
          <Link to="/"
            className="absolute left-0 top-16 text-gray-500 hover:text-gray-900 transition-colors cursor-pointer"
          >
            <ChevronLeft className="w-6 h-6 text-gray-800" />
          </Link>
          <Link to="/" className="text-3xl font-bold text-[#e50914] tracking-wider block">
            AllDayMovie
          </Link>
          <p className="mt-4 text-gray-500 font-medium">서비스 이용을 위해 로그인해주세요.</p>
        </div>

        {/* 로그인 폼 */}
        <form onSubmit={handleLogin} className="flex flex-col gap-4 mt-8 flex-1">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">아이디</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="아이디를 입력하세요"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">비밀번호</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="비밀번호를 입력하세요"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
          </div>

          {/* 에러 메시지 영역 */}
          {errorMsg && (
            <p className="text-sm text-[#e50914] font-medium mt-1">
              {errorMsg}
            </p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className={`w-full h-12 mt-4 text-white font-bold rounded-lg shadow-lg transition-all cursor-pointer ${
              isLoading 
                ? 'bg-gray-400 cursor-not-allowed' 
                : 'bg-[#e50914] hover:bg-red-700 shadow-red-500/30 hover:shadow-red-500/50'
            }`}
          >
            {isLoading ? '로그인 중...' : '로그인'}
          </button>

          <div className="flex items-center justify-between mt-6 text-sm text-gray-500 font-medium">
            <Link to="/signup" className="hover:text-[#e50914] transition-colors">
              회원가입
            </Link>
            <div className="flex gap-4">
              <Link to="/find-account" className="hover:text-[#e50914] transition-colors">
                아이디/비밀번호 찾기
              </Link>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
