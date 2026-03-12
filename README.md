# URL Shortener with Analytics

A production-grade URL shortener built with Spring Boot, MySQL, and Redis.
<img width="1000" height="500" alt="image" src="https://github.com/user-attachments/assets/3e96c044-2841-48d4-886f-c8786b410dfd" />


## Why this matters
This project demonstrates how to build a production‑ready system that combines caching, analytics, and expiration logic - the same patterns used by companies like Bitly and TinyURL.
URL shorteners are used by millions of people every day, but the real value comes from analytics, understanding how users interact with links. This project combines functionality and insight by offering real‑time click tracking, daily/weekly metrics, and expiration controls.


## Features
- 🔗 URL shortening with Base62 short codes
- ⚡ Redis caching for sub-millisecond redirects
- 📊 Click analytics (total, today, this week, recent history)
- ⏰ URL expiration with scheduled cleanup
- 📖 Swagger UI for interactive API docs

## Tech Stack
- **Java 21** + **Spring Boot 3.5**
- **MySQL 8** — persistent URL and analytics storage
- **Redis 7** — cache-aside pattern for fast redirects
- **Docker Compose** — local infrastructure
- **SpringDoc OpenAPI** — Swagger UI

## Quick Start

### Prerequisites
- Java 21+
- Docker + Docker Compose
- Maven 3.9+

### Run locally
```bash
# Start MySQL and Redis
docker compose up -d

# Run the app
mvn spring-boot:run
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/shorten` | Create a short URL |
| GET | `/{shortCode}` | Redirect to original URL |
| GET | `/stats/{shortCode}` | Get click analytics |
| POST | `/admin/cleanup` | Trigger expired URL cleanup |

### Swagger UI
```
http://localhost:8081/swagger-ui/index.html
```

### Example Usage
```bash
# Shorten a URL
curl -X POST http://localhost:8081/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.github.com"}'

# Shorten with expiry (1 hour)
curl -X POST http://localhost:8081/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.github.com", "expiresInMinutes": "60"}'

# Get analytics
curl http://localhost:8081/stats/{shortCode}
```

## Architecture
```
Client
  │
  ▼
Spring Boot (port 8081)
  │
  ├── Redis (port 6379)  ← cache-aside for redirects
  │
  └── MySQL (port 3307)  ← persistent storage
        ├── urls
        └── click_events
```
