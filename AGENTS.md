# Repository agent instructions

## CodeGraph

This branch is CodeGraph-enabled. The local index is intentionally not committed.

- Run `codegraph init .` after cloning, or `codegraph index .` after large changes.
- When `.codegraph/` exists, use `codegraph explore "<symbol or question>"` before text search or manual file traversal.
- Do not commit `.codegraph/codegraph.db`, daemon state, sockets, or logs.
