#!/usr/bin/env python3
"""Compare retained CardDemo products against an independent prior gap assessment.

Requires zstd on PATH. The run manifest contains path, id, baselineOutput and
output for each source; both output directories must retain the raw products.
Only explicitly justified diagnostic/identity/metrics changes are normalized.
"""
from pathlib import Path
import argparse,json,subprocess,collections,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--run',type=Path,required=True,help='73-source execution results.json')
parser.add_argument('--assessment-root',type=Path,required=True,help='prior programs/<id>/sp assessment root')
parser.add_argument('--output',type=Path,required=True,help='verification JSON to write')
args=parser.parse_args()
args.output.parent.mkdir(parents=True,exist_ok=True)
def rd(p):return json.loads(subprocess.check_output(['zstd','-dcq',str(p)]))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def norm(x,pub):
 if isinstance(x,str):return x.replace(pub,'PUBLICATION')
 if isinstance(x,list):return [norm(v,pub) for v in x]
 if isinstance(x,dict):return {k:norm(v,pub) for k,v in x.items()}
 return x
def packed(x):return json.dumps(x,sort_keys=True,separators=(',',':'))
def key(g):return (g['statement'],g['scope'],g['code'])
def dependencyOriginShape(x):
 if isinstance(x,list):return [dependencyOriginShape(v) for v in x]
 if isinstance(x,dict):
  if x.get('kind')=='line_columns':
   q=x['span'];return {'kind':'LINE_COLUMNS','columnBase':q['columnBase'],'columnUnit':q['columnUnit'],'lineBase':q['lineBase'],'endExclusive':q['endExclusive'],'startColumn':q['start']['column'],'startLine':q['start']['line'],'endColumn':q['end']['column'],'endLine':q['end']['line']}
  return {k:sorted([dependencyOriginShape(i) for i in v],key=packed) if k=='inputs' else v.upper() if k=='kind' and isinstance(v,str) else dependencyOriginShape(v) for k,v in x.items()}
 return x

def refs(x,domain):
 if isinstance(x,dict):
  result={x['localId']} if x.get('domain')==domain and 'localId' in x else set()
  for v in x.values():result|=refs(v,domain)
  return result
 if isinstance(x,list):return set().union(*(refs(v,domain) for v in x)) if x else set()
 return set()
