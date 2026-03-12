# CRM Platform - Multi-Tenant Microservices Backend

A production-ready, multi-tenant, microservices-based CRM backend built with Java 21 and Spring Boot 3.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
    ┌─────────┬───────────┼───────────┬─────────┬─────────┐
    │         │           │           │         │         │
┌───▼───┐ ┌──▼──┐  ┌─────▼───┐  ┌───▼───┐ ┌──▼──┐  ┌──▼──┐
│ Auth  │ │Lead │  │Account  │  │Contact│ │Opp. │  │Act. │
│Service│ │Svc  │  │Service  │  │Service│ │Svc  │  │Svc  │
│:8081  │ │:8082│  │:8083    │  │:8084  │ │:8085│  │:8086│
└───┬───┘ └──┬──┘  └────┬────┘  └───┬───┘ └──┬──┘  └──┬──┘
    │        │          │           │         │        │
    │   ┌────▼────┐ ┌───▼────┐ ┌───▼───┐     │        │
    │   │Notif.   │ │Workflow│ │  AI   │     │        │
    │   │Service  │ │Service │ │Service│     │        │
    │   │:8087    │ │:8088   │ │:8089  │     │        │
    │   └────┬────┘ └───┬────┘ └───┬───┘     │        │
    │        │          │          │          │        │
┌───▼────────▼──────────▼──────────▼──────────▼────────▼──┐
│                    Apache Kafka                          │
└─────────────────────────┬───────────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────▼────┐     ┌────▼────┐     ┌────▼────┐
    │PostgreSQL│     │  Redis  │     │  LLM    │
    │         │     │ Cache   │     │  API    │
    └─────────┘     └─────────┘     └─────────┘
```

## Tech Stack

| Component          | Technology                        |
|--------------------|-----------------------------------|
| Language           | Java 21                           |
| Framework          | Spring Boot 3.2.3                 |
| Build Tool         | Maven                             |
| Database           | PostgreSQL 16                     |
| ORM                | JPA + Hibernate                   |
| Migration          | Flyway                            |
| Caching            | Redis 7                           |
| Messaging          | Apache Kafka                      |
| Security           | Spring Security + JWT             |
| API Docs           | OpenAPI 3 (Swagger)               |
| AI Integration     | External LLM API + Resilience4j   |
| Containerization   | Docker + Docker Compose           |

## Services

| Service              | Port | Database            | Description                          |
|----------------------|------|---------------------|--------------------------------------|
| Auth Service         | 8081 | crm_auth            | Authentication, JWT, RBAC            |
| Lead Service         | 8082 | crm_leads           | Lead management, scoring, conversion |
| Account Service      | 8083 | crm_accounts        | Account CRUD, hierarchy              |
| Contact Service      | 8084 | crm_contacts        | Contact management, account linking  |
| Opportunity Service  | 8085 | crm_opportunities   | Opportunities, pipeline, forecasting |
| Activity Service     | 8086 | crm_activities      | Tasks, calls, meetings               |
| Notification Service | 8087 | crm_notifications   | Email, SMS, in-app notifications     |
| Workflow Service     | 8088 | crm_workflows       | Trigger-condition-action engine      |
| AI Service           | 8089 | crm_ai              | LLM integration, lead scoring, AI    |

## Multi-Tenancy

The platform uses a **shared database with tenant_id column** strategy:
- Every entity includes a `tenant_id` column
- JWT tokens contain the tenant identifier
- Hibernate filters automatically isolate tenant data
- A `TenantInterceptorFilter` extracts tenant context from headers/JWT
- All queries are automatically scoped to the current tenant

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker & Docker Compose
- Git

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd CRMSAPP
```

### 2. Start Infrastructure with Docker Compose

```bash
# Start all infrastructure (PostgreSQL, Redis, Kafka)
docker-compose up -d postgres redis zookeeper kafka
```

### 3. Build the Project

```bash
mvn clean install -DskipTests
```

### 4. Run Individual Services

```bash
# Terminal 1: Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 2: Lead Service  
cd lead-service && mvn spring-boot:run

# Terminal 3: Account Service
cd account-service && mvn spring-boot:run

# ... repeat for other services
```

### 5. Run All Services with Docker Compose

```bash
# Build all services
mvn clean package -DskipTests

# Start everything
docker-compose up -d --build
```

## API Usage

### Register a User

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@acme.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Admin",
    "tenantId": "acme-corp"
  }'
```

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@acme.com",
    "password": "SecurePass123!",
    "tenantId": "acme-corp"
  }'
```

