# Mini-UPS NLQ MCP Server

Natural-language query MCP Server for Mini-UPS. It orchestrates OpenRouter LLMs (OpenAI/Gemini, etc.) to parse intents, call the backend read-only APIs, and generate friendly answers.

## Quick Start (Local)

- Requirements: Node.js 20+, npm.
- Setup env: copy `.env.example` to `.env` and set `OPENROUTER_API_KEY` and `BACKEND_BASE_URL`.

Commands:
- `npm run dev` – run via tsx for local dev.
- `npm run build && npm start` – compile then run.
- `npm test` – run unit/integration tests (Vitest).

Environment variables (common):
- `OPENROUTER_API_KEY`: your OpenRouter key (required)
- `OPENROUTER_MODEL_FAST`/`STRICT`/`ANSWER`: model names (optional overrides)
- `BACKEND_BASE_URL`: Mini-UPS backend base URL (required)

## Docker

Build and run inside the project root using docker compose (service `mcp-server`). Ensure `.env` contains `OPENROUTER_*` and backend envs, or set them in the compose file.

## Tools

The server exposes MCP tools:
- `nlq_query`: natural language query (shipments, orders, inventory, health)
- `health_check`: component health status
- `get_system_stats`: runtime stats and cost tracking
- `clear_cache`: clear caches (orchestrator/backend/OpenRouter)

## Notes

- Only read operations are allowed. No DB direct access.
- Logs redact PII by default. Configure via env.
- Costs are approximated and tracked daily.

## Endpoint Allowlist

The orchestrator only allows calling endpoints defined in `src/schemas/endpoints.ts`. Update this list to match your backend’s actual REST routes (methods and path templates). The intent-parsing prompt dynamically lists this allowlist to keep LLM planning aligned with reality.
