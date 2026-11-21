import difflib
import sys

conflicted, idx, targets = sys.argv[1], int(sys.argv[2]), sys.argv[3:]


def hunk(path, want):
    lines = open(path, encoding="utf-8").read().split("\n")
    n = 0
    cur = None
    sec = None
    for l in lines:
        if l.startswith("<<<<<<< "):
            cur = {"ours": [], "base": [], "theirs": []}
            sec = "ours"
            continue
        if cur is None:
            continue
        if l.startswith("||||||| "):
            sec = "base"
            continue
        if l.startswith("=======") and sec != "theirs":
            sec = "theirs"
            continue
        if l.startswith(">>>>>>> "):
            n += 1
            if n == want:
                return cur
            cur = None
            continue
        cur[sec].append(l)
    raise SystemExit("hunk not found")


h = hunk(conflicted, idx)
base, theirs = h["base"], h["theirs"]
texts = {t: open(t, encoding="utf-8").read().split("\n") for t in targets}
sm = difflib.SequenceMatcher(a=base, b=theirs, autojunk=False)
failed = 0
for tag, i1, i2, j1, j2 in sm.get_opcodes():
    if tag == "equal":
        continue
    placed = False
    for ctx in (2, 1):
        before = base[max(0, i1 - ctx):i1]
        after = base[i2:i2 + ctx]
        old = before + base[i1:i2] + after
        new = before + theirs[j1:j2] + after
        for t, lines in texts.items():
            hits = [k for k in range(len(lines) - len(old) + 1) if lines[k:k + len(old)] == old]
            if len(hits) == 1:
                k = hits[0]
                lines[k:k + len(old)] = new
                placed = True
                break
        if placed:
            break
    if not placed:
        failed += 1
        print(f"NOT PLACED ({tag}): base {i1}-{i2}:", base[i1:i2][:3], "->", theirs[j1:j2][:3])
for t, lines in texts.items():
    open(t, "w", encoding="utf-8").write("\n".join(lines))
print("failed", failed)
