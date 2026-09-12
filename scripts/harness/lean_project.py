"""Fast frontend contract regression; performance and mutation remain local."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from lean import require_local

FAST_TESTS = ('ArchitectureBoundaryTest', 'HarnessDocsTest', 'SemanticProductEntryGobackTest',
              'ScalarMoveCheckpoint4ATest', 'MoveDataSourceTest', 'CallCheckpointW1ATest', 'IfCheckpointW2ATest',
              'IfCanonicalProofTest', 'SemanticProductMoveCallContractTest')


def pin_errors(root):
    # This producer has no runtime cross-repo product dependency; Maven pins remain technical.
    try:
        pom = ET.parse(root / 'pom.xml').getroot()
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        props = {p.tag.rsplit('}', 1)[-1]: p.text for p in pom.find('m:properties', ns)}
        errors = []
        for dependency in pom.findall('m:dependencies/m:dependency', ns):
            version = dependency.findtext('m:version', namespaces=ns) or ''
            if version.startswith('${'):
                version = props.get(version[2:-1], '')
            if not version or any(x in version for x in ('LATEST','RELEASE','SNAPSHOT','[','(', '${')):
                errors.append('missing/floating Maven dependency pin')
        return errors
    except (OSError, ET.ParseError, TypeError) as error:
        return ['dependency pins: ' + str(error)]


def copy_pin_fixture(source, destination):
    shutil.copyfile(source / 'pom.xml', destination / 'pom.xml')


def break_pin_fixture(root):
    p = root / 'pom.xml'
    p.write_text(p.read_text().replace('<antlr.version>4.13.2</antlr.version>', ''))


def maven(*args):
    command = [os.environ.get('MAVEN_BIN', 'mvn'), '-B', '-ntp']
    if os.environ.get('FRONTEND_MAVEN_REPO'):
        command.append('-Dmaven.repo.local=' + os.environ['FRONTEND_MAVEN_REPO'])
    return command + list(args)


def technical_fast(root):
    subprocess.run(maven('clean', '-Dtest=' + ','.join(FAST_TESTS), 'test'), cwd=root, check=True)
    reports = root / 'target/surefire-reports'
    observed = {}
    for path in reports.glob('TEST-*.xml'):
        report = ET.parse(path).getroot()
        observed[report.attrib['name'].rsplit('.',1)[-1]] = report
    for suite in FAST_TESTS:
        report = observed.get(suite)
        if report is None or int(report.get('tests', '0')) == 0 or any(int(report.get(k, '0')) for k in ('failures','errors','skipped')):
            raise RuntimeError('Fast suite missing/failed/skipped: ' + suite)
    print('PASS: all nine Fast frontend contract/architecture suites executed')


def full_local(root):
    require_local()
    subprocess.run(maven('test'), cwd=root, check=True)
    subprocess.run(['bash', 'scripts/source-normalizer-regression.sh', 'full'], cwd=root, check=True)
    subprocess.run(['bash', 'scripts/verify-naming.sh'], cwd=root, check=True)
