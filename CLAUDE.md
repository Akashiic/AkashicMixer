# CLAUDE.md

Entry point for Claude (or any coding assistant) working in this repository.

1. Read [`AGENTS.md`](AGENTS.md): the engineering rules. They apply to you.
2. Read [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md).

## Owner and language

The owner is Gabriel (GitHub `Akashiic`, Rede Akashic). Conversation with him is in Brazilian Portuguese; repository
documentation, commit messages, issues and code comments are in English. [`docs/PT-BR_RESUMO.md`](docs/PT-BR_RESUMO.md)
is the Portuguese summary; keep it in step with `PROJECT_STATE.md`.

## Consumers

AkashicFM (`radios`, `music`) and AkashicWeb (`screens`) depend on this mod (`required-after:akashicmixer`). An API
change is not done until both build against it.
