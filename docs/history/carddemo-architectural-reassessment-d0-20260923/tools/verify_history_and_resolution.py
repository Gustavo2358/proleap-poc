import pathlib,json,hashlib,collections
R=pathlib.Path(__file__).resolve().parents[3];D=pathlib.Path(__file__).resolve().parents[1];B=R/'.positive-memory-topology/continuacao-w4-w9/evidence';out=[]
for w in ['w5','w6','w6-r1']:
 p=B/w;d=json.loads((p/'artifact-sha256.json').read_text());files=d.get('files',d);bad=[];checked=0
 for k,v in files.items():
  h=v.get('sha256') if isinstance(v,dict) else v
  if not isinstance(h,str) or len(h)!=64:continue
  f=p/k
  if not f.is_file():bad.append({'path':k,'issue':'missing'});continue
  a=hashlib.sha256(f.read_bytes()).hexdigest();checked+=1
  if a!=h:bad.append({'path':k,'expected':h,'actual':a})
 out.append({'wave':w,'checked':checked,'bad':bad})
for w in ['w7','w7-r1']:
 p=B/w;bad=[];checked=0
 for l in (p/'SHA256SUMS').read_text().splitlines():
  h,k=l.split(maxsplit=1);k=k.lstrip('*');f=p/k
  if not f.exists():
   trim=k.split('/',1)[-1];candidate=(R/trim if k.startswith('reference/') else p/trim)
   if candidate.is_file():f=candidate
  if not f.is_file():bad.append({'path':k,'issue':'missing'});continue
  a=hashlib.sha256(f.read_bytes()).hexdigest();checked+=1
  if a!=h:bad.append({'path':k,'expected':h,'actual':a})
 out.append({'wave':w,'checked':checked,'bad':bad})
(D/'probes/historical-evidence-hashes.json').write_text(json.dumps(out,indent=2)+'\n');print([(x['wave'],x['checked'],len(x['bad'])) for x in out])
E=R/'artefatos-e2e/carddemo-validation-20260923';ms=json.loads((E/'full/measurements.json').read_text())['programs'];blast=json.loads((D/'probes/storage-blast-radius.json').read_text());rows=[]
for b in blast:
 if 'F5' not in b['family']:continue
 m=next(x for x in ms if x['path']==b['program']);p=E/'full'/m['rawDirectory']/'sp';d=json.loads((p/'resolution-data.js').read_text().split('=',1)[1].strip().rstrip(';'));unknown=[e for e in d['entries'] if e['status'] not in ['RESOLVED','EXTERNAL_OBSERVED']];rows.append({'program':b['program'],'counts':d['counts'],'unresolved':unknown,'allEntries':d['entries']})
(D/'probes/f5-resolution.json').write_text(json.dumps(rows,indent=2)+'\n')
for r in rows:print(r['program'],r['counts']['status'],dict(collections.Counter(x['writtenText'] for x in r['unresolved'])))