def compare(r):
 old=Path(r['baselineOutput']);new=Path(r['output']);result={'path':r['path'],'id':r['id'],'checks':collections.Counter()}
 def check(condition,name):
  if not condition:raise AssertionError(r['path']+' '+name)
  result['checks'][name]+=1
 aSP=rd(old/'sp/cobol-semantic-product.json.zst');bSP=rd(new/'sp/cobol-semantic-product.json.zst')
 assessment=rd(args.assessment_root/r['id']/'sp/semantic-gap-assessment.json.zst')['units'][0]
 expected=collections.Counter(key(g) for g in assessment['gaps'] if g['status']!='SUPERSEDED')
 expected=collections.Counter({(s,scope,'CONTROL_MEMBERSHIP_NOT_PROVEN' if code=='CONTAINMENT_NOT_PROJECTED' else code):n for (s,scope,code),n in expected.items()})
 check(expected==collections.Counter(key(g) for g in bSP['gaps']),'canonical gaps equal prior proof-based pending inventory')
 result['gapsBefore']=len(aSP['gaps']);result['gapsAfter']=len(bSP['gaps'])
 for f in ['controlTopology','nominalValues','storage','entryInventory','factDependencies','dataDeclarations','fileInventory','sourceDependencies','ordinaryContinuations']:
  check(aSP.get(f)==bSP.get(f),'SP '+f+' unchanged')
 check(aSP['coverage']==bSP['coverage'],'SP coverage unchanged')
 removedCodes={'CONTAINMENT_NOT_PROJECTED','PERFORM_ISOLATED_PRIMARY_NOT_PROVEN','PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN','PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN','PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED'}
 sa=aSP['statements'];sb=bSP['statements'];check(len(sa)==len(sb),'statement inventory unchanged')
 readiness=0
 for a,b in zip(sa,sb):
  a=json.loads(packed(a));b=json.loads(packed(b));oldCodes=a.get('gapCodes',[])
  if 'gapCodes' in a:a['gapCodes']=[v for v in oldCodes if v not in removedCodes]
  if a['header']['readiness']!=b['header']['readiness']:
   check('CONTAINMENT_NOT_PROJECTED' in oldCodes,'readiness change only for retired containment restriction')
   for dimension in ['lowering','cfg']:
    aa=a['header']['readiness'][dimension];bb=b['header']['readiness'][dimension]
    if aa!=bb:
     check(aa['status']=='PARTIAL' and bb['status']=='SUFFICIENT' and aa['scope']==bb['scope'],'only redundant containment readiness changes')
     aa['status']=bb['status'];readiness+=1
  check(a==b,'statement facts preserved')
 result['localReadinessClaimsUpdated']=readiness
 aF=next(old.glob('air-*.json.zst'));bF=next(new.glob('air-*.json.zst'));a=rd(aF);b=rd(bF);ap=a['publication']['id']['localId'];bp=b['publication']['id']['localId'];a=norm(a,ap);b=norm(b,bp)
 pa=a['publication'];pb=b['publication'];oa={o['id']['localId']:o for o in pa.pop('origins')};ob={o['id']['localId']:o for o in pb.pop('origins')};ua={u['id']['localId']:u for u in pa.pop('uncertainties')};ub={u['id']['localId']:u for u in pb.pop('uncertainties')}
 def originNormalizer(origins):
  cache={}
  def value(x):
   if isinstance(x,list):return [value(v) for v in x]
   if isinstance(x,dict):
    if x.get('domain')=='origin':
     identity=x['localId'];check(identity in origins,'origin reference resolves')
     if identity not in cache:cache[identity]=value({k:v for k,v in origins[identity].items() if k!='id'})
     return {'originContent':cache[identity]}
    return {k:value(v) for k,v in x.items()}
   return x
  return value
 originA=originNormalizer(oa);originB=originNormalizer(ob)
 ua={i:originA(u) for i,u in ua.items()};ub={i:originB(u) for i,u in ub.items()}
 uncertaintyCache={}
 observedCodes={'cobol-sp:'+s['gapCode'] for s in bSP['statements'] if s['variant']=='OBSERVED'}
 def uncertainty(u):
  if id(u) in uncertaintyCache:return uncertaintyCache[id(u)]
  v={k:x for k,x in u.items() if k!='id'}
  if v['code'] in observedCodes:v['reason']='observed statement capability remains incomplete'
  uncertaintyCache[id(u)]=packed(v)
  return uncertaintyCache[id(u)]
 ca=collections.Counter(uncertainty(u) for u in ua.values());cb=collections.Counter(uncertainty(u) for u in ub.values())
 removed=ca-cb;added=cb-ca
 allowedRemoved={'cobol-sp:'+c for c in removedCodes|{'OBSERVED_STATEMENT_UNSUPPORTED','OBSERVED_STATEMENT_PARTIAL','CONDITION_SEMANTICS_NOT_AVAILABLE'}}
 check(all(json.loads(k)['code'] in allowedRemoved for k in removed),'removed AIR uncertainty content limited to retired gaps')
 check(all(json.loads(k)['code']=='cobol-sp:CONTROL_MEMBERSHIP_NOT_PROVEN' for k in added),'new AIR uncertainty only unresolved membership')
 def uncertaintyRefs(x,unc):
  if isinstance(x,list):return [uncertaintyRefs(v,unc) for v in x]
  if isinstance(x,dict):
   if x.get('domain')=='uncertainty':
    check(x['localId'] in unc,'uncertainty reference resolves')
    return {'uncertaintyContent':uncertainty(unc[x['localId']])}
   return {k:uncertaintyRefs(v,unc) for k,v in x.items()}
  return x
 def coverageRefs(p,u):
  result=[]
  for cov in [p['coverage']]+[v['coverage'] for v in p['units']]:
   result.append(collections.Counter(uncertainty(u[x['localId']]) for x in cov.pop('uncertainties')))
  return result
 for x,y in zip(coverageRefs(pa,ua),coverageRefs(pb,ub)):
  check(not ((x-y)-removed) and not ((y-x)-added),'coverage uncertainty delta exactly explained by diagnostic delta')
 check(uncertaintyRefs(originA(a),ua)==uncertaintyRefs(originB(b),ub),'AIR executable structure values effects provenance and coverage unchanged')
 result['airUncertaintiesRemoved']=sum(removed.values());result['airUncertaintiesAdded']=sum(added.values())
 ac=norm(rd(old/'cfg.json.zst'),ap);bc=norm(rd(new/'cfg.json.zst'),bp);check(ac==bc,'complete CFG unchanged')
 aq=rd(next(old.glob('source-*.json.zst')));bq=rd(next(new.glob('source-*.json.zst')))
 check(aq['source']['sha256']==hashlib.sha256(subprocess.check_output(['zstd','-dcq',str(old/'sp/cobol-semantic-product.json.zst')])).hexdigest(),'baseline qualified SP digest verified')
 check(bq['source']['sha256']==hashlib.sha256(subprocess.check_output(['zstd','-dcq',str(new/'sp/cobol-semantic-product.json.zst')])).hexdigest(),'current qualified SP digest verified')
 qEvidence=[{'air':q['air'],'source':q['source']} for q in [aq,bq]]
 for q,f in [(aq,aF),(bq,bF)]:check(q['air'][0]['sha256']==hashlib.sha256(subprocess.check_output(['zstd','-dcq',str(f)])).hexdigest(),'qualified AIR digest verified')
 for x in [aq,bq]:x.pop('air');x.pop('source')
 check(norm(aq,ap)==norm(bq,bp),'all qualified source facts derivations candidates and frontiers unchanged')
 ad=norm(rd(old/'dependencies.json.zst'),ap);bd=norm(rd(new/'dependencies.json.zst'),bp)
 depRefs=[]
 for d,u,o,ev,pub in [(ad,ua,oa,qEvidence[0],ap),(bd,ub,ob,qEvidence[1],bp)]:
  depRefs.append(collections.Counter(uncertainty(u[x['localId']]) for x in d.pop('sourceUncertaintyRefs')))
  origins=d.pop('origins');check(all(dependencyOriginShape(o[x['id']['localId']])==x for x in origins),'dependency origin table agrees with AIR')
  check(refs(d,'origin')<=set(o),'every dependency origin resolves')
  d.pop('metrics');d.get('fileDependencies',{}).pop('metrics',None)
  evidence=d.get('sourceQualifiedDependencies',{}).get('evidence',{})
  check(evidence.get('air')==norm(ev['air'],pub) and evidence.get('source')==ev['source'],'dependency fingerprints agree with qualified source')
  evidence.pop('source',None);evidence.pop('air',None)
 check(not ((depRefs[0]-depRefs[1])-removed) and not ((depRefs[1]-depRefs[0])-added),'dependency uncertainty delta explained by diagnostic delta')
 check(uncertaintyRefs(originA(ad),ua)==uncertaintyRefs(originB(bd),ub),'dependencies of every type candidates supports provenance precision reasons and status unchanged')
 check(sha(old/'sp/observed-dependencies.json.zst')==sha(new/'sp/observed-dependencies.json.zst'),'observed dependencies byte-identical')
 result['analysisStatus']=bd['analysisStatus'];result['checks']=dict(collections.Counter(result['checks']))
 result['hashes']={name:{'before':sha(old/name),'after':sha(new/name)} for name in ['sp/cobol-semantic-product.json.zst','sp/observed-dependencies.json.zst','dependencies.json.zst','cfg.json.zst']}
 return result
results=[];failures=[]
for r in json.loads(args.run.read_text()):
 try:results.append(compare(r));print(len(results),r['path'],'PASS',flush=True)
 except Exception as e:failures.append(str(e));print('FAIL',str(e),flush=True)
 args.output.write_text(json.dumps({'results':results,'failures':failures},indent=2))
print(json.dumps({'sources':len(results),'failures':failures,'before':sum(r['gapsBefore'] for r in results),'after':sum(r['gapsAfter'] for r in results),'status':dict(collections.Counter(r['analysisStatus'] for r in results))},indent=2))
if failures:raise SystemExit(1)
