# KotlinPilot

> **"Your Autonomous Kotlin & Android Engineering Agent"**

KotlinPilot is an autonomous AI software engineering agent designed specifically for **Kotlin, Android, Jetpack Compose, Gradle, and Kotlin Multiplatform** ecosystems.

Unlike generic chatbots, KotlinPilot directly inspects your project, analyzes architecture patterns (Koin/Hilt, Compose/XML, MVI/MVVM, Room, Ktor, Coroutines), modifies files safely within a workspace sandbox, runs Gradle builds/tests, diagnoses errors, and verifies diffs using automated code review.

---

## ⚡ Core Features

- **Autonomous Agent Loop**: Plan $\rightarrow$ Execute Tools $\rightarrow$ Observe Results $\rightarrow$ Reason $\rightarrow$ Build/Test $\rightarrow$ Code Review $\rightarrow$ Deliver.
- **Deep Kotlin & Android Awareness**: Automatically detects Jetpack Compose, KMP, Coroutines, Room, Ktor, Retrofit, Koin/Hilt, version catalogs, and Gradle modules.
- **Architectural Fidelity**: Honors project conventions and project-level `.kotlinpilot/rules.md`.
- **Pluggable AI Providers**: First-class support for **Groq API** (`llama-3.3-70b-versatile`) with domain abstractions and `FakeAIProvider` for local testing.
- **Robust Security Sandbox**: Strict workspace path confinement (preventing path traversal and secret leakage) and command allowlisting (preventing arbitrary system command execution).
- **Tool Suite**: Safe filesystem manipulation, code search, terminal runner, Gradle tasks (`build`, `test`, `lint`), and Git branch/diff/commit management.
- **Interactive CLI**: Inspect projects with `analyze`, execute tasks with `task`, review diffs with `review`, and inspect status with `status`.

---

## 🚀 Quick Start

### 1. Prerequisites

