# KotlinPilot Development Guide

## Setup & Building

1. Ensure **JDK 21** is active.
2. Build the project:
   ```bash
   ./gradlew build
   ```
3. Run tests:
   ```bash
   ./gradlew test
   ```

## Running the CLI Locally

```bash
./gradlew run --args="analyze"
./gradlew run --args='task "Describe this project" --provider fake'
```

## Adding New Tools

To add a new tool:
1. Implement `AgentTool` in `core.tools` or appropriate `infrastructure` package.
2. Define parameters with `AIToolParameter`.
3. Register the tool in `ToolRegistry` (see `KotlinPilotCli.kt`).
4. Write unit tests for the new tool.

## Adding New AI Providers

To add a new AI provider (e.g. OpenAI, Anthropic, Gemini, Ollama):
1. Implement `AIProvider` in `infrastructure.<provider>`.
2. Translate requests/responses to/from domain models (`AIRequest`, `AIResponse`, `AIMessage`, `AIToolCall`, `AIToolResult`).
3. Add configuration resolution in `KotlinPilotConfig` and CLI.
