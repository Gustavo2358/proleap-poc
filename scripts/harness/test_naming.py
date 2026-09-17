"""Naming guard: canonical repository references in docs are not product names."""
from pathlib import Path
import os,shutil,subprocess,tempfile,unittest

ROOT=Path(__file__).resolve().parents[2]
VENDOR='pro'+'leap'
PURPOSE='bench'+'mark'

class NamingGuard(unittest.TestCase):
    def run_guard(self,name,content,without_rg=False):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);(root/'scripts').mkdir()
            shutil.copyfile(ROOT/'scripts/verify-naming.sh',root/'scripts/verify-naming.sh')
            subprocess.run(['git','init','-q',str(root)],check=True)
            path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content)
            env=os.environ.copy()
            if without_rg:
                tools=root/'tools';tools.mkdir()
                for command in ('bash','git','dirname','sed','python3'):
                    (tools/command).symlink_to(shutil.which(command))
                env['PATH']=str(tools)
            return subprocess.run(['bash','scripts/verify-naming.sh'],cwd=root,env=env,capture_output=True,text=True).returncode
    def test_canonical_repository_in_docs(self):
        self.assertEqual(0,self.run_guard('docs/evidence.md','https://github.com/example/'+VENDOR+'-poc/pull/1'))
    def test_mixed_doc_still_rejects_product_identifier(self):
        self.assertNotEqual(0,self.run_guard('docs/evidence.md',VENDOR+'-poc\n'+VENDOR+' engine'))
    def test_product_code_cannot_use_repository_exception(self):
        self.assertNotEqual(0,self.run_guard('src/app.java',VENDOR+'-poc'))
    def test_similar_identifier_is_not_the_canonical_repository(self):
        self.assertNotEqual(0,self.run_guard('docs/product.md',VENDOR+'-pocket'))
    def test_old_purpose_remains_forbidden(self):
        self.assertNotEqual(0,self.run_guard('docs/product.md',PURPOSE))
    def test_old_path_remains_forbidden(self):
        self.assertNotEqual(0,self.run_guard('src/'+VENDOR+'.txt','source'))
    def test_content_guard_does_not_silently_pass_without_ripgrep(self):
        self.assertNotEqual(0,self.run_guard('src/app.java',VENDOR+' engine',without_rg=True))
    def test_canonical_repository_remains_valid_without_ripgrep(self):
        self.assertEqual(0,self.run_guard('docs/evidence.md',VENDOR+'-poc',without_rg=True))

if __name__=='__main__':unittest.main()
