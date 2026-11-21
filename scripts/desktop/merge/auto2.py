import difflib
import re
import sys
from pathlib import Path

NORMS = [
    (re.compile(r"\bandroid\.util\.Log\."), "com.nendo.argosy.util.PlatformLog."),
    (re.compile(r"\bcom\.nendo\.argosy\.R\."), "com.nendo.argosy.res.ArgosyR."),
    (re.compile(r"^import com\.nendo\.argosy\.R$"), "import com.nendo.argosy.res.ArgosyR as R"),
    (re.compile(r"\bandroidx\.compose\.ui\.res\.(stringResource|pluralStringResource|painterResource)\b"), r"com.nendo.argosy.res.\1"),
]


def norm(lines):
    out = []
    for l in lines:
        for rx, rep in NORMS:
            l = rx.sub(rep, l)
        out.append(l.rstrip())
    return [l for l in out if l.strip()]


COMMON = sorted(Path("shared/src/commonMain/kotlin").rglob("*.kt"))


def place_delta(base, theirs, exclude):
    sm = difflib.SequenceMatcher(a=base, b=theirs, autojunk=False)
    cache = {}
    failed = []
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue
        placed = False
        for ctx in (3, 2, 1):
            before = base[max(0, i1 - ctx):i1]
            after = base[i2:i2 + ctx]
            old = before + base[i1:i2] + after
            if not any(x.strip() for x in old):
                continue
            new = before + theirs[j1:j2] + after
            hits = []
            for p in COMMON:
                if str(p) == exclude:
                    continue
                lines = cache.get(p) or p.read_text(encoding="utf-8").split("\n")
                for k in range(len(lines) - len(old) + 1):
                    if lines[k:k + len(old)] == old:
                        hits.append((p, k, lines))
            if len(hits) == 1:
                p, k, lines = hits[0]
                lines[k:k + len(old)] = new
                cache[p] = lines
                placed = True
                break
        if not placed:
            failed.append((tag, base[i1:i2][:2], theirs[j1:j2][:2]))
    return cache, failed


for path in sys.argv[1:]:
    lines = Path(path).read_text(encoding="utf-8").split("\n")
    out, cur, sec = [], None, None
    stats = {"theirs": 0, "moved": 0, "left": 0}
    pending_writes = {}
    for l in lines:
        if l.startswith("<<<<<<< "):
            cur = {"ours": [], "base": [], "theirs": [], "raw": [l]}
            sec = "ours"
            continue
        if cur is None:
            out.append(l)
            continue
        cur["raw"].append(l)
        if l.startswith("||||||| "):
            sec = "base"
            continue
        if l.startswith("=======") and sec != "theirs":
            sec = "theirs"
            continue
        if l.startswith(">>>>>>> "):
            o, b, t = cur["ours"], cur["base"], cur["theirs"]
            if norm(o) == norm(b):
                out.extend(t)
                stats["theirs"] += 1
            elif not norm(o) and len(norm(b)) >= 5:
                cache, failed = place_delta(b, t, path)
                if failed:
                    out.extend(cur["raw"])
                    stats["left"] += 1
                    print(f"  {path}: moved block, {len(failed)} change(s) not placed: {failed[:2]}")
                else:
                    pending_writes.update(cache)
                    out.extend(o)
                    stats["moved"] += 1
            else:
                out.extend(cur["raw"])
                stats["left"] += 1
            cur = None
            continue
        cur[sec].append(l)
    for p, ls in pending_writes.items():
        p.write_text("\n".join(ls), encoding="utf-8")
        print(f"  delta placed in {p}")
    Path(path).write_text("\n".join(out), encoding="utf-8")
    print(f"{path.split('/argosy/')[-1]}: {stats}")
