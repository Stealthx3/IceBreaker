import difflib
import os
import re
import subprocess
import sys

base, upstream = sys.argv[1], sys.argv[2]


def show(rev, p):
    r = subprocess.run(["git", "show", f"{rev}:{p}"], capture_output=True, text=True, errors="replace")
    return r.stdout if r.returncode == 0 else None


def norm(line):
    return re.sub(r"\s+", " ", line.strip())


changed = subprocess.run(["git", "diff", "--name-status", "--no-renames", base, upstream], capture_output=True, text=True).stdout
for row in changed.splitlines():
    status, f = row.split("\t")[0], row.split("\t")[-1]
    if status != "M" or not f.endswith((".kt", ".xml", ".kts", ".toml")):
        continue
    b, t = show(base, f), show(upstream, f)
    if b is None or t is None:
        continue
    added = [norm(x[1:]) for x in difflib.unified_diff(b.split("\n"), t.split("\n"), lineterm="", n=0)
             if x.startswith("+") and not x.startswith("+++")]
    added = [a for a in added if len(a) > 12 and not a.startswith(("import ", "//", "*", "/*"))]
    if len(added) < 3:
        continue
    cands = [f] if os.path.exists(f) else []
    i = f.find("/com/nendo/argosy/")
    if i >= 0:
        for root in ["shared/src/commonMain/kotlin", "shared/src/androidMain/kotlin", "shared/src/desktopMain/kotlin", "app/src/main/kotlin"]:
            p = root + f[i:]
            if os.path.exists(p) and p not in cands:
                cands.append(p)
    if f.startswith("app/src/main/res/"):
        p = "shared/src/androidMain/res/" + f[len("app/src/main/res/"):]
        if os.path.exists(p):
            cands.append(p)
    if not cands:
        print(f"NOWHERE {len(added)} {f}")
        continue
    text = " ".join(norm(l) for c in cands for l in open(c, encoding="utf-8", errors="replace").read().split("\n"))
    hit = sum(1 for a in added if a in text)
    if hit / len(added) < 0.6:
        print(f"LOW {hit}/{len(added)} {f} -> {','.join(cands)}")
