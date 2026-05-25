# 📡 Prospection Radar

> AI-powered mission radar for freelance Java Tech Leads — detect opportunities on the visible **and hidden** market, scored and analyzed by Claude AI.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green?style=flat-square&logo=springboot)
![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square&logo=angular)
![Claude AI](https://img.shields.io/badge/Claude-Sonnet_4.6-blueviolet?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## The Problem

As a freelance Java Tech Lead, finding your next mission is painful:

- **Visible market** (job boards): high competition, slow response, generic offers
- **Hidden market** (~60–70% of missions never posted): requires network intelligence, timing, and signal detection
- **No fit scoring**: you spend hours reading irrelevant offers
- **No follow-up system**: relances forgotten, pipeline scattered across emails

**Prospection Radar** solves all of this in one tool.

---

## Features

### Module 1 — RADAR (Implemented)

| Feature | Description |
|---------|-------------|
| **Multi-source scraping** | Malt, Freelance.com (Playwright + Jsoup) |
| **Hidden market signals** | Funding rounds, CTO nominations (Frenchweb, Maddyness) |
| **Claude AI fit scoring** | 0–100 score per mission based on your profile |
| **Decision maker detection** | CTO vs HR vs Manager — who to contact |
| **Auto-scheduling** | Scans at 8h, 12h, 18h every day |
| **REST API** | Full CRUD + manual scan triggers |
| **Angular 17 Dashboard** | Dark-themed UI with mission table + signal cards |

### Roadmap

| Module | Description | Status |
|--------|-------------|--------|
| 2 — QUALIFICATION | Contact scoring, warmth level, LinkedIn profile analysis | Planned |
| 3 — OUTREACH | Claude-generated personalized messages (LinkedIn + email) | Planned |
| 4 — PIPELINE | Kanban board (Identified → Contacted → In Discussion → Mission) | Planned |
| 5 — ANALYTICS | Response rates, TJM market intelligence, profile fitness score | Planned |

---

## Architecture

```
prospection-radar/
├── backend/                          # Spring Boot 3.2 — Java 17
│   └── src/main/java/com/radar/prospection/
│       ├── scraper/                  # Malt (Playwright), Freelance.com (Jsoup)
│       ├── signal/                   # Hidden market detectors (Frenchweb, Maddyness)
│       ├── claude/                   # Claude AI — fit scoring + signal analysis
│       ├── domain/                   # Mission, Signal, enums
│       ├── repository/               # Spring Data JPA
│       ├── scheduler/                # Quartz — 3x daily auto-scan
│       └── api/                      # REST controllers + DTOs
│
└── frontend/                         # Angular 17 — Standalone components
    └── src/app/
        ├── core/                     # Models + RadarService + ToastService
        ├── layout/topbar/            # Navigation + scan button
        ├── features/
        │   ├── dashboard/            # Stats cards + source bar chart
        │   ├── missions/             # Sortable table + status dropdown
        │   └── signals/              # Hidden market signal cards
        └── shared/components/        # ScoreBadge, ToastContainer
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17, Spring Data JPA, Quartz |
| Scraping | Playwright (JS-rendered pages), Jsoup (static HTML) |
| AI | Claude Sonnet 4.6 via Anthropic API |
| Database | H2 (dev) / PostgreSQL (prod) |
| Frontend | Angular 17, Standalone components, Signals API |
| HTTP Proxy | Angular Dev Proxy → Spring Boot :8080 |

---

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| Angular CLI | 17+ |
| Anthropic API Key | [Get one here](https://console.anthropic.com) |

### Installation

```bash
git clone https://github.com/riadh-mnasri/prospection-radar.git
cd prospection-radar

# Install Angular dependencies
cd frontend && npm install && cd ..
```

### Configuration

Set your Anthropic API key:

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

Customize your freelance profile in `backend/src/main/resources/application.yml`:

```yaml
radar:
  profile:
    skills:
      - Java
      - Spring Boot
      - Tech Lead
      - Microservices
    tjm-min: 600
    tjm-max: 900
    remote-preference: FULL_REMOTE
    location: Paris

  scraping:
    keywords:
      - "Tech Lead Java"
      - "Architecte Java"
      - "Lead Developer Java"
```

---

## Running the App

**Terminal 1 — Backend:**

```bash
cd backend
mvn spring-boot:run
# Starts on http://localhost:8080
```

**Terminal 2 — Frontend:**

```bash
cd frontend
ng serve
# Starts on http://localhost:4200
```

Open **http://localhost:4200** in your browser.

---

## API Reference

Base URL: `http://localhost:8080/api/radar`

### Missions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/missions` | List missions (`?minScore=70&status=NEW`) |
| `GET` | `/missions/top` | Missions with fit score ≥ 70 |
| `PATCH` | `/missions/{id}/status` | Update pipeline status |
| `POST` | `/scan/missions` | Trigger an immediate scan |

### Signals

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/signals` | List signals (`?minScore=60`) |
| `GET` | `/signals/hot` | Signals with opportunity score ≥ 60 |
| `POST` | `/scan/signals` | Trigger a signal detection run |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/stats` | Aggregated dashboard stats |

### Example — Trigger a full scan

```bash
curl -X POST http://localhost:8080/api/radar/scan/missions
# → {"newMissions": 12, "duplicates": 3, "analyzed": 12, "errors": []}

curl -X POST http://localhost:8080/api/radar/scan/signals
# → {"newSignals": 4, "errors": []}
```

### Example — Get top missions

```bash
curl http://localhost:8080/api/radar/missions/top | jq '.[0]'
# → {
#     "id": 1,
#     "source": "MALT",
#     "title": "Tech Lead Java / Spring Boot",
#     "tjmMin": 650, "tjmMax": 750,
#     "fitScore": 87,
#     "fitSummary": "Excellent fit — Spring Boot, microservices, remote...",
#     "decisionMakerHint": "Contact the CTO directly via LinkedIn...",
#     "status": "NEW"
#   }
```

---

## Hidden Market Signals

The radar detects signals that indicate a company will need a Tech Lead **before they post any job offer**:

| Signal | Source | Opportunity |
|--------|--------|-------------|
| 💰 Funding round | Frenchweb, Maddyness | Company will hire in 2–3 months |
| 👤 New CTO/VP Eng | Frenchweb | Reorganization = new tech needs |
| 📈 3+ dev job offers | LinkedIn, job boards | Project ramp-up = need for TL |
| ⏳ Job open 30+ days | Job boards | Difficulty hiring = external TL |
| 🤝 Former colleague promoted | LinkedIn | Warm contact, existing relationship |

---

## Mission Pipeline Statuses

```
NEW → ANALYZED → SHORTLISTED → CONTACTED → REPLIED → IN_DISCUSSION → ✅ MISSION
                                                                    ↘ ARCHIVED
```

Change status directly from the Angular UI or via API:

```bash
curl -X PATCH http://localhost:8080/api/radar/missions/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SHORTLISTED"}'
```

---

## Database Console (Development)

H2 console available at **http://localhost:8080/h2-console**

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:file:./data/radar` |
| Username | `radar` |
| Password | `radar` |

Useful queries:

```sql
-- Top missions this week
SELECT title, source, fit_score, status
FROM missions
ORDER BY fit_score DESC
LIMIT 20;

-- Hot signals
SELECT company, type, opportunity_score, suggested_action
FROM signals
WHERE opportunity_score >= 60
ORDER BY opportunity_score DESC;
```

---

## Production Deployment

Switch to PostgreSQL in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/radar
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Build the Angular app and copy to Spring Boot static resources:

```bash
cd frontend
ng build --configuration production
cp -r dist/frontend/* ../backend/src/main/resources/static/
```

Then only one process to run:

```bash
cd backend && mvn spring-boot:run
# Full app available on http://localhost:8080
```

---

## Contributing

This project is primarily built for personal use by freelance Java Tech Leads. PRs welcome for:

- New scrapers (Talent.io, Comet, APEC)
- New signal detectors (LinkedIn, GitHub activity)
- Module 3: outreach message generation
- Module 4: Kanban pipeline

---

## License

MIT — free to use, fork, and sell.

---

*Built with [Claude Code](https://claude.ai/code) by a freelance Java Tech Lead, for freelance Java Tech Leads.*
