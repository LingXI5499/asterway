# Asterway development conventions

- Use Vue and TypeScript for this frontend. Do not introduce React.
- All new Git branches must begin with `LIngXI`, as requested by the repository owner. Never use a `codex` prefix.
- Keep v0.1.0 frontend-only. No backend, account services, API keys or real AI calls.
- Keep all business state shared and persisted locally; compute statistics from that state.
- Run frontend commands in `asterway-web/`. Validate meaningful business changes with `npm test` and `npm run build`.
- Planning assistant proposals require explicit in-app confirmation and conflict-aware undo.
