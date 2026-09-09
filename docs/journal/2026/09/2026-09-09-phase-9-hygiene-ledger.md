# Phase 9 Hygiene Ledger

Phase 9 closes the bounded Knowledge Map MCP publication without taking
ownership of these pre-existing packaging and release-process maintenance
items. They are nonblocking and do not authorize a change to the MCP surface.

HYG-P9-001 | Existing packaged-manual links escape the CAR manual subtree in src/main/car/manual/index.md:138 and src/main/car/manual/user-guide.md:420; maintenance-only manual packaging/link work outside Phase 9.

HYG-P9-002 | CAR ABI baseline is not configured, so compatibility against a prior released CAR was not checked; pre-existing release-process maintenance outside Phase 9.

Neither item changes the existing bounded `getKnowledgeMap` read, the exact
ten read-only MCP operations, or the permanent exclusion of source replacement
and administration from MCP.
