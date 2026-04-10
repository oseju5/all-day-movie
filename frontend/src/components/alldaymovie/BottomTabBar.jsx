import React from 'react';
import { motion } from 'framer-motion';
import { Film, User } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../../store/useAuthStore';
import { AIChatbot } from './AIChatbot';

const tabs = [
  { id: "movie", label: "영화", icon: Film },
  { id: "booking", label: "바로 예매" },
  { id: "my", label: "My", icon: User },
];

export function BottomTabBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn } = useAuthStore();
  const [isChatbotOpen, setIsChatbotOpen] = React.useState(false);

  // 현재 경로에 따라 active 상태 결정
  const currentPath = location.pathname;
  let activeTab = "movie";
  if (currentPath.includes("booking")) activeTab = "booking";
  else if (currentPath.includes("my") || currentPath.includes("login")) activeTab = "my";

  return (
    <motion.nav
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.2 }}
      className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px] z-50 border-t border-gray-100 shadow-[0_-5px_20px_rgba(0,0,0,0.05)]"
      style={{
        backdropFilter: 'blur(12px)',
        backgroundColor: 'rgba(255, 255, 255, 0.85)'
      }}
    >
      <div className="relative flex items-end justify-around px-4 pb-2 pt-4">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          const isBooking = tab.id === "booking";

          if (isBooking) {
            return (
              <React.Fragment key={tab.id}>
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setIsChatbotOpen(true)}
                  className="absolute left-1/2 -translate-x-1/2 -top-6 flex items-center justify-center w-[72px] h-[72px] rounded-full text-white font-bold text-sm tracking-wide shadow-xl shadow-red-500/30 transition-shadow duration-300 cursor-pointer z-50"
                  style={{
                    background: "linear-gradient(180deg, #ffc107 0%, #ff6b35 50%, #e50914 100%)"
                  }}
                >
                  AI 추천
                </motion.button>
              </React.Fragment>
            );
          }

          return (
            <motion.button
              key={tab.id}
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                if (tab.id === 'movie') navigate('/');
                else if (tab.id === 'my') {
                  if (isLoggedIn) navigate('/mypage');
                  else navigate('/login');
                }
              }}
              className={`flex flex-col items-center justify-center w-24 py-1 transition-colors cursor-pointer ${
                isActive ? "text-[#e50914]" : "text-gray-400 hover:text-gray-600"
              }`}
            >
              {Icon && <Icon className="w-6 h-6" />}
              <span className="text-xs font-medium mt-1.5 tracking-wider">{tab.label}</span>
            </motion.button>
          );
        })}
      </div>
      
      {/* 바텀탭 컨테이너 바깥쪽에서 렌더링되도록 위치 이동 */}
      <AIChatbot isOpen={isChatbotOpen} onClose={() => setIsChatbotOpen(false)} />
    </motion.nav>
  );
}
