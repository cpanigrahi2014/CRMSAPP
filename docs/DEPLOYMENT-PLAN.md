# CRMS Platform — Deployment Plan

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Deployment Environments](#2-deployment-environments)
3. [Infrastructure Requirements](#3-infrastructure-requirements)
4. [Container Registry & CI/CD Pipeline](#4-container-registry--cicd-pipeline)
5. [Kubernetes Deployment (Production)](#5-kubernetes-deployment-production)
6. [Nginx Reverse Proxy & API Gateway](#6-nginx-reverse-proxy--api-gateway)
7. [Database Strategy](#7-database-strategy)
8. [Secrets Management](#8-secrets-management)
9. [Frontend Deployment](#9-frontend-deployment)
10. [Monitoring & Observability](#10-monitoring--observability)
11. [Scaling Strategy](#11-scaling-strategy)
12. [Disaster Recovery & Backup](#12-disaster-recovery--backup)
13. [Deployment Runbook](#13-deployment-runbook)
14. [Cost Estimation](#14-cost-estimation)

---

## 1. Architecture Overview

### Service Inventory

| # | Service | Port | Language | Database | Purpose |
|---|---------|------|----------|----------|---------|
| 1 | **auth-service** | 8081 | Java 21 / Spring Boot 3.2 | `crm_auth` | Authentication, JWT, RBAC, SSO |
| 2 | **lead-service** | 8082 | Java 21 / Spring Boot 3.2 | `crm_leads` | Lead management, scoring, assignment |
| 3 | **account-service** | 8083 | Java 21 / Spring Boot 3.2 | `crm_accounts` | Company accounts, hierarchy, health scores |
| 4 | **contact-service** | 8084 | Java 21 / Spring Boot 3.2 | `crm_contacts` | Contact management, consent, segmentation |
| 5 | **opportunity-service** | 8085 | Java 21 / Spring Boot 3.2 | `crm_opportunities` | Pipeline, deals, collaboration, forecasting |
| 6 | **activity-service** | 8086 | Java 21 / Spring Boot 3.2 | `crm_activities` | Tasks, calls, meetings, timeline |
| 7 | **notification-service** | 8087 | Java 21 / Spring Boot 3.2 | `crm_notifications` | Notifications, SMS, WhatsApp, calls |
| 8 | **workflow-service** | 8088 | Java 21 / Spring Boot 3.2 | `crm_workflows` | Automation rules, triggers |
| 9 | **ai-service** | 8089 | Java 21 / Spring Boot 3.2 | `crm_ai` | AI features (scoring, predictions, NLP) |
| 10 | **email-service** | 8090 | Java 21 / Spring Boot 3.2 | `crm_email` | Email accounts, templates, tracking |
| 11 | **integration-service** | 8091 | Java 21 / Spring Boot 3.2 | `crm_integrations` | REST APIs, webhooks, marketplace |
| 12 | **crm-ai-agent** | 9100 | Node.js / Express | `crm_ai_agent` | AI conversational agent |
| 13 | **crm-frontend** | 3000 | React 18 / TypeScript / Vite | — | SPA web application |

### Infrastructure Dependencies

| Component | Image | Purpose |
|-----------|-------|---------|
| **PostgreSQL** | `postgres:16-alpine` | Primary database (12 databases) |
| **Redis** | `redis:7-alpine` | Caching, session storage |
| **Apache Kafka** | `confluentinc/cp-kafka:7.6.0` | Event streaming, inter-service messaging |
| **Zookeeper** | `confluentinc/cp-zookeeper:7.6.0` | Kafka coordination |

### Communication Patterns

```
[Frontend]  ──HTTPS──▶  [Nginx/API Gateway]
                              │
                ┌─────────────┼─────────────┐
                ▼             ▼             ▼
         [auth-service]  [lead-service]  [11 services...]
                │             │             │
                ▼             ▼             ▼
         [PostgreSQL]    [Redis]       [Kafka]
```

- **Synchronous**: REST API calls via JWT bearer tokens
- **Asynchronous**: Kafka event bus for inter-service events
- **Caching**: Redis for session tokens and query caching
- **Real-time**: Server-Sent Events (SSE) for activity streams

---

## 2. Deployment Environments

### Environment Matrix

| Environment | Purpose | Infrastructure | URL Pattern |
|-------------|---------|----------------|-------------|
| **Development** | Local development | Docker Compose | `localhost:3000` |
| **Staging** | Pre-production testing | Kubernetes (small) | `staging.crm.yourdomain.com` |
| **Production** | Live deployment | Kubernetes (HA) | `crm.yourdomain.com` |

### Environment Configuration

| Variable | Development | Staging | Production |
|----------|------------|---------|------------|
| `SPRING_PROFILES_ACTIVE` | `default` | `staging` | `prod` |
| Database Host | `localhost:5434` | RDS/CloudSQL endpoint | RDS/CloudSQL (Multi-AZ) |
| Redis Host | `localhost:6380` | ElastiCache endpoint | ElastiCache (cluster mode) |
| Kafka Brokers | `localhost:9092` | MSK/Confluent endpoint | MSK/Confluent (3 brokers) |
| JWT Secret | Base64 dev key | Per-env secret | Vault/Secrets Manager |
| LLM API Key | Test key | Test key | Production OpenAI key |
| SMTP | Disabled | Mailtrap/sandbox | SendGrid/SES/Gmail |
| Frontend URL | `http://localhost:3000` | `https://staging.crm.yourdomain.com` | `https://crm.yourdomain.com` |
| TLS | None | Let's Encrypt | ACM/Cloudflare |

---

## 3. Infrastructure Requirements

### Minimum Production Sizing

#### Compute (Kubernetes Worker Nodes)

| Component | Instances | vCPU | RAM | Storage |
|-----------|-----------|------|-----|---------|
| **App Services** (11 Java) | 2 nodes | 4 vCPU each | 16 GB each | 50 GB SSD |
| **Node.js Agent** | Shared with above | 0.5 vCPU | 512 MB | 10 GB |
| **Nginx Ingress** | 2 (HA) | 0.5 vCPU | 256 MB | — |

#### Per-Service Resource Requests (Kubernetes)

| Service | CPU Request | CPU Limit | Mem Request | Mem Limit | Replicas |
|---------|-------------|-----------|-------------|-----------|----------|
| auth-service | 250m | 1000m | 512Mi | 1Gi | 2 |
| lead-service | 250m | 500m | 512Mi | 1Gi | 2 |
| account-service | 250m | 500m | 512Mi | 1Gi | 2 |
| contact-service | 250m | 500m | 512Mi | 1Gi | 2 |
| opportunity-service | 250m | 500m | 512Mi | 1Gi | 2 |
| activity-service | 250m | 500m | 512Mi | 1Gi | 2 |
| notification-service | 200m | 500m | 384Mi | 768Mi | 1 |
| workflow-service | 200m | 500m | 384Mi | 768Mi | 1 |
| ai-service | 250m | 1000m | 512Mi | 1Gi | 1 |
| email-service | 200m | 500m | 384Mi | 768Mi | 1 |
| integration-service | 200m | 500m | 384Mi | 768Mi | 1 |
| crm-ai-agent | 200m | 500m | 256Mi | 512Mi | 1 |
| **Total (min replicas)** | **3.0 cores** | — | **5.6 GB** | — | **17 pods** |

#### Managed Services

| Service | Provider Option | Sizing | Est. Monthly Cost |
|---------|----------------|--------|-------------------|
| **PostgreSQL** | AWS RDS / GCP CloudSQL / Azure Database | db.r6g.large (2 vCPU, 16 GB) | $150–300 |
| **Redis** | AWS ElastiCache / GCP Memorystore | cache.r6g.large (2 vCPU, 13 GB) | $130–200 |
| **Kafka** | AWS MSK / Confluent Cloud | 3 brokers, kafka.m5.large | $300–500 |
| **Container Registry** | ECR / GCR / ACR | 50 GB images | $5–10 |
| **Load Balancer** | ALB / GCP LB / Azure LB | 1 instance | $20–30 |
| **DNS + CDN** | CloudFront / Cloudflare | Frontend assets | $10–50 |

---

## 4. Container Registry & CI/CD Pipeline

### GitHub Actions CI/CD Pipeline

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│   Push   │──▶│  Build   │──▶│  Test    │──▶│  Deploy  │
│  to Git  │    │  & Scan  │    │  Suite   │    │  to K8s  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

#### Recommended Pipeline: `.github/workflows/deploy.yml`

```yaml
name: Build & Deploy

on:
  push:
    branches: [main, staging]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: ${{ github.repository_owner }}/crm

jobs:
  # ── Detect changed services ──────────────────────────────
  changes:
    runs-on: ubuntu-latest
    outputs:
      services: ${{ steps.filter.outputs.changes }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            auth-service: 'auth-service/**'
            lead-service: 'lead-service/**'
            account-service: 'account-service/**'
            contact-service: 'contact-service/**'
            opportunity-service: 'opportunity-service/**'
            activity-service: 'activity-service/**'
            notification-service: 'notification-service/**'
            workflow-service: 'workflow-service/**'
            ai-service: 'ai-service/**'
            email-service: 'email-service/**'
            integration-service: 'integration-service/**'
            crm-common: 'crm-common/**'
            crm-frontend: 'crm-frontend/**'
            crm-ai-agent: 'crm-ai-agent/**'

  # ── Build Java services ──────────────────────────────────
  build-java:
    needs: changes
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - auth-service
          - lead-service
          - account-service
          - contact-service
          - opportunity-service
          - activity-service
          - notification-service
          - workflow-service
          - ai-service
          - email-service
          - integration-service
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: mvn package -pl ${{ matrix.service }} -am -DskipTests -q

      - name: Run tests
        run: mvn test -pl ${{ matrix.service }} -am

      - name: Build Docker image
        run: |
          docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-${{ matrix.service }}:${{ github.sha }} \
                        -t ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-${{ matrix.service }}:latest \
                        ./${{ matrix.service }}

      - name: Push to registry
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-${{ matrix.service }} --all-tags

  # ── Build frontend ───────────────────────────────────────
  build-frontend:
    needs: changes
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: crm-frontend/package-lock.json
      - run: cd crm-frontend && npm ci && npm run build
      - name: Build & push Nginx image
        run: |
          docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-frontend:${{ github.sha }} ./crm-frontend
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-frontend:${{ github.sha }}

  # ── Deploy to Kubernetes ─────────────────────────────────
  deploy:
    needs: [build-java, build-frontend]
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/staging'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set environment
        run: |
          if [ "${{ github.ref }}" = "refs/heads/main" ]; then
            echo "ENVIRONMENT=production" >> $GITHUB_ENV
            echo "NAMESPACE=crm-prod" >> $GITHUB_ENV
          else
            echo "ENVIRONMENT=staging" >> $GITHUB_ENV
            echo "NAMESPACE=crm-staging" >> $GITHUB_ENV
          fi

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/auth-service \
            auth-service=${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}-auth-service:${{ github.sha }} \
            -n ${{ env.NAMESPACE }}
          # Repeat for each service...
          kubectl rollout status deployment --timeout=300s -n ${{ env.NAMESPACE }}
```

### Docker Image Tagging Strategy

| Tag | Purpose | Example |
|-----|---------|---------|
| `latest` | Most recent build | `crm-auth-service:latest` |
| `<git-sha>` | Immutable build reference | `crm-auth-service:a1b2c3d` |
| `v<semver>` | Release versions | `crm-auth-service:v1.0.0` |
| `staging` | Staging environment | `crm-auth-service:staging` |

---

## 5. Kubernetes Deployment (Production)

### Namespace Layout

```
crm-prod/
├── deployments/        (11 Java services + frontend + AI agent)
├── services/           (ClusterIP for each)
├── ingress/            (single Ingress with path routing)
├── configmaps/         (shared config)
├── secrets/            (DB creds, JWT, API keys)
├── hpa/                (HorizontalPodAutoscaler)
└── pdb/                (PodDisruptionBudget)
```

### Sample Deployment: `auth-service`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: crm-prod
  labels:
    app: auth-service
    tier: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      serviceAccountName: crm-service-account
      containers:
        - name: auth-service
          image: ghcr.io/yourorg/crm-auth-service:v1.0.0
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SERVER_PORT
              value: "8081"
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: crm-db-config
                  key: host
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: crm-db-credentials
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: crm-db-credentials
                  key: password
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:postgresql://$(DB_HOST):5432/crm_auth"
            - name: REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: crm-redis-config
                  key: host
            - name: REDIS_PORT
              value: "6379"
            - name: KAFKA_SERVERS
              valueFrom:
                configMapKeyRef:
                  name: crm-kafka-config
                  key: bootstrap-servers
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: crm-jwt-secret
                  key: secret
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 1000m
              memory: 1Gi
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 30
          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            failureThreshold: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: crm-prod
spec:
  selector:
    app: auth-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service
  namespace: crm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: auth-service
  namespace: crm-prod
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: auth-service
```

### Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: crm-db-credentials
  namespace: crm-prod
type: Opaque
stringData:
  username: crm_admin          # NOT 'postgres' in prod
  password: <generated-64-char-password>

---
apiVersion: v1
kind: Secret
metadata:
  name: crm-jwt-secret
  namespace: crm-prod
type: Opaque
stringData:
  secret: <generate-new-256-bit-base64-key>

---
apiVersion: v1
kind: Secret
metadata:
  name: crm-openai-key
  namespace: crm-prod
type: Opaque
stringData:
  api-key: <your-openai-production-key>

---
apiVersion: v1
kind: Secret
metadata:
  name: crm-smtp-credentials
  namespace: crm-prod
type: Opaque
stringData:
  host: smtp.sendgrid.net
  port: "587"
  username: apikey
  password: <sendgrid-api-key>
```

### ConfigMaps

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-db-config
  namespace: crm-prod
data:
  host: "crm-postgres-rds.xxxxxxx.us-east-1.rds.amazonaws.com"
  port: "5432"

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-redis-config
  namespace: crm-prod
data:
  host: "crm-redis.xxxxxxx.cache.amazonaws.com"
  port: "6379"

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-kafka-config
  namespace: crm-prod
data:
  bootstrap-servers: "b-1.crm-kafka.xxxxxxx.kafka.us-east-1.amazonaws.com:9092,b-2.crm-kafka.xxxxxxx.kafka.us-east-1.amazonaws.com:9092"
```

---

## 6. Nginx Reverse Proxy & API Gateway

### Production Nginx Configuration

The Vite dev proxy only works in development. Production needs Nginx to route API paths to the correct backend service.

```nginx
# /etc/nginx/conf.d/crm.conf

upstream auth_backend        { server auth-service:8081;         }
upstream lead_backend        { server lead-service:8082;         }
upstream account_backend     { server account-service:8083;      }
upstream contact_backend     { server contact-service:8084;      }
upstream opportunity_backend { server opportunity-service:8085;  }
upstream activity_backend    { server activity-service:8086;     }
upstream notification_backend{ server notification-service:8087; }
upstream workflow_backend    { server workflow-service:8088;     }
upstream ai_backend          { server ai-service:8089;           }
upstream email_backend       { server email-service:8090;        }
upstream integration_backend { server integration-service:8091;  }

server {
    listen 80;
    server_name crm.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name crm.yourdomain.com;

    ssl_certificate     /etc/ssl/certs/crm.crt;
    ssl_certificate_key /etc/ssl/private/crm.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # ── Security headers ──────────────────────────────────
    add_header X-Frame-Options SAMEORIGIN always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';" always;

    # ── Rate limiting ─────────────────────────────────────
    limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;
    limit_req zone=api burst=50 nodelay;

    # ── API routing ───────────────────────────────────────
    location /api/v1/auth       { proxy_pass http://auth_backend;         }
    location /api/v1/leads      { proxy_pass http://lead_backend;         }
    location /api/v1/accounts   { proxy_pass http://account_backend;      }
    location /api/v1/contacts   { proxy_pass http://contact_backend;      }
    location /api/v1/opportunities { proxy_pass http://opportunity_backend; }
    location /api/v1/collaboration { proxy_pass http://opportunity_backend; }
    location /api/v1/activities { proxy_pass http://activity_backend;     }
    location /api/v1/notifications { proxy_pass http://notification_backend; }
    location /api/v1/communications { proxy_pass http://notification_backend; }
    location /api/v1/workflows  { proxy_pass http://workflow_backend;     }
    location /api/v1/automation { proxy_pass http://workflow_backend;     }
    location /api/v1/ai         { proxy_pass http://ai_backend;           }
    location /api/ai            { proxy_pass http://ai_backend;           }
    location /api/v1/email      { proxy_pass http://email_backend;        }
    location /api/v1/integrations { proxy_pass http://integration_backend; }
    location /api/v1/developer  { proxy_pass http://integration_backend;  }

    # ── SSE support (activity streams) ────────────────────
    location /api/v1/activities/stream {
        proxy_pass http://activity_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }

    # ── Common proxy settings ─────────────────────────────
    location /api/ {
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        client_max_body_size 50M;
    }

    # ── Frontend (React SPA) ─────────────────────────────
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;

        # Cache static assets aggressively
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

### Kubernetes Ingress (alternative to standalone Nginx)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: crm-ingress
  namespace: crm-prod
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - crm.yourdomain.com
      secretName: crm-tls-cert
  rules:
    - host: crm.yourdomain.com
      http:
        paths:
          - path: /api/v1/auth
            pathType: Prefix
            backend: { service: { name: auth-service, port: { number: 8081 } } }
          - path: /api/v1/leads
            pathType: Prefix
            backend: { service: { name: lead-service, port: { number: 8082 } } }
          - path: /api/v1/accounts
            pathType: Prefix
            backend: { service: { name: account-service, port: { number: 8083 } } }
          - path: /api/v1/contacts
            pathType: Prefix
            backend: { service: { name: contact-service, port: { number: 8084 } } }
          - path: /api/v1/opportunities
            pathType: Prefix
            backend: { service: { name: opportunity-service, port: { number: 8085 } } }
          - path: /api/v1/collaboration
            pathType: Prefix
            backend: { service: { name: opportunity-service, port: { number: 8085 } } }
          - path: /api/v1/activities
            pathType: Prefix
            backend: { service: { name: activity-service, port: { number: 8086 } } }
          - path: /api/v1/notifications
            pathType: Prefix
            backend: { service: { name: notification-service, port: { number: 8087 } } }
          - path: /api/v1/communications
            pathType: Prefix
            backend: { service: { name: notification-service, port: { number: 8087 } } }
          - path: /api/v1/workflows
            pathType: Prefix
            backend: { service: { name: workflow-service, port: { number: 8088 } } }
          - path: /api/v1/automation
            pathType: Prefix
            backend: { service: { name: workflow-service, port: { number: 8088 } } }
          - path: /api/v1/ai
            pathType: Prefix
            backend: { service: { name: ai-service, port: { number: 8089 } } }
          - path: /api/ai
            pathType: Prefix
            backend: { service: { name: ai-service, port: { number: 8089 } } }
          - path: /api/v1/email
            pathType: Prefix
            backend: { service: { name: email-service, port: { number: 8090 } } }
          - path: /api/v1/integrations
            pathType: Prefix
            backend: { service: { name: integration-service, port: { number: 8091 } } }
          - path: /api/v1/developer
            pathType: Prefix
            backend: { service: { name: integration-service, port: { number: 8091 } } }
          - path: /
            pathType: Prefix
            backend: { service: { name: crm-frontend, port: { number: 80 } } }
```

---

## 7. Database Strategy

### Production Database Architecture

```
┌─────────────────────────────────────────────────────┐
│              PostgreSQL (Managed - RDS/CloudSQL)     │
│                                                     │
│  Primary (Multi-AZ)  ──▶  Read Replica (optional)   │
│                                                     │
│  Databases:                                         │
│  ├── crm_auth          (auth-service)               │
│  ├── crm_leads         (lead-service)               │
│  ├── crm_accounts      (account-service)            │
│  ├── crm_contacts      (contact-service)            │
│  ├── crm_opportunities (opportunity-service)        │
│  ├── crm_activities    (activity-service)           │
│  ├── crm_notifications (notification-service)       │
│  ├── crm_workflows     (workflow-service)           │
│  ├── crm_ai            (ai-service)                 │
│  ├── crm_email         (email-service)              │
│  ├── crm_integrations  (integration-service)        │
│  └── crm_ai_agent      (crm-ai-agent)              │
└─────────────────────────────────────────────────────┘
```

### Migration Strategy

All services use **Flyway** migrations (`classpath:db/migration`):

```bash
# Automatic on startup (default Flyway behavior)
# Each service runs its own migrations on boot
# JPA validation mode: ddl-auto=validate (no auto DDL changes)
```

**Production migration rules:**
1. Migrations run automatically on service startup (Flyway)
2. Always backward-compatible (add columns, never rename/drop)
3. Review migration SQL in code review before merge
4. Test migrations against staging database first
5. Keep `flyway_schema_history` table for version tracking

### Database Users (per-service isolation)

```sql
-- Create per-service database users (not shared 'postgres')
CREATE USER crm_auth_user WITH PASSWORD '<generated>';
GRANT ALL PRIVILEGES ON DATABASE crm_auth TO crm_auth_user;

CREATE USER crm_leads_user WITH PASSWORD '<generated>';
GRANT ALL PRIVILEGES ON DATABASE crm_leads TO crm_leads_user;

-- Repeat for each service...
```

### Backup Schedule

| Type | Frequency | Retention | Tool |
|------|-----------|-----------|------|
| Automated snapshots | Daily | 30 days | RDS automated backups |
| Point-in-time recovery | Continuous | 7 days | RDS PITR |
| Manual snapshot | Before major releases | 90 days | Manual trigger |
| Cross-region backup | Weekly | 60 days | RDS cross-region copy |

---

## 8. Secrets Management

### Current State (Insecure — Dev Only)

The current `docker-compose.yml` has hardcoded secrets:
- `POSTGRES_PASSWORD: postgres`
- `JWT_SECRET: dGhpcyBpcyBh...` (base64 static key)
- `MAIL_PASSWORD: changeme`

### Production Requirements

| Secret | Current (Dev) | Production Approach |
|--------|--------------|---------------------|
| DB Password | `postgres` | AWS Secrets Manager / Vault |
| JWT Secret | Hardcoded base64 | 256-bit key in Secrets Manager, rotated quarterly |
| OpenAI API Key | `your-api-key` | Secrets Manager |
| SMTP Password | `changeme` | Secrets Manager |
| Gmail/Outlook OAuth | Not configured | Secrets Manager |

### Recommended: AWS Secrets Manager + External Secrets Operator

```yaml
# External Secrets Operator (Kubernetes)
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: crm-db-credentials
  namespace: crm-prod
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: crm-db-credentials
  data:
    - secretKey: username
      remoteRef:
        key: crm/prod/database
        property: username
    - secretKey: password
      remoteRef:
        key: crm/prod/database
        property: password
```

---

## 9. Frontend Deployment

### Build Process

```bash
cd crm-frontend
npm ci                    # Install exact versions
npm run build             # tsc -b && vite build → produces dist/
```

### Frontend Dockerfile

```dockerfile
# crm-frontend/Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Frontend Nginx Config (`crm-frontend/nginx.conf`)

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Health check
    location /health {
        return 200 'ok';
        add_header Content-Type text/plain;
    }
}
```

### Environment Variables at Build Time

The React frontend needs API base URLs configured. For production, the frontend sends API requests to the **same origin** (the Nginx reverse proxy handles routing). No special env vars needed — Axios already uses relative paths (`/api/v1/...`).

---

## 10. Monitoring & Observability

### Health Checks (Built-in)

All Java services expose Spring Boot Actuator:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall health |
| `/actuator/health/readiness` | Ready to serve traffic |
| `/actuator/health/liveness` | Process alive |
| `/actuator/metrics` | Prometheus-compatible metrics |
| `/actuator/info` | Build info |

### Recommended Monitoring Stack

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Prometheus   │──▶│   Grafana     │    │   AlertManager│
│  (scrape)     │    │  (dashboards) │    │  (alerts)     │
└──────────────┘    └──────────────┘    └──────────────┘
       │
       ▼
  /actuator/prometheus  (each service)
```

### Key Metrics to Monitor

| Category | Metric | Alert Threshold |
|----------|--------|----------------|
| **API Latency** | `http_server_requests_seconds` | p99 > 2s |
| **Error Rate** | `http_server_requests{status=~"5.."}` | > 1% of total |
| **JVM Memory** | `jvm_memory_used_bytes` | > 85% of limit |
| **DB Connections** | `hikaricp_connections_active` | > 80% of pool |
| **Kafka Consumer Lag** | `kafka_consumer_lag` | > 1000 messages |
| **Redis Latency** | `redis_command_duration_seconds` | p99 > 100ms |
| **Pod Restarts** | `kube_pod_container_status_restarts_total` | > 3 in 1h |
| **Node CPU** | `node_cpu_utilization` | > 80% for 5min |
| **Disk Usage** | `node_filesystem_avail_bytes` | < 20% free |

### Logging (ELK / CloudWatch)

```
┌────────────┐    ┌────────────┐    ┌────────────┐
│  FluentBit  │──▶│ Elasticsearch│──▶│   Kibana    │
│  (DaemonSet)│    │  (indexing)  │    │ (search UI) │
└────────────┘    └────────────┘    └────────────┘
```

- All services log to stdout (Docker captures)
- Structured JSON logging recommended
- Correlation IDs for cross-service request tracing
- Log retention: 30 days hot, 90 days warm, 1 year archived

### Distributed Tracing (Optional)

Add **OpenTelemetry** Java agent to each service for end-to-end tracing:

```yaml
# In Kubernetes deployment
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-javaagent:/otel/opentelemetry-javaagent.jar"
  - name: OTEL_SERVICE_NAME
    value: "auth-service"
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://otel-collector:4317"
```

---

## 11. Scaling Strategy

### Horizontal Pod Autoscaling

| Service | Min Replicas | Max Replicas | Scale Trigger |
|---------|-------------|-------------|---------------|
| auth-service | 2 | 5 | CPU > 70% |
| lead-service | 2 | 4 | CPU > 70% |
| account-service | 2 | 4 | CPU > 70% |
| contact-service | 2 | 4 | CPU > 70% |
| opportunity-service | 2 | 4 | CPU > 70% |
| activity-service | 2 | 4 | CPU > 70% |
| notification-service | 1 | 3 | CPU > 70% |
| workflow-service | 1 | 3 | CPU > 70% |
| ai-service | 1 | 3 | CPU > 70% |
| email-service | 1 | 3 | CPU > 70% |
| integration-service | 1 | 3 | CPU > 70% |

### Database Scaling Path

1. **Vertical first**: Scale RDS instance up to r6g.xlarge → 2xlarge
2. **Read replicas**: Add 1-2 read replicas for analytics/reporting queries
3. **Connection pooling**: Add PgBouncer sidecar if connection count is high
4. **Database per service**: Already isolated — can migrate individual databases to separate instances if needed

### Kafka Scaling

```
Phase 1: Single broker (dev/staging)
Phase 2: 3 brokers (production baseline)
Phase 3: 6+ brokers (high throughput)
```

---

## 12. Disaster Recovery & Backup

### Recovery Point/Time Objectives

| Component | RPO | RTO | Method |
|-----------|-----|-----|--------|
| PostgreSQL | 5 min | 30 min | RDS PITR + automated backups |
| Redis | 1 hour | 15 min | Redis AOF + snapshots |
| Kafka | 0 (replicated) | 10 min | Multi-AZ broker replication |
| Application Code | 0 | 15 min | Container re-deploy from registry |
| Frontend Assets | 0 | 5 min | CDN re-deploy |

### DR Runbook

```
1. Detect failure (monitoring alerts)
2. Assess scope (single service vs infrastructure)
3. If single service: kubectl rollout restart deployment/<service>
4. If database: RDS point-in-time restore to new instance
5. If full region: restore from cross-region backup
6. Verify data integrity via health checks
7. Run smoke tests against restored environment
8. Update DNS if region failover
```

---

## 13. Deployment Runbook

### Pre-Deployment Checklist

- [ ] All tests pass in CI pipeline
- [ ] Database migration reviewed and tested on staging
- [ ] Secrets updated in Secrets Manager (if changed)
- [ ] Docker images built and pushed to registry
- [ ] Staging deployment verified
- [ ] Rollback plan documented
- [ ] Team notified in deployment channel

### Deployment Order (Dependencies)

```
Phase 1 — Infrastructure (if changed)
  └── PostgreSQL → Redis → Kafka

Phase 2 — Core Services (parallel)
  ├── auth-service       (required by all others)
  └── Wait for auth-service healthy

Phase 3 — Business Services (parallel)
  ├── lead-service
  ├── account-service
  ├── contact-service
  ├── opportunity-service
  ├── activity-service
  ├── notification-service
  ├── workflow-service
  ├── ai-service
  ├── email-service
  └── integration-service

Phase 4 — Frontend
  └── crm-frontend (Nginx + static assets)

Phase 5 — AI Agent (optional)
  └── crm-ai-agent
```

### Rollback Procedure

```bash
# Rollback single service to previous version
kubectl rollout undo deployment/auth-service -n crm-prod

# Rollback to specific revision
kubectl rollout undo deployment/auth-service --to-revision=3 -n crm-prod

# Verify rollback
kubectl rollout status deployment/auth-service -n crm-prod
```

### Post-Deployment Verification

```bash
# 1. Health checks
for svc in auth lead account contact opportunity activity notification workflow ai email integration; do
  curl -s https://crm.yourdomain.com/api/v1/$svc/actuator/health | jq .status
done

# 2. Smoke tests
./demo/seed-demo.ps1  # or automated test suite

# 3. Monitor error rate for 15 minutes
# Check Grafana dashboards for anomalies
```

---

## 14. Cost Estimation

### AWS Monthly Cost Estimate

| Resource | Spec | Monthly Cost |
|----------|------|-------------|
| **EKS Cluster** | Control plane | $73 |
| **EC2 Worker Nodes** | 2x m6i.xlarge (4 vCPU, 16 GB) | $280 |
| **RDS PostgreSQL** | db.r6g.large, Multi-AZ, 100 GB | $300 |
| **ElastiCache Redis** | cache.r6g.large | $200 |
| **MSK (Kafka)** | 3x kafka.m5.large | $450 |
| **ALB** | Application Load Balancer | $25 |
| **ECR** | Container registry, 50 GB | $5 |
| **CloudWatch** | Logs + metrics | $50 |
| **Route 53** | DNS hosting | $1 |
| **Secrets Manager** | 10 secrets | $4 |
| **S3** | Backups, 100 GB | $3 |
| **Data Transfer** | ~100 GB/month | $9 |
| **Total** | | **~$1,400/month** |

### Cost Optimization Options

| Strategy | Savings | Trade-off |
|----------|---------|-----------|
| Use Spot Instances for workers | 50-70% on compute | Possible interruptions |
| Single-AZ RDS (staging) | 50% on DB | No HA |
| Confluent Cloud Basic instead of MSK | ~$200/month | External dependency |
| Use Fargate instead of EC2 nodes | Variable | Higher per-pod cost |
| Reserved Instances (1-year) | 30-40% | Commitment |
| Start with t3.xlarge nodes | ~$100 less | Burstable CPU |

---

## Appendix: Quick Reference

### Service Port Map

```
Auth:          8081    Lead:         8082    Account:      8083
Contact:       8084    Opportunity:  8085    Activity:     8086
Notification:  8087    Workflow:     8088    AI:           8089
Email:         8090    Integration:  8091    AI Agent:     9100
Frontend:      3000    PostgreSQL:   5432    Redis:        6379
Kafka:         9092    Zookeeper:    2181
```

### Shared JWT Secret

All 12 services + the AI agent must share the same `JWT_SECRET`. If rotated, all services must restart simultaneously.

### Key Docker Commands (Dev)

```bash
# Start everything
docker compose up -d

# Rebuild single service
mvn package -pl <service> -am -DskipTests
docker compose up -d --build <service>

# View logs
docker logs crm-<service> --tail 100 -f

# Seed demo data
.\demo\seed-demo.ps1
```
