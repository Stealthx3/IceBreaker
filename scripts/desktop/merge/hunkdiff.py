import sys,difflib
path,idx=sys.argv[1],int(sys.argv[2])
lines=open(path,encoding='utf-8').read().split('\n')
n=0;cur=None
for l in lines:
    if l.startswith('<<<<<<< '):
        cur={'ours':[],'base':[],'theirs':[]};sec='ours';continue
    if cur is None: continue
    if l.startswith('||||||| '): sec='base';continue
    if l.startswith('=======') and sec!='theirs': sec='theirs';continue
    if l.startswith('>>>>>>> '):
        n+=1
        if n==idx:
            for d in difflib.unified_diff(cur['base'],cur['theirs'],'base','theirs',n=1,lineterm=''): print(d)
            sys.exit()
        cur=None;continue
    cur[sec].append(l)
