# ERP Management Platform

Language: [中文](./README.md) | English

A full-stack ERP demo system built with Spring Boot 3 and Vue 3. It covers purchasing, sales, inventory, settlement, and a software-testing training module where the super administrator can publish controlled defects while students discover issues independently.

This project is suitable for coursework, training labs, ERP process demos, and full-stack development practice. The repository includes Docker Compose + MySQL 8.4 + Flyway schema scripts for local startup, schema verification, and deployment demos.

> Note: this is a teaching and demo-oriented ERP project focused on business workflow, role permissions, controlled defect publishing, and independent student testing.

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
| Testing competition | Defect library, student accounts, generated student ERP subaccounts, independent testing, defect reports, test files, scoring, operation logs, ranking |

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

Published defects enable corresponding wrong system behavior, but students do not see the defect IDs, summaries, or task list. Each student has an isolated ERP workspace and generated role accounts for workspace administration, purchasing, warehouse, sales, and settlement operations. Students first sign in with the main account such as `student01`, click "Enter my ERP workspace", choose a role, and enter the password. The frontend automatically uses the matching `student01_*` account, so students do not type the prefix manually.

## Tech Stack

Backend:

- Java 17
- Spring Boot 3.3.6
- Spring Web / Spring Security / Spring Data JPA
- MyBatis-Plus 3.5.x
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
- Local default: `127.0.0.1:3306/erp`
- Schema migrations: `backend/src/main/resources/db/migration/V1__create_schema.sql` and later Flyway migrations. `V4__remove_platform_admin.sql` renames and disables the historical platform `admin` account; platform competition management now uses `superadmin`.
- Business data is persisted with real-time MySQL writes. On startup the backend loads current rows with `SELECT`; during runtime each business action writes the related tables immediately with targeted `INSERT` / `UPDATE` / `DELETE`. It is not a whole-state snapshot save/load design.
- Demo data is seeded only when the business tables are empty. If MySQL already contains users/workspaces/business rows, the backend uses the existing database data.

Backend architecture:

```text
Controller -> DTO -> Service -> Store/Domain -> Mapper -> MySQL
```

- `web/`: controller layer for HTTP routing and unified API responses.
- `dto/`: request DTOs for login, student creation, defect publishing, review, and status changes.
- `service/`: business entry layer used by controllers.
- `domain/`: ERP domain models and enums.
- `store/`: core ERP business rules, student workspace isolation, defect switches, and realtime persistence orchestration; complex document, inventory, and settlement SQL is still centralized here and can be moved into custom mappers incrementally.
- `entity/`: MyBatis-Plus table entities.
- `mapper/`: MyBatis-Plus `BaseMapper` interfaces for core tables such as workspaces, users, products, documents, defect definitions, and defect reports.

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
| `superadmin` | Super administrator | Student management, defect publishing, defect report review, file review, operation logs, scoring history, rankings |
| `purchase_manager` | Purchase manager | Purchase module and purchase documents |
| `warehouse_manager` | Warehouse manager | Inventory and audit workflows |
| `sales_manager` | Sales manager | Sales module and sales documents |
| `settlement_manager` | Settlement manager | Income and expense settlement |
| `student01` | Student main account | Submit defect reports, upload test files, view personal records and rankings |
| `student01_admin` | Student ERP administrator | Maintain master data and view ERP modules in the `student01` workspace; no platform defect publishing or student-management permission |
| `student01_purchase_staff` | Student ERP purchase staff | Create purchase inbound and purchase return documents in `student01` workspace |
| `student01_warehouse_staff` | Student ERP warehouse staff | Audit inbound, outbound, and transfer documents in `student01` workspace |
| `student01_sales_staff` | Student ERP sales staff | Create sales outbound and sales return documents in `student01` workspace |
| `student01_settlement_manager` | Student ERP settlement manager | View income and expense settlements in `student01` workspace |
| `student02` | Student main account | Same workflow with an isolated `student02` workspace |

Student ERP subaccounts are isolated by workspace. For example, `student01_warehouse_staff` cannot see or audit `student02` documents. In the secondary ERP login, students choose a role and enter the password; the UI composes accounts such as `student01_admin` and `student01_purchase_staff` automatically. Logging out from a student ERP subaccount restores the saved primary student session, so the student can return to defect-report submission without typing `student01` again.

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
$env:DB_PORT = "3306"
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

- Uploaded competition files are stored under the backend runtime directory: `uploads/competition/<username>/`.
- Production deployment should replace the default JWT secret and configure persistent file storage, backup, and monitoring.

## License

No license file is currently provided. Add a clear license before publishing or reusing this project publicly.
