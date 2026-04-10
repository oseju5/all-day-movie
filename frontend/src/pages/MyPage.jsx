import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ChevronLeft, Trash2, CheckCircle2, AlertCircle, Users, CreditCard, ChevronUp, ChevronDown, Info } from 'lucide-react';
import useAuthStore from '../store/useAuthStore';

/**
 * 영화 등급에 따른 이미지 경로를 반환합니다.
 * @param {string} ratingStr 영화 등급 문자열
 * @returns {string} 이미지 경로
 */
const getRatingImage = (ratingStr) => {
  if (!ratingStr) return '/images/rating_all.png';
  if (ratingStr.includes('19') || ratingStr.includes('18') || ratingStr.includes('청소년')) return '/images/rating_19.png';
  if (ratingStr.includes('15')) return '/images/rating_15.png';
  if (ratingStr.includes('12')) return '/images/rating_12.png';
  return '/images/rating_all.png';
};

const MyPage = () => {
  const navigate = useNavigate();
  const { username, isLoggedIn, logout, token } = useAuthStore();
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);

  // 부분 취소 모드 관련 상태
  const [cancelModeId, setCancelModeId] = useState(null); // 취소 모드가 활성화된 Reservation ID
  const [selectedTicketIds, setSelectedTicketIds] = useState([]);
  const [adultCancelCount, setAdultCancelCount] = useState(0);
  const [youthCancelCount, setYouthCancelCount] = useState(0);

  // 탭 및 페이지네이션 상태
  const [activeTab, setActiveTab] = useState('COMPLETED'); // 'COMPLETED', 'EXPIRED', 'CANCELED'
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 5;

  // 탭 변경 시 페이지 1로 리셋
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setCurrentPage(1);
    setCancelModeId(null);
  };

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }
    fetchMyReservations();
  }, [isLoggedIn, username, navigate]);

  const fetchMyReservations = async () => {
    try {
const res = await fetch(`/api/reservations/my?username=${username}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`, // Bearer 한 칸 띄우고 토큰
          'Content-Type': 'application/json'
        }
      });

      // 401(토큰 만료 등) 처리
      if (res.status === 401) {
        alert("인증이 만료되었습니다. 다시 로그인해주세요.");
        logout(); // 스토어 로그아웃 처리
        navigate('/login');
        return;
      }

      const data = await res.json();
      setReservations(data);
      } catch (e) {
        console.error("예약 목록 조회 실패:", e);
      } finally {
        setLoading(false);
      }
    };

  // 상영 시작 20분 전인지 체크하는 함수
  const canCancel = (startDay, startTime) => {
    const now = new Date();
    const showtime = new Date(`${startDay}T${startTime}`);
    const diffMs = showtime - now;
    const diffMin = diffMs / (1000 * 60);
    return diffMin >= 20;
  };

  // 취소 모드 진입
  const enterCancelMode = (res) => {
    if (res.tickets.filter(t => t.status === 'ACTIVE').length === 1) {
      // 티켓이 1개면 바로 전체 취소 컨펌
      const ticket = res.tickets.find(t => t.status === 'ACTIVE');
      if (window.confirm(`선택하신 좌석(${ticket.seatNumber}) 예매를 취소하시겠습니까?`)) {
        handleCancelSubmit(res.id, [ticket.id], res.adultCount, res.youthCount);
      }
    } else {
      setCancelModeId(res.id);
      setSelectedTicketIds([]);
      setAdultCancelCount(0);
      setYouthCancelCount(0);
    }
  };

  const toggleTicketSelection = (ticketId) => {
    setSelectedTicketIds(prev => 
      prev.includes(ticketId) ? prev.filter(id => id !== ticketId) : [...prev, ticketId]
    );
  };

  const handleSelectAll = (res) => {
    const activeTicketIds = res.tickets.filter(t => t.status === 'ACTIVE').map(t => t.id);
    if (selectedTicketIds.length === activeTicketIds.length) {
      setSelectedTicketIds([]);
      setAdultCancelCount(0);
      setYouthCancelCount(0);
    } else {
      setSelectedTicketIds(activeTicketIds);
      setAdultCancelCount(res.adultCount);
      setYouthCancelCount(res.youthCount);
    }
  };

  const handleCancelSubmit = async (resId, ticketIds, adultCnt, youthCnt) => {
    if (ticketIds.length === 0) {
      alert("취소할 좌석을 선택해주세요.");
      return;
    }
    if (ticketIds.length !== (adultCnt + youthCnt)) {
      alert(`취소 인원 설정이 올바르지 않습니다. (선택 좌석: ${ticketIds.length}개, 입력 인원: ${adultCnt + youthCnt}명)`);
      return;
    }

    if (!window.confirm(`정말로 선택하신 ${ticketIds.length}개의 좌석을 취소하시겠습니까?`)) return;

    try {
      const response = await fetch('/api/reservations/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json',
                  'Authorization': `Bearer ${token}`
          },
        body: JSON.stringify({
          reservationId: resId,
          ticketIds: ticketIds,
          adultCancelCount: adultCnt,
          youthCancelCount: youthCnt,
          username: username
        })
      });

      if (response.ok) {
        alert("취소가 완료되었습니다.");
        setCancelModeId(null);
        fetchMyReservations();
      } else {
        // 403 Forbidden: 본인 예약이 아닌 경우 등에 대한 대응
        if (response.status === 403) {
          alert("본인의 예약만 취소할 수 있습니다.");
        } else {
          const err = await response.json();
          alert(err.message || "취소 처리 중 오류가 발생했습니다.");
        }
      }
    } catch (e) {
      console.error("취소 통신 오류:", e);
      alert("서버 통신 오류");
    }
  };

  if (loading) return <div className="p-10 text-center">로딩 중...</div>;

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center font-sans text-gray-900">
      <div className="w-full max-w-[600px] flex-1 bg-white shadow-2xl border-x border-gray-100 flex flex-col relative">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="w-full px-4 h-16 flex items-center justify-between">
          <button onClick={() => navigate(-1)} className="p-2 -ml-2"><ChevronLeft className="w-6 h-6 text-gray-800" /></button>
          <h1 className="text-lg font-bold">마이페이지</h1>
          <div className="w-10" /> 
        </div>
      </header>

      <main className="w-full p-4 flex min-h-screen flex-col gap-6 bg-[#F3F4F6]">
        
        {/* 프로필 요약 */}
        <section className="bg-white rounded-2xl p-6 shadow-md flex items-center justify-between gap-4 border border-gray-100">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center text-[#e50914] font-bold text-xl shrink-0">
              {username?.substring(0,1).toUpperCase()}
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">{username}님</h2>
              <p className="text-sm text-gray-500">즐거운 관람 되세요!</p>
            </div>
          </div>
          <button 
            onClick={() => {
              if(window.confirm('로그아웃 하시겠습니까?')) {
                logout();
                navigate('/login');
              }
            }}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-600 rounded-lg text-sm font-medium transition-colors whitespace-nowrap"
          >
            로그아웃
          </button>
        </section>

        <div className="flex items-center justify-between px-1">
          <h3 className="text-lg font-bold text-gray-800">나의 예매 내역</h3>
        </div>

        {/* 필터 탭 */}
        <div className="flex rounded-xl bg-white p-1 shadow-md border border-gray-100">
          <button
            onClick={() => handleTabChange('COMPLETED')}
            className={`flex-1 py-2 text-sm font-bold rounded-lg transition-colors ${
              activeTab === 'COMPLETED' ? 'bg-[#e50914] text-white shadow-sm' : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            예매완료
          </button>
          <button
            onClick={() => handleTabChange('EXPIRED')}
            className={`flex-1 py-2 text-sm font-bold rounded-lg transition-colors ${
              activeTab === 'EXPIRED' ? 'bg-[#e50914] text-white shadow-sm' : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            만료됨
          </button>
          <button
            onClick={() => handleTabChange('CANCELED')}
            className={`flex-1 py-2 text-sm font-bold rounded-lg transition-colors ${
              activeTab === 'CANCELED' ? 'bg-[#e50914] text-white shadow-sm' : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            취소
          </button>
        </div>

        {(() => {
          // 탭에 따른 데이터 필터링 로직
          const filteredReservations = reservations.filter((res) => {
            if (activeTab === 'COMPLETED') {
              return res.status === 'COMPLETED' || res.status === 'PARTIAL_CANCELLED';
            } else if (activeTab === 'EXPIRED') {
              return res.status === 'EXPIRED';
            } else if (activeTab === 'CANCELED') {
              return res.status === 'ALL_CANCELLED';
            }
            return true;
          });

          // 페이지네이션 처리
          const totalPages = Math.ceil(filteredReservations.length / ITEMS_PER_PAGE);
          const paginatedReservations = filteredReservations.slice(
            (currentPage - 1) * ITEMS_PER_PAGE,
            currentPage * ITEMS_PER_PAGE
          );

          if (filteredReservations.length === 0) {
            return (
              <div className="bg-white rounded-2xl p-12 text-center shadow-sm">
                <AlertCircle className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 font-medium">해당하는 예매 내역이 없습니다.</p>
                {activeTab === 'COMPLETED' && (
                  <Link to="/" className="text-[#e50914] font-bold mt-4 inline-block">영화 예매하러 가기</Link>
                )}
              </div>
            );
          }

          return (
            <>
              {paginatedReservations.map((res) => (
                <div key={res.id} className="bg-white rounded-2xl shadow-md overflow-hidden flex flex-col border border-gray-100 transition-all hover:shadow-lg relative">
                  {/* 상단: 영화 정보 */}
                  <div className="p-5 flex gap-4 border-b border-gray-50">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1.5">
                        <img
                          src={getRatingImage(res.movieRating)} 
                          alt={res.movieRating || '전체관람가'} 
                          className="w-5 h-5 object-contain"
                        />
                        <h4 className="font-bold text-gray-900 text-lg line-clamp-1">{res.movieTitle || '영화 제목'}</h4>
                      </div>
                      <div className="text-sm text-gray-600 space-y-1">
                        <p className="font-medium text-gray-800">{res.startDay} {res.startTime}</p>
                        <p>{res.screenName}</p>
                        <p className="text-xs text-gray-500 font-medium">좌석: {res.tickets.map(t => t.seatNumber).join(', ')}</p>
                      </div>
                    </div>
                    <div className={`px-3 py-1.5 h-fit rounded-lg text-xs font-bold ${
                      res.status === 'COMPLETED' ? 'bg-green-50 text-green-600' : 
                      res.status === 'ALL_CANCELLED' ? 'bg-red-50 text-red-500' : 
                      res.status === 'EXPIRED' ? 'bg-gray-100 text-gray-500 border border-gray-200' :
                      'bg-orange-50 text-orange-600'
                    }`}>
                      {res.status === 'COMPLETED' ? '예매완료' : res.status === 'ALL_CANCELLED' ? '전체취소' : res.status === 'EXPIRED' ? '만료됨' : '부분취소'}
                    </div>
                  </div>

                  {/* 중단: 티켓 요약 */}
                  <div className="px-5 py-4 bg-gray-50/50 flex flex-wrap gap-2 items-center justify-between">
                    <div className="flex gap-2">
                  {res.adultCount > 0 && (
                    <span className="bg-white border border-gray-200 px-2.5 py-1 rounded-full text-xs font-semibold text-gray-600 flex items-center gap-1">
                      <Users className="w-3 h-3" /> 일반 {res.adultCount}
                    </span>
                  )}
                  {res.youthCount > 0 && (
                    <span className="bg-white border border-gray-200 px-2.5 py-1 rounded-full text-xs font-semibold text-gray-600 flex items-center gap-1">
                      <Users className="w-3 h-3" /> 청소년 {res.youthCount}
                    </span>
                  )}
                </div>
                <div className="text-right">
                  <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">Total Price</p>
                  <p className="font-bold text-[#e50914] text-lg">
                    {(res.totalPrice - (res.cancelPrice || 0)).toLocaleString()}원
                  </p>
                </div>
              </div>

              {/* 부분 취소 UI 영역 */}
              {cancelModeId === res.id && (
                <div className="p-5 bg-red-50/30 border-y border-red-100 flex flex-col gap-4">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-bold text-gray-800">취소할 좌석 선택</span>
                    <button 
                      onClick={() => handleSelectAll(res)}
                      className="text-xs font-bold text-gray-500 hover:text-gray-900 underline underline-offset-2"
                    >
                      전체 선택
                    </button>
                  </div>
                  
                  <div className="grid grid-cols-8 gap-2 mt-2">
                    {res.tickets.map(t => (
                      <button
                        key={t.id}
                        disabled={t.status === 'CANCELLED'}
                        onClick={() => toggleTicketSelection(t.id)}
                        className={`aspect-square rounded-md text-xs font-bold border transition-all flex items-center justify-center ${
                          t.status === 'CANCELLED' ? 'bg-gray-100 text-gray-300 border-gray-200 cursor-not-allowed' :
                          selectedTicketIds.includes(t.id) ? 'bg-[#e50914] text-white border-[#e50914] shadow-sm' :
                          'bg-white text-gray-600 border-gray-300 hover:border-gray-500'
                        }`}
                        title={t.seatNumber}
                      >
                        {t.seatNumber}
                      </button>
                    ))}
                  </div>

                  {/* 인원 조절 카운터 */}
                  <div className="flex gap-4 items-center justify-between bg-white p-3 rounded-xl border border-red-100">
                    <div className="flex-1 flex flex-col gap-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-gray-600">일반</span>
                        <div className="flex items-center gap-3">
                          <button onClick={() => setAdultCancelCount(Math.max(0, adultCancelCount-1))} className="p-1 rounded-md hover:bg-gray-100"><ChevronDown className="w-4 h-4" /></button>
                          <span className="font-bold text-sm min-w-[12px] text-center">{adultCancelCount}</span>
                          <button onClick={() => setAdultCancelCount(Math.min(res.adultCount, adultCancelCount+1))} className="p-1 rounded-md hover:bg-gray-100"><ChevronUp className="w-4 h-4" /></button>
                        </div>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-gray-600">청소년</span>
                        <div className="flex items-center gap-3">
                          <button onClick={() => setYouthCancelCount(Math.max(0, youthCancelCount-1))} className="p-1 rounded-md hover:bg-gray-100"><ChevronDown className="w-4 h-4" /></button>
                          <span className="font-bold text-sm min-w-[12px] text-center">{youthCancelCount}</span>
                          <button onClick={() => setYouthCancelCount(Math.min(res.youthCount, youthCancelCount+1))} className="p-1 rounded-md hover:bg-gray-100"><ChevronUp className="w-4 h-4" /></button>
                        </div>
                      </div>
                    </div>
                    <div className="w-px h-10 bg-gray-100 mx-2" />
                    <div className="flex flex-col items-center min-w-[60px]">
                      <span className="text-[10px] font-bold text-gray-400 mb-1">SELECTED</span>
                      <span className={`text-lg font-black ${selectedTicketIds.length === (adultCancelCount+youthCancelCount) ? 'text-green-500' : 'text-red-500'}`}>
                        {selectedTicketIds.length}
                      </span>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <button onClick={() => setCancelModeId(null)} className="flex-1 py-3 rounded-xl bg-gray-200 text-gray-700 font-bold text-sm hover:bg-gray-300">닫기</button>
                    <button 
                      onClick={() => handleCancelSubmit(res.id, selectedTicketIds, adultCancelCount, youthCancelCount)}
                      className="flex-[2] py-3 rounded-xl bg-[#e50914] text-white font-bold text-sm hover:bg-red-700 shadow-lg shadow-red-200"
                    >
                      선택 좌석 취소 요청
                    </button>
                  </div>
                </div>
              )}

              {/* 하단: 액션 버튼 (취소 모드가 아닐 때만 노출) */}
              {cancelModeId !== res.id && res.status !== 'ALL_CANCELLED' && canCancel(res.startDay, res.startTime) && (
                <div className="p-4 flex gap-2">
                  <button 
                    onClick={() => enterCancelMode(res)}
                    className="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg border-2 border-[#e50914] text-[#e50914] text-sm font-bold hover:bg-red-50 transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" /> 예매 취소
                  </button>
                </div>
              )}

              {/* 예외: 이미 시간이 지났거나 전체 취소된 경우 안내 */}
              {res.status !== 'ALL_CANCELLED' && res.status !== 'EXPIRED' && !canCancel(res.startDay, res.startTime) && (
                <div className="px-5 py-3 bg-gray-50 flex items-center gap-2 text-gray-400 text-xs font-medium">
                  <Info className="w-3 h-3" /> 상영 시작 20분 전이 지났거나 이미 시작된 영화는 취소가 불가합니다.
                </div>
              )}
            </div>
              ))}
              
              {/* 페이지네이션 컴포넌트 */}
              {totalPages > 1 && (
                <div className="flex justify-center items-center gap-2 mt-4">
                  <button 
                    onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                    disabled={currentPage === 1}
                    className="p-2 rounded-lg bg-white border border-gray-200 text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    <ChevronLeft className="w-5 h-5" />
                  </button>
                  <span className="text-sm font-medium text-gray-600">
                    {currentPage} / {totalPages}
                  </span>
                  <button 
                    onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                    disabled={currentPage === totalPages}
                    className="p-2 rounded-lg bg-white border border-gray-200 text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    <ChevronLeft className="w-5 h-5 rotate-180" />
                  </button>
                </div>
              )}
            </>
          );
        })()}

      </main>
      </div>
    </div>
  );
};

export default MyPage;
