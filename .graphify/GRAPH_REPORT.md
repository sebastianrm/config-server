# Graph Report - .  (2026-11-09)

## Corpus Check
- Corpus is ~256 words - fits in a single context window. You may not need a graph.

## Summary
- 12 nodes · 9 edges · 3 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output
- Edge kinds: method: 6 · contains: 3


## Input Scope
- Requested: auto
- Resolved: all (source: default-auto)
- Included files: 2 · Candidates: recursive
- Excluded: 0 untracked · 0 ignored · 0 sensitive · 0 missing committed
## God Nodes (most connected - your core abstractions)
1. `ConfigServerApplicationTests` - 5 edges
2. `ConfigServerApplication` - 2 edges
3. `ConfigServerApplication` - 2 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 1 - "Community 1"
Cohesion: 0.67
Nodes (1): ConfigServerApplication

### Community 0 - "Community 0"
Cohesion: 0.33
Nodes (1): ConfigServerApplicationTests

### Community 2 - "Community 2"
Cohesion: 0.67
Nodes (1): ConfigServerApplication

## Knowledge Gaps
- **Thin community `Community 1`** (1 nodes): `ConfigServerApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 0`** (1 nodes): `ConfigServerApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 2`** (1 nodes): `ConfigServerApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Not enough signal to generate questions. This usually means the corpus has no AMBIGUOUS edges, no bridge nodes, no INFERRED relationships, and all communities are tightly cohesive. Add more files or run with --mode deep to extract richer edges._