import React, { useState, useEffect } from 'react';
import { Header } from './alldaymovie/Header';
import { MovieSwiper } from './alldaymovie/MovieSwiper';
import { BottomTabBar } from './alldaymovie/BottomTabBar';

const MainPage = () => {
  const [movies, setMovies] = useState([]);

  useEffect(() => {
    fetch('/api/movies')
      .then(res => res.json())
      .then(data => {
        setMovies(data);
      })
      .catch(err => console.error("영화 목록을 불러오지 못했습니다.", err));
  }, []);

  return (
    <div className="min-h-screen bg-white text-gray-900 font-sans overflow-x-hidden relative">
      
      {/* Main Content 영역 (Mobile First 600px) */}
      <div className="relative z-10 flex flex-col min-h-screen w-full max-w-[600px] mx-auto bg-gradient-to-b from-white via-gray-50/50 to-gray-100 shadow-2xl border-x border-gray-100">
        
        {/* 분리된 라이트모드 Header */}
        <Header />

        {/* Content Area */}
        <main className="flex-1 pb-24 overflow-x-hidden">
          <MovieSwiper movies={movies} />
        </main>

        {/* 분리된 Bottom Tab Bar */}
        <BottomTabBar />

      </div>
    </div>
  );
};

export default MainPage;
