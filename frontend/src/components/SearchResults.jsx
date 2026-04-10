import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

const getRatingImage = (ratingStr) => {
  if (!ratingStr) return '/images/rating_all.png';
  if (ratingStr.includes('19') || ratingStr.includes('18') || ratingStr.includes('청소년')) return '/images/rating_19.png';
  if (ratingStr.includes('15')) return '/images/rating_15.png';
  if (ratingStr.includes('12')) return '/images/rating_12.png';
  return '/images/rating_all.png';
};

const SearchResults = () => {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const navigate = useNavigate();

  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  // 1. 전체 영화 목록 패치
  useEffect(() => {
    fetch('/api/movies')
      .then(res => res.json())
      .then(data => {
        setMovies(data);
      })
      .catch(err => {
        console.error("영화 목록을 불러오지 못했습니다.", err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // 2. 검색어(query) 또는 전체 영화 목록(movies)이 변경될 때마다 파생 상태로 필터링 (불필요한 useEffect 및 setState 방지)
  const filteredMovies = React.useMemo(() => {
    if (!query || movies.length === 0) return [];
    
    const normalizedQuery = query.trim().replace(/\s+/g, '').toLowerCase();
    
    return movies.filter((movie) => {
      const normalizedTitle = (movie.title || '').trim().replace(/\s+/g, '').toLowerCase();
      return normalizedTitle.includes(normalizedQuery);
    });
  }, [query, movies]);

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 font-sans flex flex-col items-center">
      {/* 모바일 특화 max-w-[600px] 컨테이너 */}
      <div className="w-full max-w-[600px] min-h-screen bg-white shadow-2xl relative flex flex-col">
        
        {/* 상단 뒤로가기 및 검색어 표시 헤더 */}
        <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-gray-100 px-4 py-4 flex items-center gap-3">
          <button 
            onClick={() => navigate(-1)}
            className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-bold text-gray-800 line-clamp-1">
            <span className="text-[#e50914]">'{query}'</span> 검색 결과
          </h1>
        </header>

        {/* 메인 컨텐츠 영역 */}
        <main className="flex-1 p-4 pb-24 overflow-y-auto">
          {loading ? (
            <div className="flex justify-center items-center h-40">
              <p className="text-gray-500">검색 결과를 불러오는 중입니다...</p>
            </div>
          ) : filteredMovies.length > 0 ? (
            // 검색 결과 리스트 (카드 형태)
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              {filteredMovies.map(movie => (
                <div 
                  key={movie.id} 
                  className="group flex flex-col rounded-xl overflow-hidden bg-white shadow-[0_4px_12px_rgba(0,0,0,0.05)] border border-gray-100 cursor-pointer transition-transform hover:-translate-y-1"
                  onClick={() => navigate('/booking', { state: { movieId: movie.id } })}
                >
                  <div className="relative aspect-[2/3] w-full bg-gray-100">
                    <img
                      src={movie.poster ? movie.poster : '/images/poster_temp.png'}
                      alt={movie.title}
                      className="w-full h-full object-cover"
                      onError={(e) => { e.target.src = '/images/poster_temp.png'; }}
                    />
                  </div>
                  <div className="p-3 flex items-start gap-2 h-[72px]">
                    <img 
                      src={getRatingImage(movie.rating)} 
                      alt="rating" 
                      className="w-5 h-5 object-contain flex-shrink-0 mt-0.5"
                    />
                    <h3 className="font-semibold text-gray-900 text-sm line-clamp-2 leading-tight">
                      {movie.title}
                    </h3>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            // 검색 결과 없음 Empty State
            <div className="flex flex-col items-center justify-center text-center mt-20 px-6">
              <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <span className="text-2xl">🎬</span>
              </div>
              <p className="text-gray-900 font-medium text-lg mb-2">
                검색어에 해당하는 영화가 없습니다.
              </p>
              <p className="text-gray-500 text-sm leading-relaxed max-w-[280px]">
                AI 추천을 통해 원하는 영화를 더욱 폭넓게 검색해보세요!
              </p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default SearchResults;
