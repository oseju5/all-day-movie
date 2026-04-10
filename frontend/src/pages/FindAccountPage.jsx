import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';


const FindAccountPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('id'); // 'id' or 'pw'
  const [isLoading, setIsLoading] = useState(false);

  // 아이디 찾기 상태
  const [findIdEmail, setFindIdEmail] = useState('');
  const [foundId, setFoundId] = useState('');
  const [findIdError, setFindIdError] = useState('');

  // 비밀번호 찾기 상태
  const [pwFormData, setPwFormData] = useState({ username: '', email: '' });
  const [isCodeSent, setIsCodeSent] = useState(false); // 이메일 난수 발송 여부
  const [verificationCode, setVerificationCode] = useState('');
  const [isCodeVerified, setIsCodeVerified] = useState(false); // 인증번호 일치 여부
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [pwError, setPwError] = useState('');

  // 타이머 관련 상태 (180초 = 3분)
  const [timer, setTimer] = useState(180);
  const [isTimerActive, setIsTimerActive] = useState(false);

  // 타이머 로직
  useEffect(() => {
    let interval = null;
    if (isTimerActive && timer > 0) {
      interval = setInterval(() => {
        setTimer((prevTimer) => prevTimer - 1);
      }, 1000);
    } else if (timer === 0) {
      clearInterval(interval);
      setIsTimerActive(false);
      setIsCodeSent(false); // 3분 뒤 다시 발송 가능하도록 설정
    }
    return () => clearInterval(interval);
  }, [isTimerActive, timer]);

  // 타이머 표시 포맷 (MM:SS)
  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs < 10 ? '0' : ''}${secs}`;
  };

  // ---------- 아이디 찾기 로직 ----------
  const handleFindId = async (e) => {
    e.preventDefault();
    if (!findIdEmail) {
      setFindIdError('이메일을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setFindIdError('');
    setFoundId('');

    try {
      const response = await fetch('/api/auth/find-id', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: findIdEmail }),
      });

      if (response.ok) {
        const username = await response.text();
        setFoundId(username);
      } else {
        const errorText = await response.text();
        setFindIdError(errorText || '일치하는 정보가 없습니다.');
      }
    } catch (err) {
      console.error(err);
      setFindIdError('서버 통신 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  // ---------- 비밀번호 찾기 로직 ----------
  const handleVerifyUser = async () => {
    if (!pwFormData.username || !pwFormData.email) {
      setPwError('아이디와 이메일을 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setPwError('');

    try {
      const response = await fetch(`/api/auth/verify-user-for-reset?username=${pwFormData.username}&email=${pwFormData.email}`, {
        method: 'POST',
      });

      if (response.ok) {
        const data = await response.json();
        if (data.valid) {
          // 바로 인증번호 발송 호출 (await를 추가하여 발송 완료까지 대기)
          await handleSendCode();
        } else {
          setPwError('입력하신 아이디와 이메일이 일치하지 않습니다.');
          setIsLoading(false); // 실패 시 로딩 종료
        }
      } else {
        setIsLoading(false); // 응답 실패 시 로딩 종료
      }
    } catch (err) {
      console.error(err);
      setPwError('서버 통신 오류가 발생했습니다.');
      setIsLoading(false); // 에러 시 로딩 종료
    }
  };

  const handleSendCode = async () => {
    setPwError('');
    setIsLoading(true); // 시각적인 로딩 효과 시작
    try {
      const response = await fetch('/api/auth/email/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: pwFormData.email }),
      });
      if (response.ok) {
        setIsCodeSent(true);
        setTimer(180); // 3분 타이머 초기화
        setIsTimerActive(true); // 타이머 시작
        alert('이메일로 인증번호가 전송되었습니다.');
      } else {
        setPwError('인증번호 전송에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      setPwError('서버 통신 오류가 발생했습니다.');
    } finally {
      setIsLoading(false); // 로딩 종료
    }
  };

  const handleVerifyCode = async () => {
    if (!verificationCode) {
      setPwError('인증번호를 입력해주세요.');
      return;
    }

    setPwError('');
    try {
      const response = await fetch('/api/auth/email/verify-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: pwFormData.email, code: verificationCode }),
      });

      if (response.ok) {
        const data = await response.json();
        if (data.verified) {
          setIsCodeVerified(true);
          alert('인증이 완료되었습니다. 새 비밀번호를 설정해주세요.');
        } else {
          setPwError('인증번호가 일치하지 않습니다.');
        }
      }
    } catch (err) {
      console.error(err);
      setPwError('서버 통신 오류가 발생했습니다.');
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!newPassword || newPassword !== newPasswordConfirm) {
      setPwError('비밀번호가 일치하지 않습니다.');
      return;
    }

    const pwRegex = /^(?:(?=.*[a-zA-Z])(?=.*\d)|(?=.*[a-zA-Z])(?=.*[@$!%*?&])|(?=.*\d)(?=.*[@$!%*?&]))[A-Za-z\d@$!%*?&]{8,16}$/;
    if (!pwRegex.test(newPassword)) {
      setPwError('비밀번호는 8~16자의 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다.');
      return;
    }

    setIsLoading(true);
    setPwError('');

    try {
      const response = await fetch(`/api/auth/reset-password?username=${pwFormData.username}&newPassword=${encodeURIComponent(newPassword)}`, {
        method: 'POST',
      });

      if (response.ok) {
        alert('비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.');
        navigate('/login');
      } else {
        const errorText = await response.text();
        setPwError(errorText || '비밀번호 변경에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      setPwError('서버 통신 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center font-sans">
      <div className="w-full max-w-[600px] flex-1 bg-white shadow-2xl border-x border-gray-100 flex flex-col px-6 sm:px-12 pb-10">
        <div className="pt-16 pb-8 text-center relative">
          <Link to="/"
            className="absolute left-0 top-16 text-gray-500 hover:text-gray-900 transition-colors cursor-pointer"
          >
            <ChevronLeft className="w-6 h-6 text-gray-800" />
          </Link>
          <h1 className="text-2xl font-bold text-gray-900 tracking-wider">
            계정 찾기
          </h1>
        </div>

        {/* 탭 전환 */}
        <div className="flex border-b border-gray-200 mb-8">
          <button
            onClick={() => { setActiveTab('id'); setFoundId(''); setFindIdError(''); }}
            className={`flex-1 py-3 text-center font-semibold transition-colors cursor-pointer ${
              activeTab === 'id' ? 'text-[#e50914] border-b-2 border-[#e50914]' : 'text-gray-400 hover:text-gray-700'
            }`}
          >
            아이디 찾기
          </button>
          <button
            onClick={() => { setActiveTab('pw'); setPwError(''); }}
            className={`flex-1 py-3 text-center font-semibold transition-colors cursor-pointer ${
              activeTab === 'pw' ? 'text-[#e50914] border-b-2 border-[#e50914]' : 'text-gray-400 hover:text-gray-700'
            }`}
          >
            비밀번호 재설정
          </button>
        </div>

        {/* 아이디 찾기 탭 */}
        {activeTab === 'id' && (
          <form onSubmit={handleFindId} className="flex flex-col gap-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1">가입한 이메일</label>
              <input
                type="email"
                value={findIdEmail}
                onChange={(e) => setFindIdEmail(e.target.value)}
                placeholder="example@email.com"
                className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition-all text-gray-900"
              />
            </div>
            
            {findIdError && <p className="text-sm text-[#e50914] font-medium">{findIdError}</p>}
            
            {foundId && (
              <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg mt-2 text-center">
                <p className="text-sm text-gray-600 mb-1">회원님의 아이디는 아래와 같습니다.</p>
                <p className="text-lg font-bold text-[#e50914]">{foundId}</p>
              </div>
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
              {isLoading ? '조회 중...' : '아이디 찾기'}
            </button>
          </form>
        )}

        {/* 비밀번호 재설정 탭 */}
        {activeTab === 'pw' && (
          <div className="flex flex-col gap-4">
            {!isCodeVerified ? (
              <>
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1">아이디</label>
                  <input
                    type="text"
                    value={pwFormData.username}
                    onChange={(e) => setPwFormData({...pwFormData, username: e.target.value})}
                    disabled={isCodeSent}
                    placeholder="아이디 입력"
                    className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900 disabled:text-gray-400"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1">가입한 이메일</label>
                  <div className="flex gap-2">
                    <input
                      type="email"
                      value={pwFormData.email}
                      onChange={(e) => setPwFormData({...pwFormData, email: e.target.value})}
                      disabled={isCodeSent}
                      placeholder="example@email.com"
                      className="flex-1 h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900 disabled:text-gray-400"
                    />
                    <div className="flex items-center gap-2">
                      {isTimerActive && (
                        <span className="text-[#e50914] font-bold text-sm min-w-[40px]">
                          {formatTime(timer)}
                        </span>
                      )}
                      <button
                        type="button"
                        onClick={handleVerifyUser}
                        disabled={isCodeSent || isLoading || isTimerActive}
                        className={`px-4 h-12 font-semibold rounded-lg transition-colors whitespace-nowrap cursor-pointer ${
                          (isCodeSent || isLoading || isTimerActive)
                            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                            : 'bg-[#e50914] text-white hover:bg-red-700'
                        }`}
                      >
                        {isLoading ? '발송 중...' : '인증번호 발송'}
                      </button>
                    </div>
                  </div>
                </div>

                {isCodeSent && (
                  <div className="mt-2">
                    <label className="block text-sm font-semibold text-gray-700 mb-1">인증번호</label>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={verificationCode}
                        onChange={(e) => setVerificationCode(e.target.value)}
                        placeholder="6자리 숫자 입력"
                        className="flex-1 h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900"
                      />
                      <button
                        type="button"
                        onClick={handleVerifyCode}
                        className="px-4 h-12 bg-[#e50914] text-white font-semibold rounded-lg hover:bg-red-700 transition-colors whitespace-nowrap cursor-pointer"
                      >
                        확인
                      </button>
                    </div>
                  </div>
                )}
                
                {pwError && <p className="text-sm text-[#e50914] font-medium">{pwError}</p>}
              </>
            ) : (
              <form onSubmit={handleResetPassword} className="flex flex-col gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1">새 비밀번호</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="8~16자 영문, 숫자, 특수문자 중 2가지 이상 포함"
                    className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1">새 비밀번호 확인</label>
                  <input
                    type="password"
                    value={newPasswordConfirm}
                    onChange={(e) => setNewPasswordConfirm(e.target.value)}
                    placeholder="비밀번호 다시 입력"
                    className="w-full h-12 px-4 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-[#e50914] transition-all text-gray-900"
                  />
                </div>

                {pwError && <p className="text-sm text-[#e50914] font-medium">{pwError}</p>}

                <button
                  type="submit"
                  disabled={isLoading}
                  className={`w-full h-12 mt-4 text-white font-bold rounded-lg shadow-lg transition-all cursor-pointer ${
                    isLoading 
                      ? 'bg-gray-400 cursor-not-allowed' 
                      : 'bg-[#e50914] hover:bg-red-700 shadow-red-500/30 hover:shadow-red-500/50'
                  }`}
                >
                  {isLoading ? '변경 중...' : '비밀번호 변경'}
                </button>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default FindAccountPage;
