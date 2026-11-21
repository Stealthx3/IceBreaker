import difflib
import re
import subprocess
from pathlib import Path

BASE, UP = "98562a1e7", "origin/upstream"
COMMON = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
pairs = []
for line in open("scripts/desktop/restructure/extract-decls.txt"):
    line = line.strip()
    if not line or line.startswith("#") or "->" not in line:
        continue
    src, rest = [x.strip() for x in line.split("->", 1)]
    head = rest.split(":")
    target = head[1].strip() if head[0].strip() == "common" else head[0].strip()
    pairs.append((src, target))


def show(rev, p):
    r = subprocess.run(["git", "show", f"{rev}:{p}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None


seen = set()
for src, target in pairs:
    b = show(BASE, "app/src/main/kotlin/com/nendo/argosy/" + src)
    u = show(UP, "app/src/main/kotlin/com/nendo/argosy/" + src)
    if b is None or u is None or b == u:
        continue
    tpath = COMMON / Path(src).parent / target
    if not tpath.is_file():
        cands = [c for c in COMMON.rglob(target) if c.is_file()]
        if not cands:
            continue
        tpath = cands[0]
    tl = tpath.read_text(encoding="utf-8").split("\n")
    sm = difflib.SequenceMatcher(a=b.split("\n"), b=u.split("\n"), autojunk=False)
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag not in ("delete", "replace"):
            continue
        for k in range(i1, i2):
            line = b.split("\n")[k]
            if len(line.strip()) < 4:
                continue
            ctx_before = b.split("\n")[k - 1] if k else ""
            for n, l in enumerate(tl):
                if l == line and (n == 0 or tl[n - 1] == ctx_before or len(line.strip()) > 20):
                    key = (str(tpath), n)
                    if key not in seen:
                        seen.add(key)
                        print(f"{tpath.relative_to(COMMON)}:{n + 1}: {line.strip()[:110]}  (src {src})")