- **JDK 21** or higher
- **Gradle 8.x** (wrapper provided)
- **Groq API Key** (Get free development tier key at [console.groq.com](https://console.groq.com))

### 2. Environment Setup

Copy `.env.example` to `.env` and fill in your Groq API key:

```bash
cp .env.example .env
```

Edit `.env`:

```env
AI_PROVIDER=groq
GROQ_API_KEY=gsk_your_groq_api_key_here
GROQ_MODEL=llama-3.3-70b-versatile
PROJECT_WORKSPACE=/path/to/your/kotlin/or/android/project
MAX_AGENT_ITERATIONS=20
COMMAND_TIMEOUT_SECONDS=120
AGENT_MODE=ASSISTED
```

### 3. Build & Test

Build the project and run all unit and integration tests:

```bash
./gradlew build
./gradlew test
```

---

## 💻 CLI Usage

Run the KotlinPilot CLI using Gradle:

### 1. Analyze Project Architecture

Inspect a repository, its modules, frameworks, versions, and conventions:

```bash
./gradlew run --args="analyze"
```

Output:
```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  KotlinPilot - Autonomous Kotlin & Android Agent
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Analyzing workspace: /path/to/project ...

  Project Name:     KotlinPilot
  Kotlin:           ✓ Detected (2.2.20)
  Gradle:           ✓ Detected (8.14)
  Android Compose:  ✓ Yes
  Kotlin Multiplatform: ✗ No
  Modules (1):
    - :root (Android: false, Compose: false, KMP: false)

  Architecture & Conventions:
    - [Concurrency] Kotlin Coroutines & Flow (Coroutines core / Flow detected)
    - [Networking] Ktor Client (Found Ktor client dependencies)
    - [Serialization] Kotlinx Serialization (Found kotlinx.serialization plugin)

  Test Frameworks:   JUnit 5 (Jupiter), Kotlin Test
```

### 2. Run an Engineering Task

Execute a natural language task:

```bash
./gradlew run --args='task "Fix the Gradle compilation errors" --autonomous'
```

Available task options:
- `--autonomous`: Fully autonomous execution (applies changes, runs Gradle, reviews diff).
- `--assisted`: Prompts before critical operations (default).
- `--explain`: Read-only analysis and plan generation without touching files.
- `--provider <name>`: Specify AI provider (`groq` or `fake`).
- `--model <name>`: Override model (e.g. `llama-3.3-70b-versatile`).

### 3. Start REST API & Web Dashboard Server

Start the embedded Ktor server with real-time SSE live streaming and modern Web UI:

```bash
./gradlew run --args="server --port 8080"
```

Open your browser to:
- **Dashboard UI**: `http://localhost:8080` (or `http://localhost:8080/dashboard`)
- **Health Check**: `http://localhost:8080/health`
- **Project Architecture**: `http://localhost:8080/api/project`
- **Live SSE Event Stream**: `http://localhost:8080/api/tasks/{id}/events`

### 4. Code Review & Status

Check Git status or perform automated pattern checks on working tree changes:

```bash
./gradlew run --args="status"
./gradlew run --args="diff"
./gradlew run --args="review"
```

---

## 📱 Using KotlinPilot with Your Personal Android Projects

You can use KotlinPilot to autonomously build, fix, and refactor code in your **external Android Studio projects**:

### Method 1: Set Target Workspace in `.env`
Edit your `.env` file and specify the path to your Android app:
```env
PROJECT_WORKSPACE=C:\Users\Edgar\AndroidStudioProjects\MyAndroidApp
```
Then run tasks or start the server:
```bash
./gradlew run --args="analyze"
./gradlew run --args='task "Create a Jetpack Compose LoginScreen with MVI ViewModel" --autonomous'
./gradlew run --args="server"
```

### Method 2: Pass `--workspace` CLI Argument
Point KotlinPilot to any Android Studio project on the fly without changing `.env`:
```bash
./gradlew run --args='task "Add Room database entities for Notes" --workspace "C:\Users\Edgar\AndroidStudioProjects\NotesApp" --autonomous'
```

### Method 3: Side-by-Side with Android Studio
1. Keep **Android Studio** open with your Android project.
2. Open **KotlinPilot** in IntelliJ IDEA (or run the web dashboard on `http://localhost:8080`).
3. Set `PROJECT_WORKSPACE` to your Android project path.
4. When you execute tasks, KotlinPilot writes files, builds with Android Gradle (`gradlew`), and runs tests in the background. Android Studio will automatically synchronize and show your new files and updates in real time!

---

## 🛡️ Project Rules (`.kotlinpilot/rules.md`)

You can define project-level conventions by creating a `.kotlinpilot/rules.md` file in your repository root:

```markdown
# KotlinPilot Project Rules

Architecture: Clean Architecture + MVI
UI: Jetpack Compose
Dependency Injection: Koin
Networking: Ktor
Persistence: Room

Rules:
- Do not introduce Hilt into this project.
- Prefer immutable state models.
- Always use structured concurrency (avoid GlobalScope).
- Ensure all new public APIs have unit tests.
```

KotlinPilot automatically ingests these rules prior to generating plans or modifying code.

---

## 🏛️ Architecture Overview

```
io.kotlinpilot
│
├── app.cli                 # Command-line interface & main entrypoint
│
├── core
│   ├── domain              # Task, ProjectAnalysis, CodeReview models
│   ├── ai                  # AIProvider, AIRequest, AIResponse, FakeAIProvider
│   ├── tools               # AgentTool, ToolInput, ToolResult, ToolRegistry
│   ├── agent               # KotlinPilotAgent (Loop, Reasoning, Planning), CodeReviewer
│   └── security            # WorkspaceSecurityPolicy, CommandSecurityPolicy
│
└── infrastructure
    ├── groq                # GroqAIProvider (Ktor CIO client, OpenAI-compatible format)
    ├── filesystem          # SafeFileSystemService & FileSystemTools
    ├── terminal            # SafeTerminalService & TerminalTool (allowlisting)
    ├── gradle              # GradleService & GradleTools (build, test, lint)
    ├── git                 # GitService & GitTools (status, diff, branch, commit)
    ├── analyzer            # RepositoryAnalyzer (structure, dependencies, rules)
    ├── config              # KotlinPilotConfig & .env loader
    └── logging             # KotlinPilotLogger with secret masking
```

---

## 🔒 Security Guarantee

- **Sandbox Containment**: File operations strictly validated against the target workspace directory. Path traversal attempts (`../../`) are blocked.
- **Sensitive File Protection**: Sensitive files (`.env`, `.ssh/id_rsa`, `.git/config`, AWS/GPG keys) are blocked from agent access.
- **Command Allowlisting**: Only development commands (`git`, `gradlew`, `gradle`, `java`, `kotlinc`) can be executed. Destructive commands (`sudo`, `rm -rf /`, `shutdown`, etc.) are rejected immediately.
- **Secret Redaction**: API keys and tokens are filtered and redacted from all application logs.

---

## 📄 License

Apache License 2.0. See LICENSE for details.
