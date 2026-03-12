You are a senior enterprise Java architect.

Generate a production-ready, multi-tenant, microservices-based CRM backend using Java 21 and Spring Boot 3.

Architecture Requirements:
- Microservices architecture
- Spring Boot 3
- Maven project
- PostgreSQL
- JPA + Hibernate
- Flyway migration
- Redis caching
- Kafka event streaming
- Spring Security with JWT
- Role-Based Access Control (RBAC)
- REST APIs
- OpenAPI (Swagger)
- Global exception handling
- Audit logging
- Dockerfile for each service
- Docker Compose for full environment
- Clean architecture (Controller → Service → Repository)
- DTO + Mapper pattern
- Proper validation annotations
- Pagination & sorting support
- Centralized logging
- Unit tests using JUnit + Mockito

Multi-Tenant Requirements:
- Shared database, tenant_id column strategy
- Tenant interceptor filter
- Tenant resolver from JWT
- Automatic tenant data isolation

Core Services to Generate:

1. Auth Service
   - User registration
   - Login (JWT generation)
   - Role management
   - Password encryption (BCrypt)

2. Lead Service
   - Create Lead
   - Update Lead
   - Lead scoring field
   - Assign lead to user
   - Convert lead to opportunity

3. Account Service
   - CRUD Account
   - Account hierarchy (parent-child)

4. Contact Service
   - CRUD Contact
   - Link to account

5. Opportunity Service
   - Create opportunity
   - Update stage
   - Revenue forecast
   - Win/loss tracking

6. Activity Service
   - Tasks
   - Calls
   - Meetings

7. Notification Service
   - Email integration placeholder
   - Kafka event listener

8. Workflow Service
   - Trigger-condition-action engine
   - Rule evaluation

Security:
- JWT filter
- Method-level security annotations
- Role permissions

Entities must include:
- BaseEntity (id, tenantId, createdAt, updatedAt, createdBy)
- Soft delete flag

Database:
- Proper indexing
- Unique constraints
- Foreign key relationships

API Requirements:
- Standard REST conventions
- /api/v1/
- Proper response wrapper
- Error codes
- Validation errors structured

Testing:
- Unit test example for one service
- Integration test example

DevOps:
- Dockerfile
- docker-compose.yml
- application.yml for dev & prod
- Health check endpoint
- Actuator enabled

Generate:
- Full project folder structure
- Sample entity, repository, service, controller for Lead
- JWT security configuration
- Example Flyway migration script
- Example API request/response JSON
- README with setup steps

Ensure:
- Clean, readable, production-quality code
- No pseudo-code
- No placeholders like "implement logic here"
- Proper logging
- Exception handling
Add an AI Service module:
- REST endpoint to call external LLM API
- Lead scoring endpoint
- Next best action suggestion
- Email draft generator
- Async processing using Kafka
- Circuit breaker using Resilience4j