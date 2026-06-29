# ERP Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce an accurate comprehensive README and a presentation-oriented project guide for the ERP repository.

**Architecture:** Keep operational and developer information in the repository-root README. Keep classroom presentation, demonstration order, and defense talking points in a separate document under `docs/`; derive every claim from source files, configuration, and tests.

**Tech Stack:** Markdown, Spring Boot 3.3.6, Java 17, Vue 3.5, TypeScript, Vite 6, Maven, npm

---

### Task 1: Map Verified Project Facts

**Files:**
- Read: `backend/src/main/java/com/erp/web/*.java`
- Read: `backend/src/main/java/com/erp/security/*.java`
- Read: `backend/src/test/java/com/erp/*.java`
- Read: `frontend/src/api/index.ts`
- Read: `frontend/src/router/index.ts`
- Read: `backend/src/main/resources/application.yml`
- Read: `backend/pom.xml`
- Read: `frontend/package.json`

- [x] **Step 1: Extract controller base paths, endpoint methods, roles, and business actions**

Run:

```powershell
rg -n '@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|hasRole|hasAnyRole' backend/src/main/java/com/erp
```

Expected: a searchable list of implemented endpoints and access rules.

- [x] **Step 2: Extract tested business rules**

Run:

```powershell
rg -n '@Test|assertThat|status\(\)|jsonPath' backend/src/test frontend/src/tests
```

Expected: a searchable list of verified workflows and expected results.

### Task 2: Rewrite the Main README

**Files:**
- Modify: `README.md`

- [x] **Step 1: Replace the corrupted document with UTF-8 Markdown**

Write the following verified sections: overview, features, business flow, tech stack, repository layout, prerequisites, startup, demo accounts, configuration, API summary, test/build commands, deployment, troubleshooting, limitations, and related documentation.

- [x] **Step 2: Scan for unsupported or incomplete claims**

Run:

```powershell
rg -n 'TBD|TODO|待补充|待完善|localhost:[0-9]+|127\.0\.0\.1:[0-9]+' README.md
```

Expected: only verified local addresses and no placeholders.

### Task 3: Create the Presentation Guide

**Files:**
- Create: `docs/project-showcase.md`

- [x] **Step 1: Write a presentation-ready guide**

Include project positioning, module map, role map, technical highlights, preparation checklist, a 5-10 minute demonstration sequence, core business-loop narration, defense script, and repository-backed answers to common questions.

- [x] **Step 2: Cross-check the guide against the README**

Run:

```powershell
rg -n '^## |^### ' README.md docs/project-showcase.md
```

Expected: the README covers operation and development; the showcase guide covers presentation and defense without conflicting facts.

### Task 4: Verify Documented Commands

**Files:**
- Verify: `README.md`
- Verify: `docs/project-showcase.md`

- [x] **Step 1: Run backend tests**

Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: Maven exits with code 0.

- [x] **Step 2: Run the frontend production build**

Run:

```powershell
cd frontend
npm.cmd run build
```

Expected: Vite exits with code 0 and creates `frontend/dist`.

- [x] **Step 3: Review only documentation changes**

Run:

```powershell
git diff -- README.md docs/project-showcase.md docs/superpowers/specs/2026-06-21-erp-documentation-design.md docs/superpowers/plans/2026-06-21-erp-documentation.md
```

Expected: no application source files are changed by this documentation task.
