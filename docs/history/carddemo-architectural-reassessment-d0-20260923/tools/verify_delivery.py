#!/usr/bin/env python3
"""Read-only preservation/consistency audit; writes only this D0 evidence directory."""
import pathlib,json,hashlib,subprocess,datetime,csv,re,collections
D=pathlib.Path(__file__).resolve().parents[1];R=D.parents[1];P=D/'probes';b=json.loads((P/'baseline/initial.json').read_text());products={}
for repo,v in b['currentW8'].items():
 g=lambda *a:subprocess.check_output(['git','-C',v['path'],*a]);head=g('rev-parse','HEAD').decode().strip();status=g('status','--porcelain=v1').decode();branch=g('branch','--show-current').decode().strip();diff=hashlib.sha256(g('diff',b['approvedW7R1'][repo],'--')).hexdigest();products[repo]={'head':head,'branch':branch,'status':status,'diffSha256':diff,'headUnchanged':head==v['head'],'branchUnchanged':branch==v['branch'],'statusUnchanged':status==v['status'],'diffUnchanged':diff==v['diffSha256']}
unchanged=all(all(v[k] for k in ['headUnchanged','branchUnchanged','statusUnchanged','diffUnchanged']) for v in products.values());(P/'baseline/final-preservation.json').write_text(json.dumps({'capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'products':products,'unchanged':unchanged},indent=2)+'\n');assert unchanged
E=R/'artefatos-e2e/carddemo-validation-20260923';checked=0;bad=[]
for line in (E/'sha256sums.txt').read_text().splitlines():
 h,k=line.split(maxsplit=1);f=R/k.lstrip('*');a=hashlib.sha256(f.read_bytes()).hexdigest();checked+=1
 if a!=h:bad.append({'file':k,'expected':h,'actual':a})
(P/'baseline/canonical-final-hash-validation.json').write_text(json.dumps({'checked':checked,'mismatches':bad},indent=2)+'\n');assert checked==4787 and not bad
ms=json.loads((E/'full/measurements.json').read_text())['programs'];sourcecheck=[]
for m in ms:
 c=m['stages']['frontend']['command'];f=pathlib.Path(c[c.index('--source')+1]);sourcecheck.append({'program':m['path'],'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'expected':m['sourceSha256']})
assert all(x['sha256']==x['expected'] for x in sourcecheck);(P/'baseline/final-source-hashes.json').write_text(json.dumps(sourcecheck,indent=2)+'\n')
corpus=R/'storage-semantics-st-w0-w5/runtime/carddemo-upstream';pin=subprocess.check_output(['git','-C',str(corpus),'rev-parse','HEAD']).decode().strip();assert pin=='59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e'
required=['EXECUTIVE_SUMMARY','BASELINE','FAILURE_TAXONOMY','F2_DEEP_DIVE','F3_COMPARISON','STORAGE_PARTIALITY','F1_FRONTEND','F7_IR_INTEGRITY','MINIMUM_SEMANTIC_COMPLETENESS','RESPONSIBILITY_MATRIX','ARCHITECTURE_A','ARCHITECTURE_B','ARCHITECTURE_C','ARCHITECTURE_SCORECARD','PROCESS_POSTMORTEM','CARDEMO_GATE_PROPOSAL','RECOMMENDATION','ROADMAP_PROPOSAL','EVIDENCE_INDEX','COMPLETENESS_BLAST_RADIUS'];missing=[x for x in required if not (D/f'D0_{x}.md').is_file()];assert not missing,missing
mat=list(csv.DictReader((D/'D0_F2_CLUSTER_MATRIX.csv').open()));assert len(mat)==1964 and len({x['program'] for x in mat})==46 and len({(x['program'],x['perform site']) for x in mat})==1512
ps=json.loads((P/'f2-programs.json').read_text());assert sum(sum(x['predictedRules'].values()) for x in ps)==2170;assert sum(x['allIssuesMatch'] for x in ps)==41;assert sum(x['diagnosticLimit'] for x in ps)==5
ws=json.loads((P/'f2-witnesses.json').read_text());assert len(ws)==24 and len({x['program'] for x in ws})==14
assert all(x['astControl']['ordinaryNext']==x['target']['ast']['id'] and x['astControl']['paragraphLocalNext'] is None and x['astControl']['normalCompletionRecognized'] for x in ws)
f7=list(csv.DictReader((P/'f7-labels.csv').open()));assert len(f7)==60
sp=json.loads(next((E/'profile-pure-7/case-03').rglob('cobol-semantic-product.json')).read_text());by={x['header']['id']:x for x in sp['statements']};callers={re.findall(r'statement:\d+',x['context'])[-1] for x in f7};assert len(callers)==11 and all(not by[c]['procedures'] for c in callers)
f6=json.loads((P/'ast-control/COPAUS2C-storage.json').read_text());assert f6['unresolvedCopies']==0 and f6['storageComponents'][0]['structureProven'] is False
roots=f6['storageComponents'][0]['roots'];assert [(x['astId'],x['level'],x['parsedLevel']) for x in roots if x['level']=='SQL']==[(70,'SQL',-1),(71,'SQL',-1)]
(P/'delivery-validation.json').write_text(json.dumps({'status':'IN_PROGRESS'})+'\n')
links=[]
for f in D.glob('D0_*.md'):
 text=f.read_text();assert 'TODO' not in text and 'TBD' not in text
 for target in re.findall(r'\]\(([^)]+)\)',text):
  if target.startswith(('https://','http://','#')):continue
  path=target.split('#')[0];q=(f.parent/path).resolve();
  if not q.exists():links.append({'file':f.name,'target':target})
assert not links,links
h=json.loads((P/'historical-evidence-hashes.json').read_text());assert sum(x['checked'] for x in h)==22567 and all(not x['bad'] for x in h)
result={'status':'PASS','productReposPreserved':True,'canonicalHashesVerified':checked,'sourceHashesVerified':len(sourcecheck),'historicalEvidenceHashesVerified':22567,'carddemoPin':pin,'reports':len(required),'f2MatrixRows':len(mat),'f2Programs':46,'f2Sites':1512,'f2WitnessPrograms':14,'f2WitnessCases':24,'f2AstCorroboration':'24/24','f7MappedLabels':len(f7),'f7AffectedContextsPerVariant':len(callers),'f6RootCauseVerified':True,'brokenReportLinks':links,'productTests':'NOT_RUN_NO_PRODUCT_CHANGE'}
(P/'delivery-validation.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
