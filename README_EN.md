# mugsun-boot

[中文](README.md) | **English**

![JDK](https://img.shields.io/badge/JDK-21+-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![License](https://img.shields.io/badge/License-Apache%202.0-green)

**The monolithic backend of the Mugsun low-code rapid-development platform** — system administration, workflow, multi-tenancy, an open platform, and product analytics packed into a single executable. Run `mvn spring-boot:run` and a complete system comes alive on first boot.

Built on JDK 21 virtual threads and Spring Boot 3.5, with SM2/SM3/SM4 cryptography and end-to-end auditability baked in — production-grade from day one.

## ✨ Features

| Area | Capabilities |
| --- | --- |
| 🏢 System Admin | Users, roles, menus, departments, positions, dictionaries, business dictionaries, parameters, administrative divisions |
| 📨 Messaging | Unified in-app / email / SMS template delivery with real-time WebSocket push |
| 📦 File Storage | Local / MinIO / cloud object storage with authorized downloads for private attachments |
| 🧩 Low-Code | Code generator, dynamic DDL, online forms, form designer |
| 🔀 Workflow | Visual flow designer, conditional routing, parallel countersigning, todo workbench, approval center |
| 📊 Ops & Monitoring | Server metrics, operation / login / access / error logs, online sessions, cache management, scheduled jobs |
| 🔏 Change Audit | Tamper-evident SM3 hash chain with SM2 signatures, field-level diff timeline |
| 🌐 Open Platform | OAuth2 authorization code + PKCE, API keys, HMAC + nonce replay protection, call logging |
| 📈 Analytics | Event collection, overview / event / performance / error monitoring (sourcemap restore + alerting), session replay, behavior drill-down, funnels, retention, visual point-and-select tracking |
| 🏷 Tenant Operations | Three isolation strategies (column-based / separate datasource), package-driven menu narrowing, customer management |
| 🛡 Security | Encrypted transport with Chinese national ciphers, field-level masking, data permissions, weak-password policy + 2FA, request replay protection |

## 🧱 Tech Stack

JDK 21 (virtual threads) · Spring Boot 3.5 · MyBatis-Flex · Sa-Token · JetCache · Warm-Flow · PostgreSQL 16 (recommended) · Redis 7 · PowerJob · springdoc-openapi · x-file-storage · SMS4J · Hutool · LiteFlow · Flyway · SM2/SM3/SM4 cryptography

## 🗂 Repository Map

Mugsun is split across four repositories; this one is the backend. Clone all four **side by side** (into the same parent directory):

```mermaid
graph LR
    subgraph Frontend
        PC["mugsun-pc<br/>Vue3 admin UI"]
        TRACK["mugsun-track<br/>analytics SDK"]
    end
    subgraph Backend
        BOOT["mugsun-boot<br/>monolith backend (this repo)"]
        CORE["mugsun-core<br/>BOM + starters"]
    end
    CORE -->|Maven dependency| BOOT
    PC -->|file: dependency| TRACK
    PC -->|HTTP / WebSocket| BOOT
    TRACK -->|reports to /track/collect| BOOT
```

## 🔄 Request Pipeline

Every request flows through a single governance chain, so observability and security are solved once at the framework level:

```mermaid
flowchart TB
    REQ["HTTP request"] --> TRACE["TraceIdFilter<br/>X-Trace-Id generation / propagation · MDC"]
    TRACE --> AUTH["Sa-Token authentication"]
    AUTH --> TENANT["Tenant guard<br/>forged tenant-header rejection · package module gating"]
    TENANT --> PERM["@SaCheckPermission button-level authorization"]
    PERM --> DATA["Data-permission weaving · field masking"]
    DATA --> CTRL["Controller<br/>unified R&lt;T&gt; response · exceptions mapped to 401 / 403 / 400"]
    CTRL --> MF["MyBatis-Flex"]
    MF --> PG[("PostgreSQL")]
    CTRL -.->|"@Async virtual threads · tenant context propagated"| LOG["async access-log persistence"]
    MF -.-> CACHE["JetCache two-level cache<br/>Caffeine + Redis"]
```

## 🚀 Quick Start

### Prerequisites

- JDK 21+
- Maven 3.9+
- PostgreSQL 16
- Redis 7

### 1. Initialize the database

```bash
psql -U postgres -f scripts/init-db.sql
```

The script creates the `mugsun` role, the `mugsun` primary database, and the `mugsun_track` analytics database, and grants CREATEDB.

### 2. Build the core

```bash
cd ../mugsun-core && mvn clean install && cd ../mugsun-boot
```

### 3. Run

```bash
mvn spring-boot:run
```

Flyway automatically applies 70+ migrations plus menu seed data, so the very first boot is already a complete system. API docs live at `http://localhost:8080/swagger-ui/index.html` (disabled under the prod profile).

### 4. Sign in

Default credentials: `admin / 123456`. Override with the `MUGSUN_INIT_PASSWORD` environment variable — **you must change this in production**.

### 5. Frontend

The admin UI lives in [mugsun-pc](https://github.com/curdx/mugsun-pc). Follow its side-by-side clone instructions to connect it to this service.

## 🛡 Production Deployment

Activate the prod profile: `--spring.profiles.active=prod`.

- `application-prod.yml` reads everything from environment variables (datasources / Redis / SMTP / SMS / storage / keys) — no plaintext secrets in the repo
- `mugsun.crypto.strict-keys=true`: startup fails fast if any SM4 / SM2 / audit-signing key is missing
- Actuator is fail-closed by default; only `health` is public, and metric endpoints require authenticated authorization
- springdoc API docs are disabled automatically in prod
- Anonymous local-file access is off; private attachments stream through authorized downloads only

## 🧪 Testing & Quality

- **Backend integration tests**: powered by Testcontainers — isolated PostgreSQL 16 / Redis 7 containers, zero external dependencies for `mvn test`, covering real login / permission / tenant / workflow / report / help-feedback flows (30 test classes)
- **Frontend e2e**: 140+ Playwright end-to-end cases (including dedicated flow / tenant / OAuth specs)
- **api-probe**: performance probes hold p95 ≤ 69ms against a 100k-row log table; all 50 security probes pass

## 🗺 Roadmap (planned)

- One-command Docker Compose deployment (Dockerfile + postgres init scripts ready; Compose wiring in progress)
- Microservice distribution
- AI-powered capabilities (model integration / chat / knowledge base)

## 🤝 Contributing

Issues and pull requests are welcome. Please make sure `mvn test` stays green and include tests for new features.

## 📄 License

[Apache License 2.0](LICENSE)

---

If this project helps you, a star ⭐ means a lot!
