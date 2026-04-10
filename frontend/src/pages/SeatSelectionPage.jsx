import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { ChevronLeft, X, Info } from 'lucide-react';
import useAuthStore from '../store/useAuthStore';

const getRatingImage = (ratingStr) => {
  if (!ratingStr) return '/images/rating_all.png';
  if (ratingStr.includes('19') || ratingStr.includes('18') || ratingStr.includes('청소년')) return '/images/rating_19.png';
  if (ratingStr.includes('15')) return '/images/rating_15.png';
  if (ratingStr.includes('12')) return '/images/rating_12.png';
  return '/images/rating_all.png';
};

const SeatSelectionPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { username, token } = useAuthStore();
  
  const movie = location.state?.movie;
  const showtime = location.state?.showtime;
  const selectedDate = location.state?.date;

  const [adultCount, setAdultCount] = useState(0);
  const [youthCount, setYouthCount] = useState(0);
  const totalCount = adultCount + youthCount;

  const [seats, setSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  
  const [toastMsg, setToastMsg] = useState('');
  const [showToast, setShowToast] = useState(false);
  const toastTimeout = useRef(null);

  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  // 비정상 접근 처리
  useEffect(() => {
    if (!movie || !showtime) {
      alert("비정상적인 접근입니다.");
      navigate('/booking', { replace: true });
    }
  }, [movie, showtime, navigate]);

  // 상영시간 체크
  useEffect(() => {
    if (!showtime || !selectedDate) return;
    const now = new Date();
    const startTimeParts = showtime.startTime.split(':');
    const showtimeDate = new Date(selectedDate);
    showtimeDate.setHours(parseInt(startTimeParts[0], 10), parseInt(startTimeParts[1], 10), 0, 0);

    if (now >= showtimeDate) {
      alert("예매가 불가능한 회차입니다.");
      navigate('/', { replace: true }); 
    }
  }, [showtime, selectedDate, navigate]);

  // 좌석 데이터 조회
  const fetchSeats = async () => {
    if (!showtime) return;
    try {

      const response = await fetch(`/api/seats/status?showtimeId=${showtime.id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      // 토큰 만료 등 인증 에러 처리
      if (response.status === 401) {
        console.warn("인증이 유효하지 않습니다.");
        return;
      }

      const data = await response.json();
      setSeats(data);

      // 누군가 먼저 예매한 좌석은 내 선택 목록에서 제거
      setSelectedSeats(prev => prev.filter(s => {
        const found = data.find(ds => ds.seatNumber === s.seatNumber);
        return found && found.status === 'AVAILABLE';
      }));
    } catch (err) {
      console.error('좌석 조회 실패:', err);
    }
  };

  useEffect(() => {
    fetchSeats();
    const intervalId = setInterval(fetchSeats, 3000); 
    return () => clearInterval(intervalId);
  }, [showtime]);

  const showToastMessage = (msg) => {
    setToastMsg(msg);
    setShowToast(true);
    if (toastTimeout.current) clearTimeout(toastTimeout.current);
    toastTimeout.current = setTimeout(() => {
      setShowToast(false);
    }, 2500);
  };

  const handleCountChange = (type, val) => {
    let newAdult = adultCount;
    let newYouth = youthCount;

    if (type === 'adult') newAdult = val;
    if (type === 'youth') newYouth = val;

    const newTotal = newAdult + newYouth;
    if (newTotal > 8) {
      showToastMessage('총 8명 이하로 선택 가능합니다.');
      return;
    }
    if (newTotal < selectedSeats.length) {
      showToastMessage('선택한 좌석이 관람 인원보다 많습니다. 좌석을 먼저 취소해주세요.');
      return;
    }

    setAdultCount(newAdult);
    setYouthCount(newYouth);
  };

  const handleSeatClick = (seat) => {
    if (totalCount === 0) {
      showToastMessage('관람 인원을 먼저 선택해주세요.');
      return;
    }

    const isAlreadySelected = selectedSeats.some(s => s.seatNumber === seat.seatNumber);
    if (isAlreadySelected) {
      setSelectedSeats(prev => prev.filter(s => s.seatNumber !== seat.seatNumber));
      return;
    }

    if (selectedSeats.length >= totalCount) {
      showToastMessage(`선택하신 관람인원은 ${totalCount}명입니다.`);
      return;
    }

    if (seat.status !== 'AVAILABLE') {
      showToastMessage('이미 예매된 좌석입니다.');
      return;
    }

    setSelectedSeats([...selectedSeats, seat]);
  };

  const handlePayment = async () => {
    if (totalCount === 0 || selectedSeats.length !== totalCount) return;

    setIsProcessing(true);
    const requestBody = {
      username: username,
      showtimeId: showtime.id,
      selectedSeats: selectedSeats.map(s => s.seatNumber),
      adultCount: adultCount,
      youthCount: youthCount
    };

    try {
      const res = await fetch('/api/reservations/book', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify(requestBody)
      });

if (res.ok) {
        setShowPaymentModal(true);
      } else if (res.status === 401) {
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
        navigate('/login');
      } else if (res.status === 409 || res.status === 400) {
        const errorData = await res.json();
        alert(errorData.message || '이미 예매된 좌석이 포함되어 있습니다. 좌석을 다시 선택해주세요.');
        fetchSeats();
      } else {
        alert('결제 처리 중 서버 오류가 발생했습니다.');
      }
    } catch (e) {
      console.error(e);
      alert('네트워크 오류가 발생했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  if (!movie || !showtime) return null;

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center font-sans text-gray-900 overflow-x-hidden">
      <div className="w-full max-w-[600px] min-h-screen bg-white shadow-2xl border-x border-gray-100 flex flex-col relative pb-[140px]">
        
        {/* 상단 헤더 */}
        <header className="flex items-center justify-between px-4 py-4 border-b border-gray-100 bg-white sticky top-0 z-30">
          <button onClick={() => navigate(-1)} className="p-2 -ml-2 cursor-pointer">
            <ChevronLeft className="w-6 h-6 text-gray-800" />
          </button>
          <div className="flex flex-col items-center">
            <h1 className="text-lg font-bold">인원/좌석 선택</h1>
          </div>
          <Link to="/" className="p-2 -mr-2 cursor-pointer text-gray-500 hover:text-gray-800">
            <X className="w-6 h-6" />
          </Link>
        </header>

        {/* 영화 정보 영역 */}
        <section className="relative h-[120px] overflow-hidden flex flex-col justify-end p-4 shrink-0">
          <div 
            className="absolute inset-0 bg-cover bg-center bg-no-repeat blur-[2px]"
            style={{ backgroundImage: `url(${movie.poster || '/images/poster_temp.png'})` }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/50 to-black/30" />
          
          <div className="relative z-10 flex flex-col">
            <div className="flex items-center gap-2 mb-1">
              <img 
                src={getRatingImage(movie.rating)} 
                alt="rating" 
                className="w-5 h-5 object-contain"
              />
              <span className="text-lg font-bold text-white drop-shadow-md truncate">{movie.title}</span>
            </div>
            <div className="flex items-center gap-2 text-gray-300 text-sm font-medium">
              <span>{selectedDate}</span>
              <span className="w-1 h-1 rounded-full bg-gray-400" />
              <span>{showtime.startTime} ~ {showtime.endTime}</span>
              <span className="w-1 h-1 rounded-full bg-gray-400" />
              <span>{showtime.screenName}</span>
            </div>
          </div>
        </section>

        {/* 메인 스크롤 영역 */}
        <section className="flex-1 flex flex-col overflow-y-auto no-scrollbar">
          
          {/* 인원 선택 */}
          <div className="p-6 border-b border-gray-100">
            <h2 className="text-base font-bold mb-4">관람 인원 선택</h2>
            <div className="flex flex-col gap-6">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold w-16">일반</span>
                <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
                  {[0,1,2,3,4,5,6,7,8].map(num => (
                    <button key={`adult-${num}`} onClick={() => handleCountChange('adult', num)}
                      className={`min-w-[40px] h-[40px] rounded-lg font-bold transition-all ${adultCount === num ? 'bg-gray-900 text-white shadow-md' : 'bg-white border border-gray-200 text-gray-600 hover:border-gray-400'}`}>
                      {num}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold w-16">청소년</span>
                <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
                  {[0,1,2,3,4,5,6,7,8].map(num => (
                    <button key={`youth-${num}`} onClick={() => handleCountChange('youth', num)}
                      className={`min-w-[40px] h-[40px] rounded-lg font-bold transition-all ${youthCount === num ? 'bg-gray-900 text-white shadow-md' : 'bg-white border border-gray-200 text-gray-600 hover:border-gray-400'}`}>
                      {num}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* 좌석 선택 (SCREEN & 10열 Grid) */}
          <div className="p-6 flex flex-col items-center border-b border-gray-100 bg-gray-50 flex-1">
            <h2 className="text-base font-bold mb-6 self-start w-full">좌석 선택</h2>
            <div className="w-[80%] max-w-[400px] h-[30px] bg-gray-300 rounded-t-xl mb-12 flex items-center justify-center text-gray-600 font-bold tracking-[0.5em] shadow-inner">SCREEN</div>
            {seats.length > 0 ? (
              <div className="grid grid-cols-10 gap-x-2 gap-y-3 px-2">
                {seats.map((seat) => {
                  const isMySelected = selectedSeats.some(s => s.seatNumber === seat.seatNumber);
                  const isUnavailable = seat.status === 'RESERVED' && !isMySelected;
                  return (
                    <button key={seat.seatNumber} disabled={isUnavailable} onClick={() => handleSeatClick(seat)}
                      className={`w-7 h-7 sm:w-9 sm:h-9 rounded-t-lg rounded-b-sm text-[9px] sm:text-[11px] font-bold flex items-center justify-center transition-all ${isMySelected ? 'bg-[#e50914] text-white shadow-md shadow-red-500/40 cursor-pointer' : isUnavailable ? 'bg-gray-300 text-transparent cursor-not-allowed' : 'bg-white border border-gray-300 text-gray-500 hover:border-[#e50914] cursor-pointer'}`}>
                      {seat.seatNumber}
                    </button>
                  );
                })}
              </div>
            ) : (
              <div className="py-10 text-gray-400 text-sm">좌석 정보를 불러오는 중입니다...</div>
            )}
            <div className="flex gap-4 mt-12 text-xs font-medium text-gray-500">
              <div className="flex items-center gap-1.5"><div className="w-4 h-4 rounded-sm bg-white border border-gray-300" /><span>예매가능</span></div>
              <div className="flex items-center gap-1.5"><div className="w-4 h-4 rounded-sm bg-[#e50914]" /><span>선택</span></div>
              <div className="flex items-center gap-1.5"><div className="w-4 h-4 rounded-sm bg-gray-300" /><span>예매완료</span></div>
            </div>
          </div>

          {/* 취소 환불 규정 */}
          <div className="p-6 bg-white text-gray-600 text-xs leading-relaxed space-y-2">
            <h3 className="font-bold text-gray-800 text-sm mb-3">취소/환불 정책</h3>
            <p className="flex gap-2"><Info className="w-4 h-4 shrink-0 text-gray-400" />온라인 예매는 영화 상영시간 20분 전까지 취소 가능하며, 20분 이후 현장 취소만 가능합니다.</p>
            <p className="flex gap-2"><Info className="w-4 h-4 shrink-0 text-gray-400" />현장 취소 시 영화 상영시간 이전까지만 가능합니다.</p>
          </div>
        </section>

        {/* 결제 버튼 영역 (화면 하단 고정) */}
        <div className="fixed bottom-0 w-full max-w-[600px] p-4 bg-white border-t border-gray-100 z-40 shadow-[0_-10px_20px_rgba(0,0,0,0.05)]">
          <div className="flex justify-between items-end mb-4 px-2">
            <div className="flex flex-col">
              <span className="text-xs text-gray-500 font-medium">총 결제금액</span>
              <span className="text-xl font-bold text-[#e50914]">{((adultCount * 15000) + (youthCount * 12000)).toLocaleString()}원</span>
            </div>
            <div className="text-sm font-semibold text-gray-700">선택 좌석 <span className="text-[#e50914]">{selectedSeats.length}</span> / {totalCount}</div>
          </div>
          <button disabled={totalCount === 0 || selectedSeats.length !== totalCount || isProcessing} onClick={handlePayment}
            className={`w-full h-14 rounded-xl font-bold text-lg transition-all ${totalCount > 0 && selectedSeats.length === totalCount && !isProcessing ? 'bg-[#e50914] text-white shadow-lg shadow-red-500/30 cursor-pointer' : 'bg-gray-200 text-gray-400 cursor-not-allowed'}`}>
            {isProcessing ? '결제 처리 중...' : '결제하기'}
          </button>
        </div>

        {/* 토스트 알림 (하단 고정 바 위에 위치) */}
        <div className={`fixed bottom-[130px] left-1/2 -translate-x-1/2 w-max max-w-[90%] z-[60] transition-all duration-300 ${showToast ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4 pointer-events-none'}`}>
          <div className="bg-gray-900/90 text-white px-8 py-3.5 rounded-full text-sm font-semibold shadow-2xl backdrop-blur-sm">
            {toastMsg}
          </div>
        </div>

        {/* 결제 완료 모달 */}
        {showPaymentModal && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 backdrop-blur-sm px-4 pointer-events-auto">
            <div className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-[400px] flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center mb-4">
                <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
              </div>
              <h3 className="text-lg font-bold text-gray-900 mb-2 text-center">결제 완료</h3>
              <p className="text-sm text-gray-600 mb-6 text-center leading-relaxed">결제 기능은 추후 구현될 예정입니다.<br/>테스트 예매가 완료되었습니다.</p>
              <div className="flex gap-3 mt-auto w-full">
                <button onClick={() => navigate('/')} className="flex-1 py-3 rounded-lg border border-gray-300 text-gray-600 font-semibold hover:bg-gray-50 transition-colors cursor-pointer">메인으로</button>
                <button onClick={() => navigate('/mypage')} className="flex-1 py-3 rounded-lg bg-[#e50914] text-white font-semibold hover:bg-red-700 transition-colors cursor-pointer">예매 내역 확인</button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default SeatSelectionPage;
