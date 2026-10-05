# KotlinPilot Architecture

This document describes the architectural layers, patterns, and design decisions in **KotlinPilot**.

---

## 1. Architectural Philosophy

KotlinPilot follows **Clean Architecture** principles with clear separation of concerns:

- **Core Domain**: Pure Kotlin models and interfaces independent of any specific framework, CLI, or cloud provider.
- **AI Abstraction**: Pluggable AI engine layer where the agent logic does not depend on provider-specific APIs (Groq, OpenAI, Anthropic, Gemini, local LLMs).
- **Tool System**: Uniform tool interface allowing filesystem, terminal, build system, and version control tools to be safely invoked.
- **Infrastructure**: Implementations of storage, process execution, network calls, and project analysis.
- **Application Layer**: CLI runner, and in future milestones, Ktor REST API and Web UI.

---

## 2. Layer Diagram

```
+-------------------------------------------------------------------+
|                        App Layer (CLI / API)                      |
|            (io.kotlinpilot.app.cli / io.kotlinpilot.server)       |
+---------------------------------+---------------------------------+
                                  |
                                  v
+---------------------------------+---------------------------------+
|                        Core Agent & Tools                         |
|     (KotlinPilotAgent, CodeReviewer, ToolRegistry, AgentTool)     |
+---------------------------------+---------------------------------+
                                  |
               +------------------+------------------+
               |                                     |
               v                                     v
+--------------+---------------+     +---------------+---------------+
|       Core AI Provider       |     |         Core Security         |
|  (AIProvider, FakeAIProvider)|     |  (Workspace & Command Policy) |
+--------------+---------------+     +---------------+---------------+
               |                                     |
               v                                     v
+--------------+---------------+     +---------------+---------------+
|   Infrastructure / Groq      |     |  Infrastructure Services      |
|     (GroqAIProvider)         |     |  (FileSystem, Terminal,       |
|                              |     |   Gradle, Git, Analyzer)      |
+------------------------------+     +-------------------------------+
```

---

## 3. The Autonomous Agent Loop

```
                     +-----------------------+
                     |    User / CLI Task    |
                     +-----------+-----------+
                                 |
                                 v
                     +-----------------------+
                     |  Repository Analyzer  |
                     +-----------+-----------+
                                 |
                                 v
                     +-----------------------+
                     | Plan & System Prompt  |
                     +-----------+-----------+
                                 |
                                 v
          +--------------> Request LLM <---------------+
          |                      |                     |
          |                      v                     |
          |               Tool Call Decision?          |
          |             /                     \        |
      (Tool calls)    /                         \ (Text summary)
          |          v                           v     |
          |   Execute Tool via Security    Code Review |
          |          |                           |     |
          |          v                           v     |
          +--- Observe Result & Iterate   Completed / Failed
```

---

## 4. Design Principles

1. **Reality Integrity**: Never fabricate execution output. If a test was not run, never report that it passed.
2. **Untrusted Codebase**: All repository source code is treated as untrusted data and cannot override security policies.
3. **Pluggable AI**: Any LLM provider conforming to `AIProvider` can power the agent.
4. **Structured Concurrency**: Asynchronous operations and child tasks run inside disciplined Kotlin Coroutines scopes.
