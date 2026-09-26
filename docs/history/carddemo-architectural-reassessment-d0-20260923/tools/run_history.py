#!/usr/bin/env python3
import pathlib,json,hashlib,subprocess,sys,importlib.util
ROOT=pathlib.Path(__file__).resolve().parents[3];OUT=pathlib.Path(__file__).resolve().parents[1];E=ROOT/'.positive-memory-topology/continuacao-w4-w9/evidence';D=OUT/'probes/history';D.mkdir(exist_ok=False)
spec=importlib.util.spec_from_file_location('cardrun',ROOT/'artefatos-e2e/carddemo-validation-20260923/run_carddemo.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
universe=json.loads((ROOT/'artefatos-e2e/carddemo-validation-20260923/full/discovery.json').read_text())
names=['CBACT01C','COACTUPC','CBEXPORT','COPAUS1C','COACCT01','CBPAUP0C','COADM01C','COPAUS2C','CBACT04C','COBSWAIT'];selected=[next(s for s in universe if s['path'].startswith('app/') and pathlib.Path(s['path']).stem==n) for n in names]
def digest(p):
 h=hashlib.sha256()
 if p.is_dir():
  for f in sorted(x for x in p.rglob('*') if x.is_file()):h.update(str(f.relative_to(p)).encode()+b'\0');h.update(hashlib.sha256(f.read_bytes()).digest())
 else:h.update(p.read_bytes())
 return h.hexdigest()
configs=[('pre-positive',ROOT/'.positive-memory-topology/evidence/w0-r1/runtime.json')]+[(w,E/w/f) for w,f in [('w5','runtime-w5.json'),('w6','runtime-w6.json'),('w6-r1','runtime-r1.json'),('w7','runtime-w7.json'),('w7-r1','runtime-r1.json')]]
results=[];builds=[]
for w,p in configs:
 cfg=json.loads(p.read_text());record={'wave':w,'runtime':str(p),'runtimeSha256':digest(p)}
 if 'commands' in cfg:
  checks={name:{'expected':exp,'actual':digest(pathlib.Path(name))} for name,exp in cfg['classpathSha256'].items()};record['historicalHashes']=checks
  if any(x['expected']!=x['actual'] for x in checks.values()):record['status']='NOT_RUN_RUNTIME_DRIFT';builds.append(record);continue
  old=cfg;cfg={'sources':old['baselinePins'],'checkouts':{'proleap-poc':str(ROOT/'.dependency-preservation/proleap-poc')}}
  for stage,c in old['commands'].items():cfg[stage]={'classpath':c[c.index('-cp')+1].split(':'),'main':c[-1]}
 record['sources']=cfg['sources'];record['currentRuntimeFingerprints']={x:digest(pathlib.Path(x)) for st in r.STAGES for x in cfg[st]['classpath']};record['status']='EXECUTED';builds.append(record)
 work=D/w;work.mkdir();rows=[]
 for s in selected:
  result=r.attempt(s,cfg,work,120);rows.append(result);print(w,s['path'],','.join(k+'='+v['state'] for k,v in result['stages'].items()),flush=True)
 (work/'measurements.json').write_text(json.dumps({'runtime':record,'programs':rows},indent=2)+'\n');results.extend({'wave':w,**x} for x in rows)
(D/'builds.json').write_text(json.dumps(builds,indent=2)+'\n');(D/'measurements.json').write_text(json.dumps(results,indent=2)+'\n')
