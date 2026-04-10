import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';

/**
 * 회원가입 페이지 컴포넌트.
 * 실시간 아이디 중복 검사 및 입력 제한 기능을 포함합니다.
 * @author ohseju
 */
const SignupPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    passwordConfirm: '',
    nickname: '',
    phone: '',
    email: '',
    birthDate: ''
  });

  // 생년월일 개별 상태 관리
  const [birthYear, setBirthYear] = useState('');
  const [birthMonth, setBirthMonth] = useState('');
  const [birthDay, setBirthDay] = useState('');

  // 개별 상태가 변경될 때마다 formData.birthDate 업데이트
  useEffect(() => {
    if (birthYear && birthMonth && birthDay) {
      const formattedMonth = birthMonth.padStart(2, '0');
      const formattedDay = birthDay.padStart(2, '0');
      setFormData(prev => ({
        ...prev,
        birthDate: `${birthYear}-${formattedMonth}-${formattedDay}`
      }));
    } else {
      setFormData(prev => ({ ...prev, birthDate: '' }));
    }
  }, [birthYear, birthMonth, birthDay]);

  const [errors, setErrors] = useState({});
  const [isUsernameChecked, setIsUsernameChecked] = useState(false);
  const [isCheckingUsername, setIsCheckingUsername] = useState(false); 
  const [isLoading, setIsLoading] = useState(false);

  // 아이디 실시간 중복 검사 (Debounce)
  useEffect(() => {
    const timer = setTimeout(() => {
      if (formData.username.length >= 4) {
        checkUsername(formData.username);
      } else if (formData.username.length > 0) {
        setErrors(prev => ({ ...prev, username: '아이디는 4~20자리 영문자와 숫자만 사용 가능합니다.' }));
        setIsUsernameChecked(false);
      } else {
        setErrors(prev => ({ ...prev, username: '' }));
        setIsUsernameChecked(false);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [formData.username]);

  const checkUsername = async (username) => {
    setIsCheckingUsername(true);
    try {
      const response = await fetch(`/api/auth/check-username?username=${username}`);
      if (response.ok) {
        const data = await response.json();
        if (data.exists) {
          setErrors(prev => ({ ...prev, username: '이미 사용중인 아이디입니다.' }));
          setIsUsernameChecked(false);
        } else {
          setErrors(prev => ({ ...prev, username: '사용 가능한 아이디입니다.' }));
          setIsUsernameChecked(true);
        }
      }
    } catch (err) {
      console.error('중복 검사 실패:', err);
    } finally {
      setIsCheckingUsername(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // 아이디 입력 시 영문/숫자만 허용
    if (name === 'username') {
      const filteredValue = value.replace(/[^A-Za-z0-9]/g, '');
      if (value !== filteredValue) return; 
      
      setFormData({ ...formData, [name]: filteredValue });
      setIsUsernameChecked(false);
    } else {
      setFormData({ ...formData, [name]: value });
    }
    
    if (name !== 'username' && errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
  };

  // 연도 리스트 생성 (현재 연도부터 1900년까지)
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear - 1900 + 1 }, (_, i) => currentYear - i);
  const months = Array.from({ length: 12 }, (_, i) => i + 1);
  
  const getDaysInMonth = (year, month) => {
    if (!year || !month) return 31;
    return new Date(year, month, 0).getDate();
  };
  const days = Array.from({ length: getDaysInMonth(birthYear, birthMonth) }, (_, i) => i + 1);

  const validateForm = () => {
    const newErrors = {};
    if (!isUsernameChecked) {
      newErrors.username = '사용 가능한 아이디를 입력해주세요.';
    }
    
    const pwRegex = /^(?:(?=.*[a-zA-Z])(?=.*\d)|(?=.*[a-zA-Z])(?=.*[@$!%*?&])|(?=.*\d)(?=.*[@$!%*?&]))[A-Za-z\d@$!%*?&]{8,16}$/;
    if (!pwRegex.test(formData.password)) {
      newErrors.password = '비밀번호는 8~16자의 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다.';
    }

    if (formData.password !== formData.passwordConfirm) {
      newErrors.passwordConfirm = '비밀번호가 일치하지 않습니다.';
    }

    if (!formData.nickname) {
      newErrors.nickname = '닉네임을 입력해주세요.';
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      newErrors.email = '올바른 이메일 형식이 아닙니다.';
    }

    if (!formData.birthDate) {
      newErrors.birthDate = '생년월일을 입력해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsLoading(true);
    try {
      const response = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        alert('회원가입이 완료되었습니다. 로그인해주세요.');
        navigate('/login');
      } else {
        const errorText = await response.text();
        alert(`회원가입 실패: ${errorText}`);
      }
    } catch (err) {
      console.error(err);
      alert('서버와 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center font-sans">
      <div className="w-full max-w-[600px] flex-1 bg-white shadow-2xl border-x border-gray-100 flex flex-col px-6 sm:px-12 pb-10">
        <div className="pt-16 pb-8 text-center relative">
          <Link to="/" className="absolute left-0 top-16 text-gray-500 hover:text-gray-900 transition-colors">
            <ChevronLeft className="w-6 h-6 text-gray-800" />
          </Link>
          <h1 className="text-2xl font-bold text-gray-900 tracking-wider">회원가입</h1>
        </div>

        <div className="text-right text-xs font-semibold text-[#e50914] mb-2">* 필수 항목</div>

        <form onSubmit={handleSignup} className="flex flex-col gap-4">
          {/* 아이디 & 실시간 중복확인 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              아이디 <span className="text-[#e50914]">*</span>
            </label>
            <div className="relative">
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                maxLength={20}
                placeholder="4~20자리 영문, 숫자"
                className={`w-full h-12 px-4 bg-gray-50 border rounded-lg outline-none transition-all text-gray-900 ${
                  isUsernameChecked 
                    ? 'border-green-500 focus:border-green-500 focus:ring-1 focus:ring-green-500' 
                    : 'border-gray-200 focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914]'
                }`}
              />
              {isCheckingUsername && (
                <div className="absolute right-4 top-1/2 -translate-y-1/2">
                  <div className="w-4 h-4 border-2 border-[#e50914] border-t-transparent rounded-full animate-spin"></div>
                </div>
              )}
            </div>
            {errors.username && (
              <p className={`text-xs mt-1.5 font-medium ml-1 ${isUsernameChecked ? 'text-green-600' : 'text-[#e50914]'}`}>
                {isUsernameChecked && '✓ '}{errors.username}
              </p>
            )}
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              비밀번호 <span className="text-[#e50914]">*</span>
            </label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="8~16자 영문, 숫자, 특수문자 중 2가지 이상 포함"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
            {errors.password && <p className="text-sm text-[#e50914] font-medium mt-1">{errors.password}</p>}
          </div>

          {/* 비밀번호 확인 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              비밀번호 확인 <span className="text-[#e50914]">*</span>
            </label>
            <input
              type="password"
              name="passwordConfirm"
              value={formData.passwordConfirm}
              onChange={handleChange}
              placeholder="비밀번호 다시 입력"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
            {errors.passwordConfirm && <p className="text-sm text-[#e50914] font-medium mt-1">{errors.passwordConfirm}</p>}
          </div>

          {/* 닉네임 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              닉네임 <span className="text-[#e50914]">*</span>
            </label>
            <input
              type="text"
              name="nickname"
              value={formData.nickname}
              onChange={handleChange}
              maxLength={20}
              placeholder="최대 20자 입력 가능"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
            {errors.nickname && <p className="text-sm text-[#e50914] font-medium mt-1">{errors.nickname}</p>}
          </div>

          {/* 전화번호 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">전화번호</label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              placeholder="010-0000-0000"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
          </div>

          {/* 생년월일 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              생년월일 <span className="text-[#e50914]">*</span>
            </label>
            <div className="flex gap-2">
              <select value={birthYear} onChange={(e) => setBirthYear(e.target.value)} className="flex-1 h-12 px-2 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900">
                <option value="">년</option>
                {years.map(y => <option key={y} value={y}>{y}</option>)}
              </select>
              <select value={birthMonth} onChange={(e) => setBirthMonth(e.target.value)} className="flex-1 h-12 px-2 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900">
                <option value="">월</option>
                {months.map(m => <option key={m} value={m}>{m}</option>)}
              </select>
              <select value={birthDay} onChange={(e) => setBirthDay(e.target.value)} className="flex-1 h-12 px-2 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900">
                <option value="">일</option>
                {days.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
            {errors.birthDate && <p className="text-sm text-[#e50914] font-medium mt-1">{errors.birthDate}</p>}
          </div>

          {/* 이메일 */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              이메일 <span className="text-[#e50914]">*</span>
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="example@email.com"
              className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
            />
            {errors.email && <p className="text-sm text-[#e50914] font-medium mt-1">{errors.email}</p>}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className={`w-full h-12 mt-6 text-white font-bold rounded-lg shadow-lg transition-all cursor-pointer ${
              isLoading 
                ? 'bg-gray-400 cursor-not-allowed' 
                : 'bg-[#e50914] hover:bg-red-700 shadow-red-500/30 hover:shadow-red-500/50'
            }`}
          >
            {isLoading ? '가입 진행 중...' : '회원가입'}
          </button>

          <p className="text-center mt-4 text-sm text-gray-500 font-medium">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-[#e50914] hover:underline">로그인하기</Link>
          </p>
        </form>
      </div>
    </div>
  );
};

export default SignupPage;
