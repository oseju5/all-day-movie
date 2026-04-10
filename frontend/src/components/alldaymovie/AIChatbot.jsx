import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Send, Bot, User, Film } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function AIChatbot({ isOpen, onClose }) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: 'bot',
      text: '안녕하세요! 영화를 추천해드릴게요. \n어떤 장르나 분위기의 영화를 찾으시나요?'
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const navigate = useNavigate();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputValue.trim() || isLoading) return;

    const userMessage = inputValue.trim();
    setInputValue('');
    setMessages(prev => [...prev, { id: Date.now(), type: 'user', text: userMessage }]);
    setIsLoading(true);

    try {
      // 챗봇 API 호출 예정
      const response = await fetch('/api/chatbot/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: userMessage })
      });
      
      const data = await response.json();
      
      // API 응답 데이터 처리
      setMessages(prev => [...prev, { 
        id: Date.now(), 
        type: 'bot', 
        text: data.message || "추천 결과를 찾았습니다!",
        cards: data.cards // 추천 영화 카드 데이터 목록
      }]);
    } catch (error) {
      console.error('Chatbot API error:', error);
      setMessages(prev => [...prev, { 
        id: Date.now(), 
        type: 'bot', 
        text: "죄송합니다. 오류가 발생했습니다. 다시 시도해주세요." 
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            // inset-0는 브라우저 전체 화면을 덮음, z-index를 바텀탭(50)보다 높게 설정(60)
            className="fixed inset-0 bg-black/60 z-[60] backdrop-blur-sm w-full h-full"
            style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0 }}
          />
          
          {/* Chat Container */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px] h-[80vh] max-h-[800px] bg-gray-50 rounded-t-3xl z-[70] flex flex-col shadow-2xl overflow-hidden"
          >
            {/* Header */}
            <div className="bg-white px-6 py-4 flex items-center justify-between border-b border-gray-100 shadow-sm z-10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-red-500 to-orange-500 flex items-center justify-center shadow-md">
                  <Bot className="text-white w-6 h-6" />
                </div>
                <div>
                  <h2 className="font-bold text-gray-800 text-lg">AI 큐레이터</h2>
                  <p className="text-xs text-gray-500 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-green-500 inline-block animate-pulse"></span>
                    온라인
                  </p>
                </div>
              </div>
              <button 
                onClick={onClose}
                className="p-2 rounded-full hover:bg-gray-100 transition-colors"
              >
                <X className="w-6 h-6 text-gray-500" />
              </button>
            </div>

            {/* Messages Area */}
            <div className="flex-1 overflow-y-auto p-4 space-y-6">
              {messages.map((msg) => (
                <div 
                  key={msg.id} 
                  className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'} items-end gap-2`}
                >
                  {msg.type === 'bot' && (
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-red-500 to-orange-500 flex-shrink-0 flex items-center justify-center shadow-sm mb-1">
                      <Bot className="text-white w-5 h-5" />
                    </div>
                  )}
                  
                  <div className={`max-w-[75%] flex flex-col gap-2`}>
                    <div 
                      className={`p-3.5 rounded-2xl shadow-sm text-sm leading-relaxed whitespace-pre-wrap ${
                        msg.type === 'user' 
                          ? 'bg-gray-800 text-white rounded-tr-sm' 
                          : 'bg-white border border-gray-100 text-gray-800 rounded-tl-sm'
                      }`}
                    >
                      {msg.text}
                    </div>

                    {/* Movie Cards Carousel */}
                    {msg.cards && msg.cards.length > 0 && (
                      <div className="flex gap-3 overflow-x-auto pb-2 pt-1 w-[280px] sm:w-[400px] snap-x scrollbar-hide">
                        {msg.cards.map((card, idx) => (
                          <div key={idx} className="flex-shrink-0 w-[160px] bg-white rounded-xl shadow-md overflow-hidden snap-center border border-gray-100">
                            <div className="h-[220px] bg-gray-200 relative overflow-hidden">
                              {card.imageUrl ? (
                                <img src={card.imageUrl} alt={card.title} className="w-full h-full object-cover" />
                              ) : (
                                <div className="w-full h-full flex flex-col items-center justify-center text-gray-400 bg-gray-100">
                                  <Film className="w-10 h-10 mb-2 opacity-50" />
                                  <span className="text-xs">이미지 없음</span>
                                </div>
                              )}
                              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent flex items-end p-3">
                                <h3 className="text-white font-bold text-sm line-clamp-2 leading-snug">{card.title}</h3>
                              </div>
                            </div>
                            <div className="p-3 bg-white">
                              {card.buttons?.map((btn, bIdx) => (
                                <button
                                  key={bIdx}
                                  onClick={() => {
                                    console.log("Chatbot Button Clicked:", btn);
                                    
                                    // 예매하기 버튼 (내부 라우팅: state로 movieId 전달)
                                    if (btn.name === '예매하기') {
                                      let extractMovieId = null;
                                      
                                      // 1순위: 백엔드에서 준 btn.data.movieId 가 있다면 사용
                                      if (btn.data?.movieId) {
                                        extractMovieId = btn.data.movieId;
                                      } 
                                      // 2순위: 혹시 백엔드 변경 전이라 btn.data.url 에 "?docid=..." 형식으로 들어온다면 거기서 추출
                                      else if (btn.data?.url && btn.data.url.includes('?docid=')) {
                                        extractMovieId = btn.data.url.split('?docid=')[1];
                                      }

                                      if (extractMovieId) {
                                        console.log("Navigating to /booking with movieId:", extractMovieId);
                                        navigate('/booking', { state: { movieId: extractMovieId } });
                                      } else {
                                        console.warn("movieId is missing, navigating to default /booking");
                                        navigate('/booking'); // ID를 못 찾더라도 빈 예매창으로라도 보내기
                                      }
                                      onClose();
                                    } 
                                    // OTT 보러가기 버튼 (외부 링크 새 탭)
                                    else if (btn.data?.url) {
                                      window.open(btn.data.url, '_blank');
                                    }
                                  }}
                                  className={`w-full py-2 rounded-lg text-xs font-bold transition-all shadow-sm bg-red-50 text-red-600 hover:bg-red-100 border border-red-100`} >
                                  {btn.name}
                                </button>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {msg.type === 'user' && (
                    <div className="w-8 h-8 rounded-full bg-gray-200 flex-shrink-0 flex items-center justify-center shadow-sm mb-1 border border-gray-300">
                      <User className="text-gray-500 w-5 h-5" />
                    </div>
                  )}
                </div>
              ))}
              
              {isLoading && (
                <div className="flex justify-start items-end gap-2">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-red-500 to-orange-500 flex-shrink-0 flex items-center justify-center shadow-sm mb-1">
                    <Bot className="text-white w-5 h-5" />
                  </div>
                  <div className="bg-white border border-gray-100 p-4 rounded-2xl rounded-tl-sm shadow-sm flex gap-1.5 items-center h-[46px]">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Area */}
            <div className="bg-white p-4 border-t border-gray-100 pb-safe">
              <form onSubmit={handleSendMessage} className="relative flex items-center">
                <input
                  id="chat-input"
                  name="chatQuery"
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder="예: 데이트 할 때 보기 좋은 영화 알려줘"
                  className="w-full bg-gray-100 border-transparent focus:bg-white focus:border-red-400 focus:ring-2 focus:ring-red-100 rounded-full py-3.5 pl-5 pr-14 outline-none transition-all text-sm"
                  disabled={isLoading}
                />
                <button 
                  type="submit"
                  disabled={!inputValue.trim() || isLoading}
                  className="absolute right-1.5 top-1/2 -translate-y-1/2 w-10 h-10 flex items-center justify-center bg-gray-800 text-white rounded-full hover:bg-gray-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors shadow-md"
                >
                  <Send className="w-4 h-4 ml-0.5" />
                </button>
              </form>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
