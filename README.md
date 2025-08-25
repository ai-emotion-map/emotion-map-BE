# Clustory - Emotion Map Backend (emotion-map-BE)

**AI 감정 태깅 기반 동네 감성 기록 지도 서비스 백엔드**

---

## 개요
Clustory는 사용자의 글·사진을 기반으로 **AI가 감정을 태깅하고 지도에 시각화하는 감성 지도 플랫폼**입니다.  
단순한 기록을 넘어 **감정을 기준으로 장소와 사람을 연결**하고, 지역 상권 활성화 및 재방문을 유도하는 것을 목표로 합니다.

---

## 문제
- 정릉은 대학과 전통시장이 공존하지만 상권은 침체되고 있음  
- 주민·방문객이 감정을 기록하고 공유할 디지털 창구 부재  
- ‘위로’, ‘향수’ 같은 감정별 탐색이 어려워 정서적 가치가 지역 경제로 연결되지 못함  
- 기존 지도는 **위치·거리 중심**, SNS는 **관계 중심**, 감정 중심 서비스는 부재  

---

## 솔루션 및 핵심 기능
### 핵심 아이디어
> “감정을 중심으로 장소와 사람을 연결하는 감성 지도 플랫폼”

### 주요 기능
- **AI 감정 태깅 & 시각화**  
  - Gemini 2.0 Flash 기반으로 글/사진에서 감정 자동 분석  
  - 감정별 색상·이모지로 지도에 표시  
- **감정 기반 탐색 & 코스 추천**  
  - 예: "위로" → 조용한 카페·산책길  
  - 예: "향수" → 오래된 골목·분식집  
- **실시간 감성 피드**  
  - 최신 감정 기록을 피드로 제공, 공감·소통 기반 커뮤니티 형성   

---

## 기술 스택
- **Backend**: Spring Boot 3 (Java 17), Spring Security + JWT  
- **Database**: PostgreSQL + JPA/Hibernate, Redis (토큰 관리)  
- **Infra**: AWS EC2 (Amazon Linux), Nginx Reverse Proxy, Certbot HTTPS  
- **API**:  
  - Google Gemini 2.0 Flash → 감정 태깅  
  - Kakao Local API → 위·경도 기반 주소 변환  

---

## 실행 방법
### Requirements
- Java 17+
- Gradle  
- PostgreSQL
- 
---

### Build & Run
```bash
git clone https://github.com/ai-emotion-map/emotion-map-BE.git
cd emotion-map-BE
./gradlew build
java -jar build/libs/emotion-map-BE-0.0.1-SNAPSHOT.jar
```

---

### Environment Variables
```ini
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/emomap
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update

SPRING_JWT_SECRET=please-change-me

# 외부 API 키
GEMINI_API_KEY=your-gemini-api-key
KAKAO_REST_API_KEY=your-kakao-rest-api-key

# CORS 허용 오리진
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://emotion-map-nine.vercel.app,https://clustory.shop,https://www.clustory.shop
```

---

### 배포 URL
- Frontend: https://emotion-map-nine.vercel.app/

---

### 기대 효과
- 개인의 감정 기록이 지역 재방문과 상권 매출로 이어지는 선순환 구조를 만듭니다.
