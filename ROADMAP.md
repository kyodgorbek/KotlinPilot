# KotlinPilot Roadmap

## Phase 1: Local Foundation & Autonomous CLI (Completed ✓)
- [x] Clean Kotlin/JVM, Gradle Kotlin DSL, JDK 21 foundation
- [x] AIProvider abstraction with GroqAIProvider (`llama-3.3-70b-versatile`) and FakeAIProvider
- [x] Security layer (workspace isolation + command allowlisting)
- [x] Tool system (Filesystem, Terminal, Gradle, Git)
- [x] RepositoryAnalyzer (Kotlin, Gradle, Compose, KMP, Koin/Hilt, Room, Ktor, test frameworks, `.kotlinpilot/rules.md`)
- [x] Autonomous Agent Loop with planning, tool invocation, iteration limits, error recovery
- [x] CodeReviewer for diffs and anti-pattern detection
- [x] CLI commands (`analyze`, `task`, `status`, `diff`, `review`)
- [x] Complete test suite and documentation

## Phase 2: Ktor API & Real-time Progress (Completed ✓)
- [x] Ktor REST API endpoints (`/health`, `/api/tasks`, `/api/projects`, `/api/status`, `/api/diff`, `/api/review`)
- [x] Server-Sent Events (SSE) for live `TaskEvent` streaming

## Phase 3: Web Dashboard UI (Completed ✓)
- [x] Embedded visual dashboard for projects, tasks, live agent logs, diffs, and code reviews
- [x] Real-time dark-mode UI served directly at `http://localhost:8080/dashboard`

## Phase 4: GitHub Integration (Completed ✓)
- [x] `GitProvider` abstraction & `GitHubProvider` implementation
- [x] Automated branch creation, pushing, and Pull Request (PR) generation
- [x] GitHub Actions CI/CD pipeline (`.github/workflows/ci.yml`)

## Phase 5: Execution Environment Sandboxing (Completed ✓)
- [x] `ExecutionEnvironment` abstraction (`LocalExecutionEnvironment`, `DockerExecutionEnvironment`)
- [x] Pluggable sandbox boundary for command execution

## Phase 6: SaaS, Billing & Teams (Upcoming)
- [ ] PostgreSQL persistence
- [ ] User accounts, organizations, usage metering, and subscription tiers
