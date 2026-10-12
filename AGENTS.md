# Asterway development conventions

- Use Vue and TypeScript for this frontend. Do not introduce React.
- All new Git branches must begin with `LIngXI`, as requested by the repository owner. Never use a `codex` prefix.
- Keep `LIngXI-web-mvp` as the sole GitHub branch and the repository default branch. Do not create or push other remote branches unless the owner explicitly changes this rule.
- Create development branches locally only. Validate changes locally, merge them into `LIngXI-web-mvp`, then push only that branch with `git push origin LIngXI-web-mvp`. Delete local development branches after their changes have been merged. Never publish a development branch with `git push -u` or push all branches.
- Keep the prototype frontend-only. Community, messages, accounts, membership and AI are explicit local demonstrations, without backend services, payment or real AI calls.
- Keep all business state shared and persisted locally; compute statistics from that state.
- Run frontend commands in `asterway-web/`. Validate meaningful business changes with `npm test` and `npm run build`.
- Planning assistant proposals require in-app confirmation and conflict-aware undo. Users may explicitly enable automatic execution for additions; rescheduling still requires confirmation.
