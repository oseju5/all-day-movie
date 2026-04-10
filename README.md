# 🎬 All Day Movie
> **AI 자연어 검색 기반 지능형 영화 큐레이션 및 퀵 예매 플랫폼**
> 사용자의 감성적인 질문에 대답하는 AI 챗봇과, 단 2-Depth 만에 결제까지 이어지는 초고속 예매 경험을 제공합니다.

---

## Project Overview
* **개발 기간**: 2026.03.03 ~ 2026.04.10
* **개발자**: ohseju ([GitHub](https://github.com/oseju5/all-day-movie.git))
* **프로젝트 목적**: 파편화된 예매 정보와 OTT 정보를 통합하고, 대화형 인터페이스를 통해 사용자의 검색 피로도를 낮춘 지능형 가이드 제공

## 배포 페이지
* ([All Day Movie](http://3.38.84.196:8080/))

---

## 프로젝트 핵심 가치 (Goals)
* **G1. AI 시네마 큐레이터**: 자연어 의도 분석을 통해 상영작 예매 혹은 외부 OTT 연결로 사용자를 가이드하여 서비스 이탈 방지
* **G2. 2-Depth 퀵 예매**: GNB의 '바로예매' 및 챗봇 추천을 통해 메인 화면에서 결제 진입까지의 단계를 최소화
* **G3. 데이터 생명주기 자동화**: KMDb API와 스케줄러를 활용하여 매일 자정 데이터 동기화 및 30일 경과 데이터 자동 삭제 시스템 구축

---

## Tech Stack

### Backend
| Category | Tech |
| :--- | :--- |
| **Framework** | **Spring Boot 3.4.x**, Spring Security |
| **Database** | **PostgreSQL** (**pgvector**), Spring Data JPA |
| **AI/API** | **Gemini API (RAG)**, KMDb API |
| **Reliability** | **Resilience4j (Circuit Breaker)**, Selenium (Crawling) |

### Frontend
| Category | Tech |
| :--- | :--- |
| **Library** | **React**, React Router |
| **State** | **Zustand** (Persist Middleware) |
| **Style** | Tailwind CSS, Lucide React |

---

## 주요 기능 상세 (Features)

### 1. 지능형 AI 시네마 큐레이터 (F-01)
* **의도 분석 (NLU)**: 구어체 질문에서 영화명, 감독, 장르 등의 핵심 엔티티를 추출합니다.
* **계층적 탐색 (Tiered Search)**: 
    * **Tier 1 (Internal)**: 내부 DB 내 상영 중인 영화를 최우선 탐색하여 즉시 예매로 연결합니다.
    * **Tier 2 (External)**: 상영 종료작인 경우 외부 OTT 플랫폼 정보를 수집하여 바로가기 링크를 제공합니다.

### 2. 실시간 좌석 예매 및 동시성 제어 (F-02)
* **비관적 락(Pessimistic Lock)**: 동일 좌석에 대한 동시 선택 요청 시 DB 수준의 락을 사용하여 중복 예매를 원천 차단합니다.
* **실시간 상태 업데이트**: 3초 간격의 Polling 및 DB 연동을 통해 좌석 잔여 상태를 동적으로 시각화합니다.

### 3. 보안 및 인증 체계 (F-03)
* **JWT Stateless Auth**: Spring Security와 JWT를 연동하여 서버 부하를 최소화한 인증 시스템을 구축했습니다.
* **보안 가드(ProtectedRoute)**: 권한이 없는 사용자의 특정 페이지 접근을 차단하고, 인증 완료 후 기존에 요청했던 페이지로 리다이렉트하는 UX를 제공합니다.


---

## Technical Challenges & Solved

### 1. 외부 서버 장애 대응 (Circuit Breaker)
* **문제**: 네이버 크롤링 서버 지연 시 사용자 챗봇 응답 전체가 먹통이 되는 현상 발생.
* **해결**: **Resilience4j**를 도입하여 장애 발생 시 서킷을 즉시 개방(OPEN)하고, Fallback 응답을 반환하여 시스템 가용성을 확보했습니다.

### 2. 리액트 새로고침 시 인증 상태 유지
* **문제**: 로컬 스토리지에 토큰이 있음에도 새로고침 순간 Zustand 상태가 초기화되어 로그인 페이지로 튕기는 현상.
* **해결**: `_hasHydrated` 상태를 도입하여 스토어 복구가 완료된 시점 이후에만 인증 체크가 작동하도록 **ProtectedRoute** 가드를 고도화했습니다.

### 3. SPA 경로 포워딩 (403/404 Error)
* **문제**: 직접 URL 입력 시 백엔드가 이를 리소스로 오해하여 403 Forbidden 에러가 발생하는 문제.
* **해결**: `WebForwardController`를 구축하여 모든 라우팅 경로를 `index.html`로 포워딩하고, 시큐리티 설정을 최적화했습니다.

---

## 📂 Project Structure
```text
src/main/java/com/yonsai/backend/
├── config/          # Security (JWT), AI (Gemini), Web (CORS) 설정
├── controller/      # API 엔드포인트 및 WebForwardController
├── service/         # AI RAG, 실시간 크롤링, 예매 비즈니스 로직
├── repository/      # JPA 및 비관적 락(Lock) 적용 Repository
├── entity/          # Movie, User, Reservation, Ticket(좌석)
└── dto/             # 클라이언트 통신용 데이터 객체
```
