import re
from pathlib import Path

C = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
A = Path("shared/src/androidMain/kotlin/com/nendo/argosy")
D = Path("shared/src/desktopMain/kotlin/com/nendo/argosy")


def edit(path, pairs, regex=False):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if regex:
            t, n = re.subn(a, b, t, count=1, flags=re.M | re.S)
            assert n == 1, (str(path), a[:80])
        else:
            assert a in t, (str(path), a[:80])
            t = t.replace(a, b, 1)
    p.write_text(t, encoding="utf-8")


def strip_defaults_in(text, start):
    s = text.index("(", start)
    depth, j = 0, s
    while True:
        c = text[j]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                break
        j += 1
    params, out, cur, d = text[s + 1:j], [], "", 0
    for c in params:
        if c in "(<[{":
            d += 1
        elif c in ")]}" or (c == ">" and cur[-1:] != "-"):
            d -= 1
        if c == "," and d == 0:
            out.append(cur)
            cur = ""
        else:
            cur += c
    out.append(cur)
    cleaned = []
    for p in out:
        d, cut = 0, None
        for k, c in enumerate(p):
            if c in "(<[{":
                d += 1
            elif c in ")]}" or (c == ">" and p[k - 1:k] != "-"):
                d -= 1
            elif c == "=" and d == 0 and p[k + 1:k + 2] != ">":
                cut = k
                break
        cleaned.append(p[:cut].rstrip() if cut is not None else p)
    return text[:s + 1] + ",".join(cleaned) + text[j:]


def strip_actual_defaults(path):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    pos = 0
    while True:
        m = re.compile(r"actual (?:\w+ )*(?:fun [^(]+|constructor)\(").search(t, pos)
        if not m:
            break
        t = strip_defaults_in(t, m.start())
        pos = m.end()
    p.write_text(t, encoding="utf-8")



for f in ["libretro/LibretroCoreManager.kt", "ui/components/Box3dCover.kt", "libretro/SaveStateManager.kt"]:
    strip_actual_defaults(A / f)
print("stripped")
