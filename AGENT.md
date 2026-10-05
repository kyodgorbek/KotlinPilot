# KotlinPilot Agent Architecture & Lifecycle

## 1. Overview

`KotlinPilotAgent` implements an iterative reasoning and tool execution loop designed specifically for autonomous software engineering in Kotlin/Android repositories.

## 2. Agent Lifecycle

```
1. RECEIVE TASK
   ├── Ingest task description and execution mode (EXPLAIN, ASSISTED, AUTONOMOUS)
   └── Assign unique TaskId

2. REPOSITORY ANALYSIS
   ├── Scan workspace using RepositoryAnalyzer
   ├── Identify Kotlin/Gradle versions, Android Compose, KMP, modules
   ├── Extract architecture conventions (Koin/Hilt, Ktor/Retrofit, Room, Coroutines)
   └── Ingest project rules (.kotlinpilot/rules.md)

3. SYSTEM PROMPT SYNTHESIS
   ├── Inject hard security rules & reality integrity constraints
   ├── Inject project architecture profile & conventions
   └── Register available tools in OpenAI/Groq function schema

4. EXECUTION LOOP (up to maxIterations)
   ├── Send prompt & history to AIProvider (Groq / Fake)
   ├── If Tool Calls returned:
   │   ├── Execute tools through security layer
   │   ├── Capture structured outputs
   │   └── Append results to context
   └── If Final Response returned:
       └── Break loop

5. VALIDATION & CODE REVIEW
   ├── Inspect Git diff of all uncommitted changes
   ├── Check for prohibited patterns (hardcoded secrets, GlobalScope, Thread.sleep in coroutines, architecture mismatches)
   └── Generate CodeReviewResult

6. COMPLETION
   ├── Emit TASK_COMPLETED (or TASK_FAILED)
   └── Deliver final summary
```

## 3. Iteration Limits & Safety

- Default `maxIterations` is 20 (configurable via `MAX_AGENT_ITERATIONS`).
- Prevents infinite loops or runaway API consumption.
- If the agent reaches `maxIterations` without completion, it halts gracefully and reports partial results.
