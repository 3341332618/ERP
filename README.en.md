# ERP Management Platform

Language: [中文](./README.md) | English

A full-stack ERP demo system built with Spring Boot 3 and Vue 3. It covers purchasing, sales, inventory, settlement, and a software-testing training module where administrators can publish controlled defects while students discover issues independently.

This project is suitable for coursework, training labs, ERP process demos, and full-stack development practice. MySQL infrastructure is already connected through Docker Compose, Flyway migrations, Spring Data JPA configuration, and MySQL Connector/J.

> Current status: database schema, startup configuration, and migration tests are available. Business logic is still being migrated from the in-memory `ErpStore` to database repositories, so this version should not be described as fully persistent for every business operation yet.

## Screenshots

| Login page | Dashboard |
| --- | --- |
| [![Login page](./docs/assets/screenshots/login-page.png)](./docs/assets/screenshots/login-page.png) | [![Dashboard](./docs/assets/screenshots/home-dashboard.png)](./docs/assets/screenshots/home-dashboard.png) |

## Quick Links

- [Quick Start](#quick-start)
- [Demo Accounts](#demo-accounts)
- [Test and Build](#test-and-build)
- [Deployment](#deployment)
- [Operation Manual](./docs/operation-manual.md)
- [Project Showcase](./docs/project-showcase.md)
- [Super Admin Test Plan](./docs/testing/super-admin-flow-test-plan.md)
- [Requirements and Training Materials](./docs/requirements/)
- [Database Migration Script](./backend/src/main/resources/db/migration/V1__create_schema.sql)
- [Frontend Source](./frontend/src/) / [Backend Source](./backend/src/main/java/com/erp/)

## Features

| Module | Implemented capabilities |
| --- | --- |
| Authentication and profile | Username/password login, JWT authentication, current user, password change, avatar upload, logout |
| Master data | Brands, categories, units, products, warehouses, customers, suppliers, search, status toggle, product import |
| Purchasing | Purchase inbound, purchase return, edit, submit, delete, detail view |
| Sales | Sales outbound, sales return, edit, submit, delete, detail view |
| Inventory | Stock distribution, inbound/outbound audit, approve/reject, stock transfer, warehouse notifications |
| Settlement | Income and expense settlement, settlement details, related business document view |
| Testing competition | Defect library, student accounts, independent testing, defect reports, test files, scoring, operation logs, ranking |

## Business Flow

```text
Master data
   ↓
Create purchase / sales / transfer documents
   ↓
Draft → Pending audit → Approved / Rejected
   ↓
Inventory mutation + settlement generation + warehouse notification
```

Published defects enable corresponding wrong system behavior, but students do not see the defect IDs, summaries, or task list. Students must explore the ERP workspace and submit their own findings.

## Tech Stack

Backend:

- Java 17
- Spring Boot 3.3.6
- Spring Web / Spring Security / Spring Data JPA
- Flyway
- MySQL Connector/J
- JJWT
- Maven Wrapper
- JUnit 5 / Spring Boot Test / Testcontainers

Frontend:

- Vue 3.5
- TypeScript 5.6
- Vite 6
- Vue Router 4
- Pinia 2
- Element Plus 2
- Axios 1.7
- Vitest 2

Database:

- MySQL 8.4
- Local default: `127.0.0.1:3307/erp`
- Schema migration: `backend/src/main/resources/db/migration/V1__create_schema.sql`

## Quick Start

Prerequisites:

- JDK 17 or later
- Node.js 20 LTS or compatible version
- npm
- Docker Desktop or Docker Engine with Docker Compose

Check the environment:

```powershell
java -version
node --version
npm.cmd --version
docker --version
docker compose version
```

Start MySQL:

```powershell
cd "D:\ai work\ERP"
Copy-Item .env.example .env
docker compose up -d mysql
docker compose ps
```

Start the backend:

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd spring-boot:run
```

Start the frontend:

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd install
npm.cmd run dev
```

Open:

```text
http://127.0.0.1:5173
```

The Vite dev server proxies `/api` requests to `http://127.0.0.1:8080`.

## Demo Accounts

All built-in accounts use the initial password:

```text
123456
```

| Account | Role | Usage |
| --- | --- | --- |
| `admin` | System administrator | ERP modules and testing competition management |
| `superadmin` | Super administrator | Student management, defect publishing, file review, operation logs, scoring history |
| `purchase_manager` | Purchase manager | Purchase module and purchase documents |
| `warehouse_manager` | Warehouse manager | Inventory and audit workflows |
| `sales_manager` | Sales manager | Sales module and sales documents |
| `settlement_manager` | Settlement manager | Income and expense settlement |
| `student01` | Student | Independent ERP workspace and testing competition |
| `student02` | Student | Independent ERP workspace and testing competition |

## Test and Build

Frontend tests:

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd test -- --run
```

Frontend production build:

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd run build
```

Output:

```text
frontend/dist/
```

Backend tests:

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd test
```

Backend package:

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd clean package
```

Output:

```text
backend/target/erp-backend-0.0.1-SNAPSHOT.jar
```

## Deployment

The project is deployed as three parts:

1. MySQL database
2. Spring Boot backend JAR
3. Vue frontend static files

Backend startup example:

```powershell
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3307"
$env:DB_NAME = "erp"
$env:DB_USERNAME = "erp"
$env:DB_PASSWORD = "erp_local_password"
$env:ERP_JWT_SECRET = "replace-with-a-long-random-secret"

java -jar ".\backend\target\erp-backend-0.0.1-SNAPSHOT.jar"
```

Frontend deployment:

1. Run `npm.cmd run build`.
2. Serve `frontend/dist/` with Nginx, Caddy, or another static server.
3. Proxy `/api` to the backend service on port `8080`.

## Current Limitations

- Business persistence is still being migrated from `ErpStore` to repository-backed storage.
- Uploaded competition files are stored under the backend runtime directory: `uploads/competition/<username>/`.
- Production deployment should replace the default JWT secret and configure persistent file storage, backup, and monitoring.

## License

No license file is currently provided. Add a clear license before publishing or reusing this project publicly.
