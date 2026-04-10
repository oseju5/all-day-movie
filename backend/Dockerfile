# 1. Java 17 실행 환경 사용
FROM eclipse-temurin:17-jdk-alpine

# 2. 크롬 브라우저와 필요한 라이브러리 설치
RUN apt-get update && apt-get install -y \
    chromium \
    chromium-driver \
    fonts-nanum \
    && rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. 빌드된 JAR 파일을 컨테이너 내부로 복사
COPY *.jar app.jar

# 5. 환경변수 설정 (크롬 경로 지정)
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROME_PATH=/usr/lib/chromium/

# 4. 앱 실행 (RDS 연결 정보는 환경변수로 넘길 예정)
ENTRYPOINT ["java", "-jar", "app.jar"]
