import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ChevronLeft, Clock } from 'lucide-react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { FreeMode } from 'swiper/modules';
import useAuthStore from '../store/useAuthStore';
import 'swiper/css';
import 'swiper/css/free-mode';

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

/**
 * 영화 예매 페이지 컴포넌트.
 * @author ohseju
 */
const BookingPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const initialMovieId = location.state?.movieId;
  const initialDate = location.state?.date;
  const { isLoggedIn, birthDate } = useAuthStore(); // 로그인 상태 및 유저 정보 가져오기

  const [movies, setMovies] = useState([]); 
  const [filteredMovies, setFilteredMovies] = useState([]); 
  const [selectedMovie, setSelectedMovie] = useState(null); // null이면 '전체 영화' 선택 상태
  // initialDate가 있으면 우선 적용, 없으면 오늘 날짜 사용
  const [selectedDate, setSelectedDate] = useState(initialDate || new Date().toISOString().split('T')[0]);
  const [dates, setDates] = useState([]);
  const [showtimes, setShowtimes] = useState([]); 
  const [allShowtimes, setAllShowtimes] = useState([]); 
  const [isLoading, setIsLoading] = useState(false);
  const [dateSchedules, setDateSchedules] = useState({}); 
  const [swiperInstance, setSwiperInstance] = useState(null);

  // 전체 영화 모드에서 영화별 아코디언 토글 상태 관리
  // 구조: { [movieId]: boolean }
  const [expandedMovies, setExpandedMovies] = useState({});

  // 임박 회차 경고 팝업 상태
  const [showWarningModal, setShowWarningModal] = useState(false);
  const [pendingShowtime, setPendingShowtime] = useState(null);

  // 1. 날짜 데이터 생성 (오늘 ~ 7일 뒤)
  useEffect(() => {
    const tempDates = [];
    const today = new Date();
    for (let i = 0; i < 8; i++) {
      const date = new Date();
      date.setDate(today.getDate() + i);
      tempDates.push({
        fullDate: date.toISOString().split('T')[0],
        day: date.getDate(),
        dayOfWeek: date.getDay(),
        isToday: i === 0
      });
    }
    setDates(tempDates);
  }, []);

  // 2. 영화 목록 조회
  useEffect(() => {
    const fetchMovies = async () => {
      try {
        const response = await fetch('/api/movies');
        const data = await response.json();
        setMovies(data);
      } catch (err) {
        console.error('영화 목록 조회 실패:', err);
      }
    };
    fetchMovies();
  }, []);

  // 3. 현재 선택된 날짜에 상영 회차가 있는 영화만 필터링
  useEffect(() => {
    const checkMovieSchedules = async () => {
      if (movies.length === 0) return;

      const results = await Promise.all(movies.map(async (movie) => {
        const response = await fetch(`/api/movies/booking/showtimes?movieId=${movie.id}&date=${selectedDate}`);
        const data = await response.json();
        return { ...movie, hasShowtime: data.length > 0 };
      }));

      const filtered = results.filter(m => m.hasShowtime);
      setFilteredMovies(filtered);

      // URL 파라미터 우선 적용 로직
      if (initialMovieId && !selectedMovie) {
        const target = filtered.find(m => m.id === initialMovieId);
        if (target) {
          setSelectedMovie(target);
          return;
        }
      }

      // 기본 선택 로직
      if (selectedMovie !== null) {
        if (filtered.length > 0) {
          if (!filtered.find(m => m.id === selectedMovie.id)) {
            setSelectedMovie(filtered[0]);
          }
        } else {
          setSelectedMovie(null); 
        }
      }
    };

    checkMovieSchedules();
  }, [selectedDate, movies, initialMovieId]);

  // 4. 특정 날짜들에 스케줄이 있는지 체크 (일력 비활성화용)
  useEffect(() => {
    const checkAllDateSchedules = async () => {
      if (movies.length === 0) return;
      
      const newSchedules = {};
      await Promise.all(dates.map(async (d) => {
        const checks = await Promise.all(movies.map(async (m) => {
          const res = await fetch(`/api/movies/booking/showtimes?movieId=${m.id}&date=${d.fullDate}`);
          const data = await res.json();
          return data.length > 0;
        }));
        newSchedules[d.fullDate] = checks.some(c => c === true);
      }));
      setDateSchedules(newSchedules);
    };

    checkAllDateSchedules();
  }, [movies, dates]);

  // 5. 회차 정보 조회
  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        if (selectedMovie) {
          const response = await fetch(`/api/movies/booking/showtimes?movieId=${selectedMovie.id}&date=${selectedDate}`);
          const data = await response.json();
          setShowtimes(data);
          setAllShowtimes([]);
        } else if (filteredMovies.length > 0) {
          const groupResults = await Promise.all(filteredMovies.map(async (movie) => {
            const response = await fetch(`/api/movies/booking/showtimes?movieId=${movie.id}&date=${selectedDate}`);
            const data = await response.json();
            return { movie, showtimes: data };
          }));
          
          // 상영 회차가 존재하는 영화만 그룹에 추가
          const validGroupResults = groupResults.filter(group => group.showtimes.length > 0);
          setAllShowtimes(validGroupResults);
          
          // 처음 로딩될 때 첫 번째 영화만 확장되도록 상태 초기화
          if (validGroupResults.length > 0) {
            const initialExpanded = {};
            validGroupResults.forEach((group, index) => {
              initialExpanded[group.movie.id] = index === 0; // 첫 번째만 true
            });
            setExpandedMovies(initialExpanded);
          }
          
          setShowtimes([]);
        } else {
          setShowtimes([]);
          setAllShowtimes([]);
          setExpandedMovies({});
        }
      } catch (err) {
        console.error('회차 정보 조회 실패:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [selectedMovie, selectedDate, filteredMovies]);

  // 아코디언 토글 핸들러
  const toggleMovieAccordion = (movieId) => {
    console.log(`Toggling accordion for movie: ${movieId}`);
    setExpandedMovies(prev => ({
      ...prev,
      [movieId]: !prev[movieId]
    }));
  };

  // 6. 초기 렌더링 시 선택된 영화가 스와이퍼 중앙에 위치하도록 슬라이드 이동
  useEffect(() => {
    if (swiperInstance && selectedMovie && filteredMovies.length > 0) {
      const movieIndex = filteredMovies.findIndex(m => m.id === selectedMovie.id);
      if (movieIndex !== -1) {
        setTimeout(() => {
          swiperInstance.slideTo(movieIndex + 1, 300);
        }, 100);
      }
    }
  }, [swiperInstance, selectedMovie, filteredMovies]);

  const formatRuntime = (runtime) => {
    if (!runtime || runtime <= 0) return '2시간 0분';
    const hours = Math.floor(runtime / 60);
    const minutes = runtime % 60;
    return hours > 0 ? `${hours}시간 ${minutes}분` : `${minutes}분`;
  };

  const getDayName = (dayOfWeek, isToday) => {
    if (isToday) return '오늘';
    const names = ['일', '월', '화', '수', '목', '금', '토'];
    return names[dayOfWeek];
  };

  // 만 나이 계산 함수
  const calculateAge = (birthDateString) => {
    if (!birthDateString) return 0;
    const today = new Date();
    const birthDate = new Date(birthDateString);
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  };

  // 회차 클릭 핸들러 (인증/인가 및 20분 임박 여부 체크)
  const handleShowtimeClick = (st) => {
    // 2. 청소년 관람 불가 영화 등 타겟 확인용
    const targetMovie = selectedMovie || allShowtimes.find(group => group.showtimes.some(s => s.id === st.id))?.movie;

    // 1. 로그인 여부 확인
    if (!isLoggedIn) {
      alert('로그인이 필요한 서비스입니다.');
      // 로그인 후 사용자가 보던 정보를 유지하기 위해 영화 ID와 날짜를 넘김
      navigate('/login', { 
        state: { 
          from: location.pathname,
          movieId: targetMovie?.id,
          date: selectedDate
        } 
      });
      return;
    }

    // 3. 청소년 관람 불가 영화 체크 (만 18세 미만 차단)
    if (targetMovie && targetMovie.rating) {
      const isAdultMovie = targetMovie.rating.includes('18') || targetMovie.rating.includes('19') || targetMovie.rating.includes('청소년');
      if (isAdultMovie) {
        const age = calculateAge(birthDate);
        if (age < 18) {
          alert('해당 영화는 청소년 관람 불가입니다.');
          return; // 진행 차단
        }
      }
    }

    // 4. 20분 이내 시작 여부 체크
    const now = new Date();
    const startTimeParts = st.startTime.split(':');
    const showtimeDate = new Date(selectedDate);
    showtimeDate.setHours(parseInt(startTimeParts[0], 10), parseInt(startTimeParts[1], 10), 0, 0);

    const diffInMinutes = (showtimeDate - now) / (1000 * 60);

    if (diffInMinutes <= 20) {
      setPendingShowtime(st);
      setShowWarningModal(true);
    } else {
      navigate('/seat-selection', { 
        state: { 
          movie: targetMovie, 
          showtime: st, 
          date: selectedDate 
        } 
      });
    }
  };

  const confirmWarning = () => {
    const targetMovie = selectedMovie || allShowtimes.find(group => group.showtimes.some(s => s.id === pendingShowtime.id))?.movie;
    setShowWarningModal(false);
    navigate('/seat-selection', { 
      state: { 
        movie: targetMovie, 
        showtime: pendingShowtime, 
        date: selectedDate 
      } 
    });
    setPendingShowtime(null);
  };

  const cancelWarning = () => {
    setShowWarningModal(false);
    setPendingShowtime(null);
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center font-sans text-gray-900">
      <div className="w-full max-w-[600px] flex-1 bg-white shadow-2xl border-x border-gray-100 flex flex-col relative">
        
        {/* 상단 헤더 */}
        <header className="flex items-center px-4 py-4 border-b border-gray-100 bg-white relative z-10">
          <button onClick={() => navigate(-1)} className="p-2 -ml-2 cursor-pointer">
            <ChevronLeft className="w-6 h-6 text-gray-800" />
          </button>
          <h1 className="flex-1 text-center text-lg font-bold mr-8">영화 예매</h1>
        </header>

        {/* 영화 선택 Swiper (어두운 그라디언트 및 텍스트 반전) */}
        <section className="py-6 bg-gradient-to-b from-gray-900 to-black overflow-hidden relative z-0">
          <div className="px-4">
            <Swiper
              modules={[FreeMode]}
              slidesPerView={'auto'}
              spaceBetween={12}
              freeMode={true}
              grabCursor={true}
              centeredSlides={true}
              onSwiper={setSwiperInstance}
              className="!overflow-visible flex items-end"
            >
              {/* 전체 영화 보기 버튼 슬라이드 */}
              <SwiperSlide style={{ width: '90px' }} className="pt-5 pb-2 self-end">
                <div 
                  onClick={() => setSelectedMovie(null)}
                  className="cursor-pointer flex flex-col items-center justify-end h-[150px]"
                >
                  <div 
                    className={`w-full flex flex-col items-center justify-center rounded-lg shadow-md transition-all duration-300 origin-bottom ${
                      selectedMovie === null 
                        ? 'bg-[#e50914] text-white border-2 border-[#e50914] h-[135px] opacity-100 scale-110' 
                        : 'bg-gray-800 text-gray-400 h-[120px] opacity-60 hover:opacity-80'
                    }`}
                  >
                    <span className="text-[13px] font-bold">전체 영화</span>
                    <span className="text-[11px]">보기</span>
                  </div>
                </div>
              </SwiperSlide>

              {filteredMovies.map((movie) => {
                const isSelected = selectedMovie?.id === movie.id;
                return (
                  <SwiperSlide key={movie.id} style={{ width: '90px' }} className="pt-5 pb-2 self-end">
                    <div 
                      onClick={() => setSelectedMovie(movie)}
                      className="cursor-pointer flex flex-col items-center justify-end h-[150px]"
                    >
                      <img 
                        src={movie.poster || '/images/poster_temp.png'} 
                        alt={movie.title}
                        className={`rounded-lg shadow-xl object-cover transition-all duration-300 origin-bottom ${
                          isSelected 
                            ? 'w-[90px] h-[135px] border-2 border-[#e50914] opacity-100 z-10 scale-110' 
                            : 'w-[80px] h-[120px] opacity-40 grayscale-[30%] hover:opacity-70 hover:grayscale-[10%]'
                        }`}
                      />
                    </div>
                  </SwiperSlide>
                );
              })}
            </Swiper>
          </div>

          {/* 선택된 영화 정보 (단일 선택 시에만 노출) */}
          {selectedMovie && (
            <div className="px-6 mt-4 text-center animate-fade-in relative z-10">
              <div className="flex items-center justify-center gap-2 mb-1">
                <span className="text-lg font-bold text-white drop-shadow-md">{selectedMovie.title}</span>
                <img 
                  src={getRatingImage(selectedMovie.rating)} 
                  alt="rating" 
                  className="w-5 h-5 object-contain drop-shadow-md"
                />
              </div>
              <div className="flex items-center justify-center gap-1 text-gray-300 text-xs">
                <Clock className="w-3 h-3" />
                <span>{formatRuntime(selectedMovie.runtime)}</span>
              </div>
            </div>
          )}
          {!selectedMovie && filteredMovies.length > 0 && (
            <div className="px-6 mt-[30px] text-center text-gray-400 text-xs italic h-[34px] flex items-center justify-center relative z-10">
              * 전체 영화의 상영 일정을 확인 중입니다.
            </div>
          )}
        </section>

        {/* 일력 (Date Picker) */}
        <section className="py-4 border-b border-gray-100 bg-white">
          <div className="flex overflow-x-auto gap-2 px-4 no-scrollbar">
            {dates.map((d) => {
              const isSelected = selectedDate === d.fullDate;
              const isSunday = d.dayOfWeek === 0;
              const isSaturday = d.dayOfWeek === 6;
              const hasSchedule = dateSchedules[d.fullDate];

              let textColor = 'text-gray-900';
              if (isSunday) textColor = 'text-red-500';
              else if (isSaturday) textColor = 'text-blue-500';

              return (
                <button
                  key={d.fullDate}
                  onClick={() => setSelectedDate(d.fullDate)}
                  disabled={!hasSchedule && !d.isToday}
                  className={`flex flex-col items-center justify-center min-w-[50px] py-2 rounded-xl transition-all cursor-pointer ${
                    isSelected ? 'bg-[#e50914] !text-white' : 
                    (!hasSchedule && !d.isToday) ? 'bg-gray-50 text-gray-300 cursor-not-allowed' : 'hover:bg-gray-100'
                  }`}
                >
                  <span className={`text-[13px] font-medium mb-1 ${isSelected ? 'text-white' : ( (!hasSchedule && !d.isToday) ? 'text-gray-300' : 'text-gray-900')}`}>
                    {getDayName(d.dayOfWeek, d.isToday)}
                  </span>
                  <span className={`text-[13px] font-bold ${isSelected ? 'text-white' : ( (!hasSchedule && !d.isToday) ? 'text-gray-200' : textColor)}`}>
                    {d.day}
                  </span>
                </button>
              );
            })}
          </div>
        </section>

        {/* 회차 목록 (Showtime Grid) */}
        <section className="p-4 flex-1 overflow-y-auto no-scrollbar bg-white relative z-0">
          {isLoading ? (
            <div className="py-20 text-center text-gray-400">데이터를 불러오는 중...</div>
          ) : selectedMovie ? (
            /* 단일 영화 모드 */
            showtimes.length > 0 ? (
              <div className="grid grid-cols-3 gap-3">
                {showtimes.map((st) => (
                  <ShowtimeItem key={st.id} st={st} startDay={selectedDate} onClick={() => handleShowtimeClick(st)} />
                ))}
              </div>
            ) : (
              <div className="py-20 text-center text-gray-400">상영 스케줄이 없습니다.</div>
            )
          ) : (
            /* 전체 영화 모드 */
            allShowtimes.length > 0 ? (
              <div className="flex flex-col gap-4">
                {allShowtimes.map(({ movie, showtimes: movieShowtimes }) => (
                  <div key={movie.id} className="flex flex-col border border-gray-200 rounded-xl bg-gray-50 overflow-hidden transition-all shadow-sm">
                    {/* 아코디언 헤더 (클릭 시 토글) */}
                    <div 
                      className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-100 transition-colors"
                      onClick={() => toggleMovieAccordion(movie.id)}
                    >
                      <div className="flex items-center gap-3">
                        <img 
                          src={getRatingImage(movie.rating)} 
                          alt="rating" 
                          className="w-5 h-5 object-contain"
                        />
                        <span className="font-bold text-gray-900 text-base">{movie.title}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="flex items-center gap-1 text-xs text-gray-500 font-medium bg-white px-2 py-1 rounded-md border border-gray-200">
                          <Clock className="w-3 h-3" />
                          <span>{formatRuntime(movie.runtime)}</span>
                        </div>
                        <ChevronLeft className={`w-5 h-5 text-gray-400 transition-transform duration-300 ${expandedMovies[movie.id] ? '-rotate-90' : ''}`} />
                      </div>
                    </div>

                    {/* 상영 회차 리스트 (토글 상태에 따라 노출) */}
                    <div 
                      className={`transition-all duration-300 ease-in-out overflow-hidden ${expandedMovies[movie.id] ? 'max-h-[1000px] opacity-100' : 'max-h-0 opacity-0'}`}
                    >
                      <div className="p-4 pt-2 border-t border-gray-100 bg-white">
                        <div className="grid grid-cols-3 gap-3">
                          {movieShowtimes.map((st) => (
                            <ShowtimeItem key={st.id} st={st} startDay={selectedDate} onClick={() => handleShowtimeClick(st)} />
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-20 text-center text-gray-400">상영 스케줄이 없습니다.</div>
            )
          )}
        </section>

        {/* 20분 임박 회차 경고 모달 (전체 페이지 오버레이) */}
        {showWarningModal && (
          <div className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4 pointer-events-auto">
            <div className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-[400px] flex flex-col">
              <h3 className="text-lg font-bold text-gray-900 mb-4 text-center">예매 진행 확인</h3>
              
              <p className="text-sm text-gray-600 mb-6 text-center leading-relaxed">
                선택하신 시간은 곧 영화가 시작되는 일정입니다.<br/>
                취소 및 환불 규정을 확인 후 예매해 주세요.<br/>
                <span className="font-semibold text-gray-800 mt-2 block">예매를 계속 진행하시겠습니까?</span>
              </p>

              <div className="flex gap-3 mt-auto">
                <button
                  onClick={cancelWarning}
                  className="flex-1 py-3 rounded-lg border border-gray-300 text-gray-600 font-semibold hover:bg-gray-50 transition-colors cursor-pointer"
                >
                  취소
                </button>
                <button
                  onClick={confirmWarning}
                  className="flex-1 py-3 rounded-lg bg-[#e50914] text-white font-semibold hover:bg-red-700 transition-colors cursor-pointer"
                >
                  확인
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

/**
 * 개별 상영 회차 아이템 컴포넌트
 * 상영 일시(startDay + startTime)와 현재 시간(now)을 비교해 지난 회차인지 검사합니다.
 */
const ShowtimeItem = ({ st, startDay, onClick }) => {
  // 현재 시간과 상영 시간 비교
  const now = new Date();
  const showtimeDate = new Date(`${startDay}T${st.startTime}:00`);
  const isPast = showtimeDate < now;

  return (
    <button
      onClick={isPast ? undefined : onClick}
      disabled={isPast}
      className={`flex flex-col items-center justify-between p-3 border rounded-lg transition-all text-left min-h-[90px] ${
        isPast 
          ? 'bg-gray-100 border-gray-200 cursor-not-allowed opacity-60 grayscale-[50%]' 
          : 'bg-white border-gray-200 hover:border-[#e50914] hover:bg-red-50 cursor-pointer shadow-sm'
      }`}
    >
      <div className="flex flex-col items-center w-full">
        <div className="flex items-end gap-1 mb-1">
          <span className={`text-lg font-bold leading-none ${isPast ? 'text-gray-500' : 'text-gray-900'}`}>
            {st.startTime}
          </span>
          <span className="text-[10px] text-gray-400 leading-none pb-[2px]">~{st.endTime}</span>
        </div>
        <div className="w-full border-t border-dashed border-gray-200 my-1.5" />
      </div>
      <div className="flex flex-col items-center w-full">
        <span className={`text-xs font-medium mb-1 ${isPast ? 'text-gray-400' : 'text-gray-600'}`}>
          {st.screenName}
        </span>
        <span className="text-sm font-bold">
          <span className={isPast ? 'text-gray-400' : 'text-[#e50914]'}>
            {st.remainingSeats}
          </span>
          <span className="text-gray-400 text-xs"> / {st.totalSeats}</span>
        </span>
      </div>
    </button>
  );
};

export default BookingPage;
