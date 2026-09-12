#!/usr/bin/env python3
"""W1A semantic falsification; compilation errors never count as detection."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

from lean import require_local
require_local()

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/java/io/github/gustavo2358/cobolexplorer'
OUT = ROOT / 'target/cp6-w1a/challenges'
WRITER = 'semanticproduct/transport/SemanticProductJsonWriter.java'
MODEL = 'semanticproduct/CobolSemanticProduct.java'
ENGINE = 'ScalarMoveSemantics.java'
PROJECTOR = 'semanticproduct/projection/CobolSemanticProductProjector.java'
TESTS = 'CallCheckpointW1ATest'
CHALLENGES = [
    ('literal-lost', [(WRITER, 'operandHandle(literal.id()), literal.text(),', 'operandHandle(literal.id()), "",')]),
    ('whole-item-erased', [(ENGINE, 'if (declarations.containsKey(selected.entityId())) whole = Optional.of(selected.entityId());',
                          'if (declarations.containsKey(selected.entityId())) whole = Optional.empty();')]),
    ('wrong-continuation', [(PROJECTOR,
        '"canonical CALL continuation is not a statement");\n                return Objects.requireNonNull(statementIds.get((Ast.Statement) node), "continuation must be published");',
        '"canonical CALL continuation is not a statement");\n                return new StatementId(statementId.unit(), 0);')]),
    ('using-hidden', [('AstBuilder.java', 'context.callUsingPhrase() != null, giving != null,', 'false, giving != null,')]),
    ('returning-hidden', [('AstBuilder.java', 'context.callUsingPhrase() != null, giving != null,', 'context.callUsingPhrase() != null, false,')]),
    ('effects-none', [(MODEL, 'public enum CallEffects { UNKNOWN }', 'public enum CallEffects { UNKNOWN, NONE }'),
                      (MODEL, 'return CallEffects.UNKNOWN;', 'return CallEffects.NONE;')]),
    ('padding-erased', [(ENGINE, 'text.value() + " ".repeat(shape.extent() - text.extent())', 'text.value()')]),
    ('nonwhole-promoted', [(ENGINE,
        '&& occurrence.role() == ResolutionContracts.ReferenceRole.CALL_TARGET\n                        && target.understanding() == Ast.ReferenceUnderstanding.STRUCTURED\n                        && target.subscriptGroups().isEmpty() && target.referenceModification() == null',
        '&& occurrence.role() == ResolutionContracts.ReferenceRole.CALL_TARGET\n                        && target.understanding() == Ast.ReferenceUnderstanding.STRUCTURED')]),
]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def run(label):
    log = OUT / (label + '.log')
    if log.exists():
        raise RuntimeError('Preserve prior evidence: choose a fresh output directory before rerunning')
    with log.open('wb') as output:
        result = subprocess.run([os.getenv('MAVEN_BIN', 'mvn'), '-q', '-Dtest=' + TESTS, 'test'],
                                cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
    text = log.read_text()
    return {'label': label, 'exitCode': result.returncode, 'logSha256': digest(log.read_bytes()),
            'semanticRed': result.returncode != 0 and 'COMPILATION ERROR' not in text
                           and ('Failures:' in text or 'Errors:' in text)}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    results = [run('green-before')]
    if results[0]['exitCode'] != 0:
        raise RuntimeError('Initial GREEN failed')
    for label, changes in CHALLENGES:
        originals = {name: (BASE / name).read_bytes() for name, _, _ in changes}
        result = None
        try:
            for name, before, after in changes:
                path = BASE / name
                source = path.read_text()
                if source.count(before) != 1:
                    raise RuntimeError(label + ': expected one mutation site')
                path.write_text(source.replace(before, after))
            result = run(label)
            results.append(result)
            if not result['semanticRed']:
                raise RuntimeError(label + ': survived or nonsemantic failure')
        finally:
            for name, data in originals.items():
                (BASE / name).write_bytes(data)
            restored = all((BASE / name).read_bytes() == data for name, data in originals.items())
            assert restored
            if result is not None:
                result['restoredByteExact'] = restored
                result['sourceSha256'] = {name: digest(data) for name, data in originals.items()}
            (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
        print(json.dumps(result), flush=True)
    results.append(run('green-restored'))
    (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
    if results[-1]['exitCode'] != 0:
        raise RuntimeError('Restored second GREEN failed')
    print('W1A: eight semantic mutants rejected; byte-exact restoration; second GREEN.', flush=True)


if __name__ == '__main__':
    main()
