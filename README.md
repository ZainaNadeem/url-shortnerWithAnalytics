# URL Shortener with Analytics

A production-grade URL shortener built with Spring Boot, MySQL, and Redis.

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
