import pathlib,json,csv,re,collections,hashlib
R=pathlib.Path(__file__).resolve().parents[3];D=pathlib.Path(__file__).resolve().parents[1];E=R/'artefatos-e2e/carddemo-validation-20260923';P=D/'probes'
def dump(n,o):(P/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n')
ms=json.loads((E/'full/measurements.json').read_text())['programs'];by={m['path']:m for m in ms};aa=json.loads((P/'all-programs-analysis.json').read_text());details=json.loads((P/'f2-details.json').read_text())
def sp_for(path):return json.loads((E/'full'/by[path]['rawDirectory']/'sp/cobol-semantic-product.json').read_text())
def walk(x):
 if isinstance(x,dict):
  yield x
  for v in x.values():yield from walk(v)
 elif isinstance(x,list):
  for v in x:yield from walk(v)
clusters={};members=collections.defaultdict(list)
for prog in details:
 tags=set()
 for site in prog['sites']:
  for bad in site['bad']:
   member=bad['member'];front=any(member['id'] in p['completions'] for p in site['range']);verb=member['surface'].split()[0].upper().rstrip('.')
   tag='K4_UNPUBLISHED_FRONTIER' if not front else 'K3_COMPOSED_FRONTIER' if member['containment']['parent'] else 'K1_ROOT_EXIT' if verb=='EXIT' else 'K2_ROOT_SEQUENTIAL';tags.add(tag)
 clusters[prog['path']]=sorted(tags)
 for t in tags:members[t].append(prog['path'])
dump('f2-clusters.json',{'membership':clusters,'clusters':dict(members)})
mat=list(csv.DictReader((D/'D0_F2_CLUSTER_MATRIX.csv').open()))
for row in mat:row['cluster']=' | '.join(clusters[row['program']])+'; '+row['cluster']
with (D/'D0_F2_CLUSTER_MATRIX.csv').open('w') as f:w=csv.DictWriter(f,fieldnames=mat[0].keys());w.writeheader();w.writerows(mat)
# Witness choices are explicit, from actual lower offending locations, not sampled synthetic inputs.
choices={'CBACT01C':[(89,158)],'CBACT02C':[(31,49)],'COACTUPC':[(858,1284),(858,1312),(886,943),(916,1317),(1210,1222)],'COCRDUPC':[(306,344)],'CBEXPORT':[(94,106),(103,111),(133,145)],'COACCT01':[(136,158),(143,166)],'COPAUS0C':[(271,291),(300,319)],'COPAUS1C':[(131,168)],'COBIL00C':[(127,152)],'COTRTUPC':[(157,196)],'COPAUA0C':[(185,192),(229,240),(234,292)],'CBTRN02C':[(271,277)],'CBTRN01C':[(107,130)],'CBTRN03C':[(143,169)]}
ws=[];dossier=['# Real F2 source / AST / SP diagnostic dossier','', 'Extracted source is evidence; expected edges are independently reasoned in D0_F2_DEEP_DIVE.md.','']
for name,selected in choices.items():
 prog=next(x for x in details if x['path'].startswith('app/') and pathlib.Path(x['path']).stem==name);sp=sp_for(prog['path']);statements={s['header']['id']:s for s in sp['statements']};pre=(E/'full'/by[prog['path']]['rawDirectory']/'sp/preprocessed.cbl').read_text().splitlines();ast=json.loads((P/'ast-control'/f'{name}.json').read_text());astby={s['id']:s for s in ast['statements']};dossier+=['## '+name,'']
 for caller,terminal in selected:
  site=next(s for s in prog['sites'] if s['site']['id']==f'statement:{caller}');bad=next(b for b in site['bad'] if b['statement']==f'statement:{terminal}');member=bad['member'];para=next(p for p in site['range'] if member['id'] in p['statements']);i=site['range'].index(para);expected=site['range'][i+1]['entry'] if i+1<len(site['range']) else statements[site['site']['id']]['normalContinuation']['statement'];ar=astby.get(member['ast']['id']);row={'program':prog['path'],'caller':site['site'],'terminal':member,'target':bad['target'],'range':site['range'],'paragraph':para,'expectedContextCompletion':expected,'astControl':ar,'producerCompletions':para['completions'],'gaps':site['gapCodes']};ws.append(row)
  dossier += [f"### {site['site']['id']} / {member['id']}",f"Source: {member['source']}; caller: {site['site']['surface']}",f"AST control: `{json.dumps(ar)}`",f"SP completion membership: {member['id'] in para['completions']}; gaps: {site['gapCodes']}",'```cobol']
  e=statements[member['id']]['header']['provenance']['expanded'];a=max(0,e['startLine']-6);b=min(len(pre),e['endLine']+5);dossier += [f'{k+1:5d} {pre[k]}' for k in range(a,b)]+['```','']
dump('f2-witnesses.json',ws);(P/'f2-source-dossier.md').write_text('\n'.join(dossier)+'\n')
# Per-unit blast radius: references stay qualified as observed, not attributed to absent vendor definitions.
rows=[]
for x in aa:
 if not any(t in str(x['family']) for t in ['F4','F5','F6']):continue
 sp=sp_for(x['path']);ds=sp['dataDeclarations'];refs=[]
 for s in sp['statements']:
  for o in walk(s):
   if 'binding' in o and isinstance(o['binding'],dict):
    b=o['binding'];refs.append({'statement':s['header']['id'],'binding':b,'provenance':o.get('provenance')})
 unresolved=[r for r in refs if r['binding'].get('status') not in ['RESOLVED','RESOLVED_EXTERNAL']];blocked=(E/'full'/by[x['path']]['rawDirectory']/'lower.stderr').read_text();blockids=set(re.findall(r'PROFILE_FACT (data:\d+)',blocked));
 row={'program':x['path'],'family':x['family'],'gaps':x['storageGapCodes'],'copySites':[o for o in x['dependencies']['occurrences'] if o['resolution']=='UNRESOLVED'],'declarations':len(ds),'directSourceDeclarations':[{'id':d['id'],'name':d['canonicalName'],'line':d['provenance']['original']['startLine']} for d in ds if not d['provenance']['includeChain']],'rejectedDeclarations':[{'id':d['id'],'name':d['canonicalName'],'source':d['provenance']['original']} for d in ds if d['id'] in blockids],'unresolvedReferences':unresolved,'resolvedReferenceStatements':sorted({r['statement'] for r in refs if r['binding'].get('status')=='RESOLVED'}),'dependencyStatements':[{'id':s['header']['id'],'variant':s['variant'],'source':s['header']['provenance']['original'],'surface':{k:v for k,v in s.items() if k in ['target','operation','observedShape']}} for s in sp['statements'] if s['variant'] in ['CALL','CICS_PROGRAM_CONTROL'] or s.get('observedShape')=='OPAQUE_CICS'],'allSourceDependencies':x['dependencies']['occurrences']};rows.append(row)
dump('storage-blast-radius.json',rows)
# Full review of existing auxiliary probe measurements, no rerun.
profiles={}
for name in ['profile-probe','profile-input-probe','profile-pure-7']:
 files=list((E/name).rglob('measurements.json'));profiles[name]=[]
 for f in files:
  d=json.loads(f.read_text());programs=d['programs']
  profiles[name].extend({'path':m['path'],'stages':{k:{a:v.get(a) for a in ['state','reasonCode','diagnostic','elapsedMs']} for k,v in m['stages'].items()}} for m in programs)
dump('profile-probe-analysis.json',profiles)
# One row for every actual I-02, with materializer and precise closure deficiency.
rows=[]
for case in ['03','05']:
 d=json.loads((P/f'f7-{case}.json').read_text());sp=json.loads(next((E/'profile-pure-7'/f'case-{case}').rglob('cobol-semantic-product.json')).read_text());s={x['header']['id']:x for x in sp['statements']}
 for issue in d['issues']:
  source=issue['sourceStatements'][0];context,target=re.search(r'key=(.*):(statement:\d+)\]',issue['targetIdentity']).groups();caller=re.findall(r'statement:\d+',context)[-1];assert source in d['compositions'][caller] and target not in d['compositions'][caller];assert target in d['ordinaryInventory'];rows.append({'case':case,'operation':issue['operation'],'source statement':source,'source line':s[source]['header']['provenance']['original']['startLine'],'target label':issue['targetLabel'],'target statement':target,'target line':s[target]['header']['provenance']['original']['startLine'],'context':context,'expected materializer':'PartialProgramAssembler.append / activation '+context+' / '+target,'why absent':'CompositionalPerformAdmission closure follows ordinary/control statement edges but omits fileInventory INVALID_KEY handler entry; root inventory exists in different context','mechanism':'FILE_HANDLER_REFERENCE_CLOSURE','rule':issue['rule']})
with (P/'f7-labels.csv').open('w') as f:w=csv.DictWriter(f,fieldnames=rows[0].keys());w.writeheader();w.writerows(rows)
print('clusters', {k:len(v) for k,v in members.items()},'witness programs',len(choices),'witness cases',len(ws),'F7',len(rows))