### Create a Lead

```bash
curl -X POST http://localhost:8082/api/v1/leads \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@prospect.com",
    "company": "Prospect Inc",
    "source": "WEB"
  }'
```

### Get Leads (Paginated)

```bash
curl http://localhost:8082/api/v1/leads?page=0&size=20&sortBy=createdAt&sortDir=desc \
  -H "Authorization: Bearer <your-jwt-token>"
```

### AI Lead Scoring

```bash
curl -X POST http://localhost:8089/api/v1/ai/lead-score \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "leadId": "<lead-uuid>",
    "leadData": {
      "company": "Prospect Inc",
      "title": "VP Engineering",
      "source": "WEB"
    }
  }'
```

## Swagger UI

Each service exposes Swagger UI for API documentation:

- Auth Service: http://localhost:8081/swagger-ui.html
- Lead Service: http://localhost:8082/swagger-ui.html
- Account Service: http://localhost:8083/swagger-ui.html
- Contact Service: http://localhost:8084/swagger-ui.html
- Opportunity Service: http://localhost:8085/swagger-ui.html
- Activity Service: http://localhost:8086/swagger-ui.html
- Notification Service: http://localhost:8087/swagger-ui.html
- Workflow Service: http://localhost:8088/swagger-ui.html
- AI Service: http://localhost:8089/swagger-ui.html

## Health Checks

Each service exposes Spring Boot Actuator health endpoints:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
# ... etc
```

## Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific service
cd lead-service && mvn test

# Run with coverage
mvn test jacoco:report
```

## Project Structure

```
CRMSAPP/
├── pom.xml                          # Parent POM
├── docker-compose.yml               # Full environment orchestration
├── init-databases.sql               # Database initialization
├── crm-common/                      # Shared library
│   └── src/main/java/com/crm/common/
│       ├── audit/                   # Auditing configuration
│       ├── config/                  # Kafka, Redis, OpenAPI config
│       ├── dto/                     # ApiResponse, PagedResponse
│       ├── entity/                  # BaseEntity
│       ├── event/                   # CrmEvent, EventPublisher
│       ├── exception/               # Global exception handler
│       ├── security/                # JWT, filters, SecurityConfig
│       └── tenant/                  # Multi-tenant support
├── auth-service/                    # Authentication & authorization
├── lead-service/                    # Lead management
├── account-service/                 # Account management
├── contact-service/                 # Contact management
├── opportunity-service/             # Opportunity & pipeline
├── activity-service/                # Tasks, calls, meetings
├── notification-service/            # Notifications & email
├── workflow-service/                # Workflow automation engine
├── ai-service/                      # AI/LLM integration
└── docs/                            # API examples
```

## Security

- **JWT Authentication**: All endpoints (except auth) require a valid JWT token
- **RBAC**: Role-based access control with `ADMIN`, `MANAGER`, `USER` roles
- **Method-level security**: `@PreAuthorize` annotations on all endpoints
- **BCrypt**: Password encryption using BCrypt
- **Multi-tenant isolation**: Data automatically isolated per tenant

## Configuration

### Environment Variables

| Variable         | Default                  | Description           |
|------------------|--------------------------|-----------------------|
| DB_HOST          | localhost                | PostgreSQL host       |
| DB_PORT          | 5432                     | PostgreSQL port       |
| DB_USERNAME      | postgres                 | Database username     |
| DB_PASSWORD      | postgres                 | Database password     |
| REDIS_HOST       | localhost                | Redis host            |
| REDIS_PORT       | 6379                     | Redis port            |
| KAFKA_SERVERS    | localhost:9092           | Kafka bootstrap       |
| JWT_SECRET       | (base64 encoded)         | JWT signing secret    |
| LLM_API_URL      | https://api.openai.com/v1| LLM API endpoint      |
| LLM_API_KEY      | your-api-key             | LLM API key           |
| LLM_MODEL        | gpt-4                    | LLM model name        |

## Kafka Topics

| Topic               | Publisher              | Consumer                        |
|----------------------|------------------------|---------------------------------|
| user-events          | Auth Service           | Notification Service            |
| lead-events          | Lead Service           | Notification, Workflow, AI      |
| contact-events       | Contact Service        | Notification, Workflow          |
| account-events       | Account Service        | Notification, Workflow          |
| opportunity-events   | Opportunity Service    | Notification, Workflow          |
| activity-events      | Activity Service       | Notification, Workflow          |
| workflow-actions     | Workflow Service       | Notification Service            |

## License

This project is proprietary. All rights reserved.
