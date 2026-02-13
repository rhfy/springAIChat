# Personal AI Agent

Spring AI 1.1.2와 Groq API를 사용한 개인 AI 에이전트 애플리케이션입니다.

## 기술 스택

### Backend
- Java 23
- Spring Boot 3.4.2
- Spring AI 1.1.2
- Groq API (OpenAI-compatible)
- Gradle

### Frontend
- React
- TypeScript
- Vite
- Tailwind CSS

## 주요 기능

- Groq API를 통한 AI 채팅 (llama-3.3-70b-versatile 모델)
- 대화 메모리 관리
- 재시도 로직
- 로깅 및 모니터링
- Health check 엔드포인트

## 시작하기

### 사전 요구사항

- Java 23
- Node.js 18+
- Groq API Key ([여기서 발급](https://console.groq.com/keys))

### 환경 변수 설정

1. `.env.example` 파일을 복사하여 `.env` 파일 생성:
```bash
cp .env.example .env
```

2. `.env` 파일에 Groq API 키 입력:
```
GROQ_API_KEY=your_actual_groq_api_key_here
```

### Backend 실행

```bash
cd backend
./gradlew bootRun
```

Backend는 `http://localhost:8080`에서 실행됩니다.

### Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

Frontend는 `http://localhost:3001`에서 실행됩니다.

## API 엔드포인트

### Chat API
```bash
POST /api/chat
Content-Type: application/json

{
  "messages": [
    {
      "role": "user",
      "content": "Hello!"
    }
  ]
}
```

### Health Check
```bash
GET /actuator/health
```

## 프로젝트 구조

```
.
├── backend/                 # Spring Boot 백엔드
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/personalai/backend/
│   │   │   │       ├── advisor/          # Chat advisors
│   │   │   │       ├── config/           # Configuration classes
│   │   │   │       ├── controller/       # REST controllers
│   │   │   │       ├── dto/              # Data transfer objects
│   │   │   │       ├── health/           # Health indicators
│   │   │   │       └── service/          # Business logic
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── build.gradle
├── frontend/                # React 프론트엔드
│   ├── src/
│   └── package.json
└── README.md
```

## 주요 설정

### Groq API 통합

이 프로젝트는 Spring AI의 OpenAI 클라이언트를 사용하여 Groq API와 통합합니다. Groq API는 `extra_body` 파라미터를 지원하지 않기 때문에, 커스텀 HTTP 인터셉터(`GroqRequestInterceptor`)를 사용하여 해당 파라미터를 제거합니다.

### Advisors

- **LoggingAdvisor**: 요청/응답 로깅
- **RetryAdvisor**: 실패 시 재시도
- **MessageChatMemoryAdvisor**: 대화 메모리 관리

## 라이선스

MIT
