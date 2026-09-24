import pathlib,json,subprocess
R=pathlib.Path(__file__).resolve().parents[3];D=pathlib.Path(__file__).resolve().parents[1];cfg=json.loads((R/'.positive-memory-topology/continuacao-w4-w9/evidence/w8/runtime-closure.json').read_text());cp=':'.join(cfg['frontend']['classpath']);ms=json.loads((R/'artefatos-e2e/carddemo-validation-20260923/full/measurements.json').read_text())['programs'];java=ms[0]['stages']['frontend']['command'][0];out=D/'probes/ast-control';out.mkdir(exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
p=subprocess.run([str(pathlib.Path(java).with_name('javac')),'-cp',cp,'-d',str(classes),str(D/'tools/AstControlProbe.java')],capture_output=True,text=True);print(p.returncode,p.stderr,flush=True)
if p.returncode:raise SystemExit(p.returncode)
names=['CBTRN01C','CBTRN03C']
for n in names:
 m=next(x for x in ms if x['path'].startswith('app/') and pathlib.Path(x['path']).stem==n);c=m['stages']['frontend']['command'];cmd=[java,'-Xmx2g','-cp',str(classes)+':'+cp,'io.github.gustavo2358.cobolexplorer.AstControlProbe',c[c.index('--source')+1],c[c.index('--copybooks')+1]]
 with (out/(n+'.json')).open('w') as f,(out/(n+'.stderr')).open('w') as e:p=subprocess.run(cmd,stdout=f,stderr=e)
 print(n,p.returncode,flush=True)
