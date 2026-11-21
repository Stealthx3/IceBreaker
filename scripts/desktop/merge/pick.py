import sys

path = sys.argv[1]
choices = {}
for spec in sys.argv[2:]:
    idx, choice = spec.split("=", 1)
    choices[int(idx)] = choice

lines = open(path, encoding="utf-8").read().split("\n")
out = []
n = 0
cur = None
sec = None
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
        n += 1
        c = choices.get(n)
        if c == "ours":
            out.extend(cur["ours"])
        elif c == "theirs":
            out.extend(cur["theirs"])
        elif c == "both":
            out.extend(cur["ours"] + cur["theirs"])
        elif c == "theirs+ours":
            out.extend(cur["theirs"] + cur["ours"])
        elif c and c.startswith("@"):
            out.extend(open(c[1:], encoding="utf-8").read().rstrip("\n").split("\n"))
        else:
            out.extend(cur["raw"])
        cur = None
        continue
    cur[sec].append(l)
open(path, "w", encoding="utf-8").write("\n".join(out))
left = sum(1 for l in out if l.startswith("<<<<<<< "))
print(f"{path.rsplit('/', 1)[-1]}: hunks={n} left={left}")
