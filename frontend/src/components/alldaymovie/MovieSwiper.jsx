import React from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { EffectCoverflow } from 'swiper/modules';
import { useNavigate } from 'react-router-dom';
import useAuthStore from '../../store/useAuthStore';
import 'swiper/css';
import 'swiper/css/effect-coverflow';

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
 * 메인 페이지용 영화 스와이퍼 컴포넌트.
 * @author ohseju
 */
export function MovieSwiper({ movies = [] }) {
  const navigate = useNavigate();
  const { isLoggedIn, nickname } = useAuthStore();

  if (!movies || movies.length === 0) {
    return <div className="py-20 text-center text-gray-500">영화 정보를 불러오는 중입니다...</div>;
  }

  return (
    <section className="flex-1 py-8 overflow-hidden">
      <div className="px-4 mb-8 text-center mt-2">
        <h2 className="text-xl font-bold text-gray-900">
          {isLoggedIn ? (
            <><span className="text-[#e50914]">{nickname}</span> 님, 환영합니다!</>
          ) : (
            <><span className="text-[#e50914]">로그인</span> 후 무한한 영화를 즐겨보세요!</>
          )}
        </h2>
      </div>

      <div className="relative w-full max-w-[600px] mx-auto mt-10">
        <Swiper
          modules={[EffectCoverflow]}
          effect={'coverflow'}
          grabCursor={true}
          centeredSlides={true}
          slidesPerView={'auto'}
          loopedSlides={5}
          spaceBetween={10}
          loop={true}
          coverflowEffect={{
            rotate: 0,
            stretch: 0,
            depth: 0,
            modifier: 1,
            scale: 0.95,
            slideShadows: false,
          }}
          className="h-[580px] w-full !overflow-visible"
        >
          {movies.map((movie, index) => {
            const ranking = index + 1;
            return (
              <SwiperSlide key={`${movie.id}-${index}`} style={{ width: '280px' }}>
                {({ isActive }) => (
                  <div className={`relative w-full h-full transition-opacity duration-200 ease-out ${isActive ? 'opacity-100' : 'opacity-60'}`}>
                    {/* Poster */}
                    <div
                      className={`relative aspect-[2/3] rounded-2xl overflow-hidden transition-shadow duration-200 ${
                        isActive
                          ? "shadow-2xl shadow-black/20 border border-gray-200"
                          : "shadow-lg shadow-black/10"
                      }`}
                    >
                      <img
                        src={movie.poster ? movie.poster : '/images/poster_temp.png'}
                        alt={movie.title}
                        className="w-full h-full object-cover"
                        draggable={false}
                        onError={(e) => { e.target.src = '/images/poster_temp.png'; }}
                      />
                      
                      {/* Gradient Overlay */}
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />

                      {/* Ranking Number */}
                      <div className="absolute bottom-3 left-4 text-white text-5xl font-bold italic drop-shadow-[0_2px_4px_rgba(0,0,0,0.5)]">
                        {ranking}
                      </div>
                    </div>

                    {/* Info */}
                    <div className="mt-5 text-center transition-opacity duration-300 w-full">
                      <div className={`w-full font-semibold text-gray-900 flex items-center justify-center gap-2 ${isActive ? "text-xl" : "text-base opacity-60"}`}>
                        <h3 className="line-clamp-2 break-keep text-center">
                          {movie.title}
                        </h3>
                        <img 
                          src={getRatingImage(movie.rating)} 
                          alt="rating" 
                          className="w-5 h-5 object-contain flex-shrink-0"
                        />
                      </div>
                      
                      <button
                        onClick={() => navigate('/booking', { state: { movieId: movie.id } })}
                        className={`mt-4 px-8 py-2 text-sm font-medium text-[#e50914] border border-[#e50914] rounded-full bg-transparent hover:bg-[#e50914] hover:text-white transition-colors pointer-events-auto w-full max-w-[200px] cursor-pointer ${!isActive && "opacity-50 hover:opacity-100"}`}
                      >
                        예매하기
                      </button>
                    </div>
                  </div>
                )}
              </SwiperSlide>
            );
          })}
        </Swiper>
      </div>
    </section>
  );
}
