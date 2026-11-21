# Upstream merge helpers

Used for the v2.31.0 merge (Phase 5 step 5, unit 5a); kept for the v3.0 merge. Run from the repository root of the merge worktree.

- `resolve_hunks.py <imports|theirs> <files…>`: import-only conflict hunks take both sides' imports; with `theirs`, every other hunk takes upstream's side (used for `androidMain` files).
- `auto2.py <files…>`: a hunk whose ours side differs from base only by the restructure rewriter's mechanical changes takes upstream's side; a hunk whose ours side is empty (a block Phase 1 extracted) has upstream's change applied to the extracted copy, wherever it lives in `commonMain`.
- `pick.py <file> N=ours|theirs|both|theirs+ours|@replacement-file …`: resolves hunk N by choice.
- `hunkdiff.py <file> N`: shows upstream's base→theirs change inside hunk N.
- `apply_delta.py <conflicted> N <targets…>`: applies hunk N's base→theirs change to target files by context.
- `seams.py`: the one-off script that split five new upstream Android classes into `expect` (commonMain) + Android actual + desktop actual.
- `landing_check.py <base> <upstream>`: for every file upstream modified, checks that most of upstream's added lines are present in our real copy (catches rename detection pairing an upstream file with the wrong file, such as the byte-identical copies under `spikes/`).

After resolving: `python3 -I scripts/desktop/restructure/rewrite_imports.py`, then de-duplicate identical import lines, then `node scripts/gen-tokens.mjs`.
