#!/usr/bin/env python3
"""Read immutable CardDemo evidence; derive diagnostics only, never product or semantic facts."""
import csv,json,pathlib,collections,hashlib,re
ROOT=pathlib.Path(__file__).resolve().parents[3]
E=ROOT/'artefatos-e2e/carddemo-validation-20260923'
OUT=pathlib.Path(__file__).resolve().parents[1]
def dump(name,obj):
 (OUT/'probes'/name).write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n')
def js(p):return json.loads(p.read_text().split('=',1)[1].strip().rstrip(';'))
def sid(s):return s['header']['id']
def loc(s):return s['header']['provenance']['original']
def nxt(s):
 if s['variant'] in ['GO_TO','GOBACK']:return None
 n=s.get('localContinuation',s.get('normalContinuation'))
 return {'statement':n,'availability':'KNOWN'} if isinstance(n,str) else n
classification=list(csv.DictReader((E/'classification.csv').open()))
full=json.loads((E/'full/measurements.json').read_text()); programs={p['path']:p for p in full['programs']}
assert len(programs)==73==len(classification)
program_rows=[];matrix=[];details=[];whole=[]
for c in classification:
 path=c['path'];m=programs[path]; raw=E/'full'/m['rawDirectory'];p=raw/'sp/cobol-semantic-product.json'
 source=pathlib.Path(m['stages']['frontend']['command'][m['stages']['frontend']['command'].index('--source')+1]); source_bytes=source.read_bytes();assert hashlib.sha256(source_bytes).hexdigest()==m['sourceSha256']
 row={'path':path,'family':c['firstBlockingFamily'] if 'firstBlockingFamily' in c else c.get('family'),'sourceLines':len(source_bytes.splitlines()),'stages':{k:{z:v.get(z) for z in ['state','exitCode','elapsedMs','artifactSha256','diagnostic']} for k,v in m['stages'].items()}}
 whole.append(row)
 if not p.exists():continue
 sp=json.loads(p.read_text()); ast=js(raw/'sp/ast-data.js');pre=(raw/'sp/preprocessed.cbl').read_text().splitlines();byid={sid(s):s for s in sp['statements']};astindex=collections.defaultdict(list)
 for n in ast['nodes']:astindex[n['l'],n['c']].append(n)
 def surface(s):
  e=s['header']['provenance']['expanded'];lines=pre[e['startLine']-1:e['endLine']];lines[-1]=lines[-1][:e['endColumn']+1];lines[0]=lines[0][e['startColumn']:];return ' '.join(x.strip() for x in lines)
 def anode(s):
  e=s['header']['provenance']['expanded']; ns=astindex[e['startLine'],e['startColumn']];return next((n for n in ns if 'Statement' in n['t']),{})
 def compact(s):
  return {'id':sid(s),'variant':s['variant'],'source':loc(s),'ast':anode(s),'surface':surface(s),'next':nxt(s),'containment':s['header']['containment']}
 row.update(statements=len(byid),astNodes=len(ast['nodes']),parseTreeNodes=ast['meta']['parseTreeNodes'],variants=dict(collections.Counter(s['variant'] for s in byid.values())),storageGapCodes=sp['storage']['gapCodes'],dependencies=sp['sourceDependencies'],entryInventory=sp['entryInventory'])
 if 'F2' not in str(row['family']):continue
 ordinary={r['statement']:r['destination'] for r in sp.get('ordinaryContinuations',[])}
 site_count=0;issues=[];site_summaries=[];count=collections.Counter()
 for s in sp['statements']:
  if s['variant']!='PERFORM_PROCEDURE':continue
  site_count+=1;site_issues=[]; ps=s['procedures']; all_members={x for p in ps for x in p['statements']}; ms=sorted((byid[x] for x in all_members),key=lambda q:q['header']['programPoint']);astsite=anode(s)
  for i,p in enumerate(ps):
   local=set(p['statements']); cs=set(p['completions']);bad=[]
   for id in sorted(cs,key=lambda q:byid[q]['header']['programPoint']):
    member=byid[id];n=nxt(member);t=n.get('statement') if n else None
    if not(n is not None and (t is None or member['variant']=='CALL' and t not in local) and member['variant'] not in ['GOBACK','GO_TO']):bad.append({'rule':'normal completion cannot override explicit control','statement':id})
   for id in p['statements']:
    member=byid[id];n=nxt(member);t=n.get('statement') if n else None
    if n is not None and t is not None and t not in local and not(member['variant']=='CALL' and id in cs):bad.append({'rule':'intrinsic normal edge stays in paragraph','statement':id})
   for b in bad:
    f=byid[b['statement']];r={'path':path,'perform':sid(s),'paragraph':p['id'],**b,'member':compact(f),'target':compact(byid[nxt(f)['statement']]) if nxt(f) and nxt(f).get('statement') else None};site_issues.append(r);issues.append(r)
   frontier=[compact(byid[x]) for x in p['completions']]
   terminal_evidence=[compact(byid[x]) for x in dict.fromkeys(p['completions']+[b['statement'] for b in bad])]
   kinds=sorted({(f['surface'].split() or ['EMPTY'])[0].upper().rstrip('.') for f in frontier})
   paragraph_name=next((n['n'] for n in ast['nodes'] if n['t']=='Paragraph' and n['l']==p['provenance']['expanded']['startLine']),p['id'])
   rowm={'program':path,'perform site':sid(s)+'@'+str(loc(s)['startLine']),'range':json.dumps(astsite.get('a',{}),ensure_ascii=False),'paragraphs':paragraph_name,'terminal/frontier statements':' | '.join(f['id']+': '+f['surface'][:250] for f in terminal_evidence),'intrinsic continuation':' | '.join(f['id']+' -> '+str((f['next'] or {}).get('statement')) for f in terminal_evidence),'ordinary continuation':' | '.join(f['id']+' -> '+str(ordinary.get(f['id'],(f['next'] or {}).get('statement'))) for f in terminal_evidence),'contextual completion':ps[i+1]['entry'] if i+1<len(ps) else 'resume '+str(s.get('normalContinuation',{}).get('statement')),'explicit transfer':' | '.join(sid(x)+': '+surface(x)[:180] for x in ms if x['variant'] in ['GO_TO','GO_TO_DEPENDING','GOBACK']),'nested control':json.dumps(dict(collections.Counter(x['variant'] for x in ms if x['variant'] in ['IF','EVALUATE','PERFORM_PROCEDURE','PERFORM','CICS_PROGRAM_CONTROL','CICS_FILE_CONTROL']))),'current producer facts':s.get('publicationKind','LEGACY')+'; entry='+p['entry']+'; completions='+','.join(p['completions']),'current lower rejection':' | '.join(b['statement']+': '+b['rule'] for b in bad),'cluster':('RANGE' if len(ps)>1 else 'SINGLE')+';'+','.join(kinds)+';'+('REPETITION' if any(s.get(k) for k in ['loop','times','varying']) else 'ONCE')}
   matrix.append(rowm);count.update(kinds)
  site_summaries.append({'site':compact(s),'astAttributes':astsite.get('a',{}),'range':ps,'publicationKind':s.get('publicationKind','LEGACY'),'gapCodes':s['gapCodes'],'bad':site_issues})
 # Match actual diagnostic lines, allowing the documented diagnostic ceiling.
 actual=(raw/'lower.stderr').read_text();actual_rules=collections.Counter(re.findall(r'(normal completion cannot override explicit control|intrinsic normal edge stays in paragraph)',actual)); predicted_rules=collections.Counter(x['rule'] for x in issues)
 issue_kinds=collections.Counter((r['member']['surface'].split() or ['EMPTY'])[0].upper().rstrip('.') for r in issues)
 program_rows.append({'path':path,'statements':len(byid),'performSites':site_count,'rangeSites':sum(len(x['range'])>1 for x in site_summaries),'repetitionSites':sum(x['astAttributes'].get('repetition') not in [None,'ONCE'] for x in site_summaries),'frontierKinds':dict(count),'issueKinds':dict(issue_kinds),'predictedRules':dict(predicted_rules),'actualRules':dict(actual_rules),'diagnosticLimit':'IMPLEMENTATION_LIMIT' in actual,'allIssuesMatch':all(actual_rules[k]==v for k,v in predicted_rules.items())})
 details.append({'path':path,'sites':site_summaries})
with (OUT/'D0_F2_CLUSTER_MATRIX.csv').open('w') as f:
 w=csv.DictWriter(f,fieldnames=list(matrix[0]));w.writeheader();w.writerows(matrix)
dump('all-programs-analysis.json',whole);dump('f2-programs.json',program_rows);dump('f2-details.json',details)
print('classification fields',classification[0].keys());print('F2 programs',len(program_rows),'site-paragraph rows',len(matrix))
for p in program_rows:print(p['path'],p['performSites'],p['rangeSites'],p['repetitionSites'],p['issueKinds'],p['predictedRules'],p['allIssuesMatch'],p['diagnosticLimit'])
