import React, { useState, useEffect } from 'react';
import { AnimatePresence } from 'framer-motion';
import { Search } from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';

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

export function Header() {
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [movies, setMovies] = useState([]);
  const [searchResult, setSearchResult] = useState(null);
  const navigate = useNavigate();

  // 검색을 위한 영화 목록 미리 가져오기
  useEffect(() => {
    fetch('/api/movies')
      .then(res => res.json())
      .then(data => setMovies(data))
      .catch(err => console.error("영화 목록 로드 실패", err));
  }, []);

  const handleSearchChange = (e) => {
    const value = e.target.value;
    setSearchTerm(value);

    if (value.trim() === '') {
      setSearchResult(null);
      return;
    }

    // 공백을 제거하고 실시간 필터링
    const normalizedTerm = value.trim().replace(/\s+/g, '').toLowerCase();
    const matchedMovie = movies.find(movie => 
      movie.title.replace(/\s+/g, '').toLowerCase().includes(normalizedTerm)
    );

    setSearchResult(matchedMovie || null);
  };

  const handleSearch = (e) => {
    e.preventDefault();
    // 공백 무효화 처리 (모든 공백 제거) 및 trim
    const normalizedTerm = searchTerm.trim().replace(/\s+/g, '');
    
    if (normalizedTerm) {
      // 검색어가 있는 경우 검색 페이지로 이동하면서 쿼리 파라미터로 전달
      navigate(`/search?q=${encodeURIComponent(normalizedTerm)}`);
      setSearchTerm(''); // 검색 후 입력창 비우기 (선택 사항)
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch(e);
    }
  };
  return (
    <header
      className="sticky top-0 z-50 px-6 py-4 border-b border-gray-100"
      style={{
        backdropFilter: 'blur(12px)',
        backgroundColor: 'rgba(255, 255, 255, 0.85)'
      }}
    >
      <div className="flex items-center justify-between gap-3 w-full">
        {/* Logo */}
        <Link to="/" className="shrink-0">
          <h1 className="text-xl md:text-2xl font-bold text-[#e50914] tracking-wider whitespace-nowrap hover:opacity-80 transition-opacity">
            AllDayMovie
          </h1>
        </Link>

        {/* Search Bar */}
        <div
          className="w-[200px] md:w-[240px] ml-auto"
        >
          <div className="relative flex items-center group">
            <input
              type="text"
              value={searchTerm}
              onChange={handleSearchChange}
              onFocus={() => setIsSearchFocused(true)}
              onBlur={() => setTimeout(() => setIsSearchFocused(false), 200)}
              onKeyDown={handleKeyDown}
              placeholder="영화명 검색"
              className="w-full h-10 pl-4 pr-10 text-sm text-gray-900 bg-gray-100 rounded-full border-0 outline-none focus:ring-2 focus:ring-[#e50914]/30 transition-all placeholder:text-gray-400 relative z-[60]"
            />
            <button 
              onClick={handleSearch}
              className="absolute right-4 text-gray-500 hover:text-[#e50914] transition-colors"
            >
              <Search className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* 1. 검색 포커스 시 나타나는 반투명 오버레이 및 실시간 검색 결과 */}
      <AnimatePresence>
        {isSearchFocused && (
          <div 
            className="fixed inset-0 top-[73px] h-[calc(100vh-73px)] z-[50] bg-black/80 backdrop-blur-md flex flex-col items-center justify-start pt-10 px-5 transition-opacity overflow-y-auto pb-20"
          >
            {searchTerm.trim() === '' ? (
              <p className="text-white/70 text-sm font-medium">영화 제목을 입력해보세요.</p>
            ) : searchResult ? (
              <div 
                className="w-full max-w-[350px] flex flex-col items-center gap-4 cursor-pointer group"
                onClick={() => {
                  navigate('/booking', { state: { movieId: searchResult.id } });
                  setIsSearchFocused(false);
                }}
              >
                <div className="relative w-full aspect-[2/3] max-w-[500px] rounded-2xl overflow-hidden shadow-2xl border-2 border-transparent group-hover:border-[#e50914] transition-all duration-300 transform group-hover:scale-[1.02]">
                  <img 
                    src={searchResult.poster || '/images/poster_temp.png'} 
                    alt={searchResult.title} 
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/30 to-transparent flex items-end p-6">
                    <div className="flex items-center gap-3">
                      <img 
                        src={getRatingImage(searchResult.rating)} 
                        alt="rating" 
                        className="w-8 h-8 object-contain drop-shadow-md"
                      />
                      <h3 className="text-white font-bold text-3xl drop-shadow-lg leading-tight break-keep">
                        {searchResult.title}
                      </h3>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center bg-white/10 p-8 rounded-2xl backdrop-blur-md border border-white/20 max-w-[400px] shadow-2xl">
                <p className="text-white font-bold text-lg mb-3">검색어에 해당하는 영화가 없습니다.</p>
                <p className="text-gray-300 text-sm leading-relaxed">
                  우측 하단의 AI 추천 버튼을 통해<br />
                  원하는 영화를 폭넓게 검색해보세요!
                </p>
              </div>
            )}
          </div>
        )}
      </AnimatePresence>

    </header>
  );
}
